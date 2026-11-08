package io.github.cunninglittlevixen.urlshortener.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    @Test
    void rejectsBlankCode() {
        assertThrows(IllegalArgumentException.class, () -> new Notification(
            UUID.randomUUID(), NotificationType.EXPIRED, " ", "https://example.com", NOW));
    }

    @Test
    void rejectsMissingType() {
        assertThrows(NullPointerException.class, () -> new Notification(
            UUID.randomUUID(), null, "abc123", "https://example.com", NOW));
    }
}
