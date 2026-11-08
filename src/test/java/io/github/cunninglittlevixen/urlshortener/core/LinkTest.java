package io.github.cunninglittlevixen.urlshortener.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkTest {

    private static final Instant CREATED = Instant.parse("2026-11-08T10:00:00Z");
    private static final Instant EXPIRES = CREATED.plusSeconds(3600);

    private Link link(int limit, int count) {
        return new Link("abc123", "https://example.com", UUID.randomUUID(),
            CREATED, EXPIRES, limit, count);
    }

    @Test
    void isExpiredAtExpirationMoment() {
        Link link = link(5, 0);

        assertFalse(link.isExpired(EXPIRES.minusSeconds(1)));
        assertTrue(link.isExpired(EXPIRES));
    }

    @Test
    void isLimitReachedOnlyWhenCountEqualsLimit() {
        assertTrue(link(3, 3).isLimitReached());
        assertFalse(link(3, 2).isLimitReached());
    }

    @Test
    void isAvailableOnlyBeforeExpirationAndBelowLimit() {
        assertTrue(link(3, 0).isAvailable(CREATED));
        assertFalse(link(3, 0).isAvailable(EXPIRES));
        assertFalse(link(1, 1).isAvailable(CREATED));
    }

    @Test
    void withClickRegisteredIncrementsCountWithoutChangingOriginal() {
        Link original = link(5, 0);

        Link updated = original.withClickRegistered();

        assertEquals(1, updated.clickCount());
        assertEquals(0, original.clickCount());
    }

    @Test
    void withClickRegisteredFailsWhenLimitReached() {
        assertThrows(IllegalArgumentException.class, () -> link(1, 1).withClickRegistered());
    }

    @Test
    void withClickLimitChangesLimitAndKeepsCount() {
        Link updated = link(5, 2).withClickLimit(10);

        assertEquals(10, updated.clickLimit());
        assertEquals(2, updated.clickCount());
    }

    @Test
    void rejectsZeroLimit() {
        assertThrows(IllegalArgumentException.class, () -> link(0, 0));
    }

    @Test
    void rejectsBlankCode() {
        assertThrows(IllegalArgumentException.class, () ->
            new Link("  ", "https://example.com", UUID.randomUUID(), CREATED, EXPIRES, 5, 0));
    }

    @Test
    void rejectsExpirationNotAfterCreation() {
        assertThrows(IllegalArgumentException.class, () ->
            new Link("abc123", "https://example.com", UUID.randomUUID(), CREATED, CREATED, 5, 0));
    }

    @Test
    void statusReflectsExpiryAndLimitWithExpiryFirst() {
        Instant created = Instant.parse("2026-11-08T10:00:00Z");
        Instant expires = created.plusSeconds(3600);
        UUID owner = UUID.randomUUID();
        Link active = new Link("abc123", "https://example.com", owner, created, expires, 5, 0);
        Link exhausted = new Link("abc124", "https://example.com", owner, created, expires, 5, 5);

        assertEquals(LinkStatus.ACTIVE, active.status(created));
        assertEquals(LinkStatus.LIMIT_REACHED, exhausted.status(created));
        assertEquals(LinkStatus.EXPIRED, active.status(expires));
        assertEquals(LinkStatus.EXPIRED, exhausted.status(expires));
    }
}
