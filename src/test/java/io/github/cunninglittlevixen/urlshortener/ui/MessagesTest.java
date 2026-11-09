package io.github.cunninglittlevixen.urlshortener.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.LinkStatus;
import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.NotificationType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final String SHORT_URL = "clck.ru/aB3xYz";

    private Notification notification(NotificationType type) {
        return new Notification(UUID.randomUUID(), type, "aB3xYz", "https://example.com", NOW);
    }

    @Test
    void statusNamesAreInRussian() {
        assertEquals("действует", Messages.statusName(LinkStatus.ACTIVE));
        assertEquals("лимит исчерпан", Messages.statusName(LinkStatus.LIMIT_REACHED));
        assertEquals("срок истёк", Messages.statusName(LinkStatus.EXPIRED));
    }

    @Test
    void limitReachedNotificationNamesLinkAndSuggestsCreate() {
        String text = Messages.notificationText(notification(NotificationType.LIMIT_REACHED), SHORT_URL);

        assertTrue(text.contains(SHORT_URL));
        assertTrue(text.contains("https://example.com"));
        assertTrue(text.contains("исчерпан"));
        assertTrue(text.contains("create"));
    }

    @Test
    void expiredNotificationNamesLinkAndSuggestsCreate() {
        String text = Messages.notificationText(notification(NotificationType.EXPIRED), SHORT_URL);

        assertTrue(text.contains(SHORT_URL));
        assertTrue(text.contains("истёк"));
        assertTrue(text.contains("удалена"));
        assertTrue(text.contains("create"));
    }

    @Test
    void formatTimeUsesGivenZone() {
        assertEquals("08.11.2026 13:00:00", Messages.formatTime(NOW, ZoneId.of("Europe/Moscow")));
        assertEquals("08.11.2026 10:00:00", Messages.formatTime(NOW, ZoneId.of("UTC")));
    }
}
