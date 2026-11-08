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
}
