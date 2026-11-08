package io.github.cunninglittlevixen.urlshortener.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.infra.InMemoryLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.RandomCodeGenerator;
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

    private LinkService service(CodeGenerator generator) {
        return new LinkService(repository, generator, new UrlValidator(), config, clock);
    }

    private LinkService service() {
        return service(new RandomCodeGenerator(6));
    }

    private LinkService serviceAt(Instant moment) {
        return new LinkService(repository, new RandomCodeGenerator(6), new UrlValidator(),
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
}
