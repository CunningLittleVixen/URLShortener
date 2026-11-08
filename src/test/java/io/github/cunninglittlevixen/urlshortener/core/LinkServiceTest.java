package io.github.cunninglittlevixen.urlshortener.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.cunninglittlevixen.urlshortener.infra.InMemoryLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.RandomCodeGenerator;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryNotificationRepository;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final String URL = "https://www.baeldung.com/java-9-http-client";

    private final AppConfig config = new AppConfig(Duration.ofHours(24), 1, 1000, 500, 6,
        "clck.ru/", Duration.ofMinutes(1), Path.of("data/links.json"));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryLinkRepository repository = new InMemoryLinkRepository();
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();

    private LinkService service(CodeGenerator generator) {
        return new LinkService(repository, notifications,generator, new UrlValidator(), config, clock);
    }

    private LinkService service() {
        return service(new RandomCodeGenerator(6));
    }

    private LinkService serviceAt(Instant moment) {
        return new LinkService(repository, notifications, new RandomCodeGenerator(6), new UrlValidator(),
            config, Clock.fixed(moment, ZoneOffset.UTC));
    }

    @Test
    void createsLinkWithDefaultLimitAndTtl() {
        UUID owner = UUID.randomUUID();

        Link link = service().createLink(owner, URL);

        assertEquals(owner, link.ownerId());
        assertEquals(URL, link.originalUrl());
        assertEquals(500, link.clickLimit());
        assertEquals(0, link.clickCount());
        assertEquals(NOW.plus(Duration.ofHours(24)), link.expiresAt());
        assertEquals(link, repository.findByCode(link.code()).orElseThrow());
    }

    @Test
    void usesCustomLimit() {
        Link link = service().createLink(UUID.randomUUID(), URL, 7);

        assertEquals(7, link.clickLimit());
    }

    @Test
    void rejectsLimitOutOfRange() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();

        assertThrows(ServiceException.class, () -> service.createLink(owner, URL, 0));
        assertThrows(ServiceException.class, () -> service.createLink(owner, URL, 1001));
    }

    @Test
    void rejectsInvalidUrl() {
        assertThrows(ServiceException.class,
            () -> service().createLink(UUID.randomUUID(), "not a url"));
    }

    @Test
    void sameUrlFromDifferentUsersGivesDifferentCodes() {
        LinkService service = service();

        Link first = service.createLink(UUID.randomUUID(), URL);
        Link second = service.createLink(UUID.randomUUID(), URL);

        assertNotEquals(first.code(), second.code());
        assertNotEquals(first.ownerId(), second.ownerId());
    }

    @Test
    void sameUserGetsNewLinkEachTime() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();

        service.createLink(owner, URL);
        service.createLink(owner, URL);

        assertEquals(2, repository.findByOwner(owner).size());
    }

    @Test
    void retriesWhenGeneratedCodeIsTaken() {
        Iterator<String> codes = List.of("dup", "dup", "fresh").iterator();
        LinkService service = service(codes::next);

        service.createLink(UUID.randomUUID(), URL);
        Link second = service.createLink(UUID.randomUUID(), URL);

        assertEquals("fresh", second.code());
    }

    @Test
    void failsWhenCodeIsAlwaysTaken() {
        LinkService service = service(() -> "same");
        service.createLink(UUID.randomUUID(), URL);

        assertThrows(IllegalStateException.class,
            () -> service.createLink(UUID.randomUUID(), URL));
    }

    @Test
    void shortUrlStartsWithConfiguredPrefix() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL);

        assertTrue(service.shortUrl(link).startsWith("clck.ru/"));
        assertTrue(service.shortUrl(link).endsWith(link.code()));
    }

    @Test
    void openReturnsOriginalUrlAndCountsClick() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL);

        String result = service.open(link.code());

        assertEquals(URL, result);
        assertEquals(1, repository.findByCode(link.code()).orElseThrow().clickCount());
    }

    @Test
    void openAcceptsCodeWithPrefixAndSpaces() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL);

        assertEquals(URL, service.open("  clck.ru/" + link.code() + " "));
    }

    @Test
    void openUnknownCodeThrows() {
        LinkService service = service();

        assertThrows(ServiceException.class, () -> service.open("nope12"));
    }

    @Test
    void openBlankInputThrows() {
        LinkService service = service();

        assertThrows(ServiceException.class, () -> service.open("   "));
        assertThrows(ServiceException.class, () -> service.open("clck.ru/"));
        assertThrows(ServiceException.class, () -> service.open(null));
    }

    @Test
    void openWorksJustBeforeExpiry() {
        Link link = service().createLink(UUID.randomUUID(), URL);
        Instant almostExpired = link.expiresAt().minusSeconds(1);

        assertEquals(URL, serviceAt(almostExpired).open(link.code()));
    }

    @Test
    void openExpiredLinkThrowsAndKeepsCounter() {
        Link link = service().createLink(UUID.randomUUID(), URL);

        ServiceException error = assertThrows(ServiceException.class,
            () -> serviceAt(link.expiresAt()).open(link.code()));

        assertTrue(error.getMessage().contains("истёк"));
        assertEquals(0, repository.findByCode(link.code()).orElseThrow().clickCount());
    }

    @Test
    void openBlocksLinkAfterLimitReached() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL, 2);
        service.open(link.code());
        service.open(link.code());

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.open(link.code()));

        assertTrue(error.getMessage().contains("исчерпан"));
        assertEquals(2, repository.findByCode(link.code()).orElseThrow().clickCount());
    }

    @Test
    void expiryHasPriorityOverLimit() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL, 1);
        service.open(link.code());

        ServiceException error = assertThrows(ServiceException.class,
            () -> serviceAt(link.expiresAt()).open(link.code()));

        assertTrue(error.getMessage().contains("истёк"));
    }

    @Test
    void blockedLinkStaysInRepositoryUntilCleanup() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL, 1);
        service.open(link.code());

        assertThrows(ServiceException.class, () -> service.open(link.code()));

        assertTrue(repository.findByCode(link.code()).isPresent());
    }

    @Test
    void removeExpiredDeletesOnlyExpiredLinks() {
        Link old = service().createLink(UUID.randomUUID(), URL);
        Link fresh = serviceAt(NOW.plus(Duration.ofHours(1))).createLink(UUID.randomUUID(), URL);

        List<Link> removed = serviceAt(old.expiresAt()).removeExpired();

        assertEquals(List.of(old), removed);
        assertTrue(repository.findByCode(old.code()).isEmpty());
        assertTrue(repository.findByCode(fresh.code()).isPresent());
    }

    @Test
    void removeExpiredKeepsLinksBeforeExpiry() {
        Link link = service().createLink(UUID.randomUUID(), URL);

        List<Link> removed = serviceAt(link.expiresAt().minusSeconds(1)).removeExpired();

        assertTrue(removed.isEmpty());
        assertTrue(repository.findByCode(link.code()).isPresent());
    }

    @Test
    void removeExpiredKeepsExhaustedLinkUntilTtlEnds() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL, 1);
        service.open(link.code());

        assertTrue(service.removeExpired().isEmpty());
        assertTrue(repository.findByCode(link.code()).isPresent());
    }

    @Test
    void removeExpiredReturnsEachLinkOnlyOnce() {
        Link link = service().createLink(UUID.randomUUID(), URL);
        LinkService later = serviceAt(link.expiresAt());

        assertEquals(1, later.removeExpired().size());
        assertTrue(later.removeExpired().isEmpty());
    }

    @Test
    void editLimitChangesOnlyLimit() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 5);
        service.open(link.code());

        Link edited = service.editLimit(owner, link.code(), 10);

        assertEquals(10, edited.clickLimit());
        assertEquals(1, edited.clickCount());
        assertEquals(link.expiresAt(), edited.expiresAt());
        assertEquals(edited, repository.findByCode(link.code()).orElseThrow());
    }

    @Test
    void editLimitByNonOwnerThrowsAndKeepsLimit() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL, 5);

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.editLimit(UUID.randomUUID(), link.code(), 10));

        assertTrue(error.getMessage().contains("другому пользователю"));
        assertEquals(5, repository.findByCode(link.code()).orElseThrow().clickLimit());
    }

    @Test
    void editLimitRespectsBoundsAndCounter() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 10);
        service.open(link.code());
        service.open(link.code());
        service.open(link.code());

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.editLimit(owner, link.code(), 3));
        assertTrue(error.getMessage().contains("от 4 до 1000"));
        assertThrows(ServiceException.class, () -> service.editLimit(owner, link.code(), 1001));

        assertEquals(4, service.editLimit(owner, link.code(), 4).clickLimit());
    }

    @Test
    void editLimitOfExhaustedLinkThrows() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 1);
        service.open(link.code());

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.editLimit(owner, link.code(), 5));

        assertTrue(error.getMessage().contains("исчерпан"));
        assertEquals(1, repository.findByCode(link.code()).orElseThrow().clickLimit());
    }

    @Test
    void editLimitOfExpiredLinkThrows() {
        UUID owner = UUID.randomUUID();
        Link link = service().createLink(owner, URL, 5);

        ServiceException error = assertThrows(ServiceException.class,
            () -> serviceAt(link.expiresAt()).editLimit(owner, link.code(), 10));

        assertTrue(error.getMessage().contains("истёк"));
    }

    @Test
    void editLimitUnknownCodeThrows() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();

        assertThrows(ServiceException.class, () -> service.editLimit(owner, "nope12", 10));
    }

    @Test
    void deleteLinkRemovesOwnLink() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL);

        service.deleteLink(owner, link.code());

        assertTrue(repository.findByCode(link.code()).isEmpty());
        assertThrows(ServiceException.class, () -> service.open(link.code()));
    }

    @Test
    void deleteLinkByNonOwnerThrowsAndKeepsLink() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL);

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.deleteLink(UUID.randomUUID(), link.code()));

        assertTrue(error.getMessage().contains("другому пользователю"));
        assertTrue(repository.findByCode(link.code()).isPresent());
    }

    @Test
    void deleteUnknownCodeThrows() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();

        assertThrows(ServiceException.class, () -> service.deleteLink(owner, "nope12"));
    }

    @Test
    void deleteAcceptsCodeWithPrefix() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL);

        service.deleteLink(owner, "clck.ru/" + link.code());

        assertTrue(repository.findByCode(link.code()).isEmpty());
    }

    @Test
    void ownerCanDeleteExhaustedAndExpiredLinks() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link exhausted = service.createLink(owner, URL, 1);
        service.open(exhausted.code());
        Link expired = service.createLink(owner, URL);

        service.deleteLink(owner, exhausted.code());
        serviceAt(expired.expiresAt()).deleteLink(owner, expired.code());

        assertTrue(repository.findByOwner(owner).isEmpty());
    }

    @Test
    void listLinksReturnsOnlyOwnLinksOldestFirst() {
        UUID owner = UUID.randomUUID();
        Link third = serviceAt(NOW.plusSeconds(120)).createLink(owner, URL);
        Link first = serviceAt(NOW).createLink(owner, URL);
        Link second = serviceAt(NOW.plusSeconds(60)).createLink(owner, URL);
        service().createLink(UUID.randomUUID(), URL);

        assertEquals(List.of(first, second, third), service().listLinks(owner));
    }

    @Test
    void listLinksIncludesExhaustedLinks() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 1);
        service.open(link.code());

        List<Link> links = service.listLinks(owner);

        assertEquals(1, links.size());
        assertEquals(LinkStatus.LIMIT_REACHED, links.get(0).status(NOW));
    }

    @Test
    void editableLimitRangeDependsOnClickCount() {
        LinkService service = service();
        Link link = service.createLink(UUID.randomUUID(), URL, 10);
        assertEquals(1, service.minEditableLimit(link));

        service.open(link.code());
        service.open(link.code());
        service.open(link.code());
        Link opened = repository.findByCode(link.code()).orElseThrow();

        assertEquals(4, service.minEditableLimit(opened));
        assertEquals(1000, service.maxEditableLimit());
    }

    @Test
    void openNotifiesOwnerOnlyWhenLimitIsReached() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 2);

        service.open(link.code());
        assertTrue(notifications.takeAll(owner).isEmpty());

        service.open(link.code());
        List<Notification> received = notifications.takeAll(owner);
        assertEquals(1, received.size());
        assertEquals(NotificationType.LIMIT_REACHED, received.get(0).type());
        assertEquals(link.code(), received.get(0).code());
        assertEquals(URL, received.get(0).originalUrl());

        assertThrows(ServiceException.class, () -> service.open(link.code()));
        assertTrue(notifications.takeAll(owner).isEmpty());
    }

    @Test
    void removeExpiredNotifiesOwner() {
        UUID owner = UUID.randomUUID();
        Link link = service().createLink(owner, URL);

        serviceAt(link.expiresAt()).removeExpired();

        List<Notification> received = notifications.takeAll(owner);
        assertEquals(1, received.size());
        assertEquals(NotificationType.EXPIRED, received.get(0).type());
        assertEquals(link.code(), received.get(0).code());
        assertEquals(link.expiresAt(), received.get(0).createdAt());
    }

    @Test
    void exhaustedLinkGivesBothNotificationsInOrder() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 1);
        service.open(link.code());

        serviceAt(link.expiresAt()).removeExpired();

        List<NotificationType> types = notifications.takeAll(owner).stream()
            .map(Notification::type)
            .toList();
        assertEquals(List.of(NotificationType.LIMIT_REACHED, NotificationType.EXPIRED), types);
    }

    @Test
    void manualDeleteCreatesNoNotification() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL);
        service.deleteLink(owner, link.code());

        serviceAt(link.expiresAt()).removeExpired();

        assertTrue(notifications.takeAll(owner).isEmpty());
    }

    @Test
    void takeNotificationsReturnsOnceAndClears() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL, 1);
        service.open(link.code());

        assertEquals(1, service.takeNotifications(owner).size());
        assertTrue(service.takeNotifications(owner).isEmpty());
    }

    @Test
    void loginWithUnknownUuidIsNotKnown() {
        LoginResult result = service().login(UUID.randomUUID());

        assertFalse(result.isKnown());
        assertTrue(result.links().isEmpty());
        assertTrue(result.notifications().isEmpty());
    }

    @Test
    void loginShowsLinksAndNotificationsOnlyOnce() {
        LinkService service = service();
        UUID owner = UUID.randomUUID();
        Link exhausted = service.createLink(owner, URL, 1);
        service.createLink(owner, URL);
        service.open(exhausted.code());

        LoginResult first = service.login(owner);
        LoginResult second = service.login(owner);

        assertTrue(first.isKnown());
        assertEquals(2, first.links().size());
        assertEquals(1, first.notifications().size());
        assertEquals(2, second.links().size());
        assertTrue(second.notifications().isEmpty());
    }

    @Test
    void userWithOnlyNotificationsIsKnownUntilTheyAreRead() {
        UUID owner = UUID.randomUUID();
        Link link = service().createLink(owner, URL);
        serviceAt(link.expiresAt()).removeExpired();

        LoginResult first = service().login(owner);
        LoginResult second = service().login(owner);

        assertTrue(first.links().isEmpty());
        assertEquals(1, first.notifications().size());
        assertTrue(first.isKnown());
        assertFalse(second.isKnown());
    }
}
