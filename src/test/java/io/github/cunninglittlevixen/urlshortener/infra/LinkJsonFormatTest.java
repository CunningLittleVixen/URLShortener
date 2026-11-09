package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.Link;
import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.NotificationType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LinkJsonFormatTest {

    private static final Instant CREATED = Instant.parse("2026-11-08T10:00:00Z");

    private final JsonMapper mapper = JsonMappers.create();

    private Link link() {
        return new Link("abc123", "https://example.com", UUID.randomUUID(),
            CREATED, CREATED.plusSeconds(3600), 5, 2);
    }

    @Test
    void linkSurvivesRoundTrip() {
        Link link = link();

        String json = mapper.writeValueAsString(link);

        assertEquals(link, mapper.readValue(json, Link.class));
    }

    @Test
    void timeIsWrittenAsReadableText() {
        String json = mapper.writeValueAsString(link());

        assertTrue(json.contains("2026-11-08T10:00:00Z"));
    }

    @Test
    void onlyStoredFieldsAreWritten() {
        String json = mapper.writeValueAsString(link());

        assertFalse(json.contains("limitReached"));
        assertFalse(json.contains("expired"));
    }

    @Test
    void notificationSurvivesRoundTrip() {
        Notification notification = new Notification(UUID.randomUUID(),
            NotificationType.LIMIT_REACHED, "abc123", "https://example.com", CREATED);

        String json = mapper.writeValueAsString(notification);

        assertEquals(notification, mapper.readValue(json, Notification.class));
    }

    @Test
    void invalidDataIsRejectedByLinkChecks() {
        String json = """
            {"code":"abc123","originalUrl":"https://example.com","ownerId":"%s",
             "createdAt":"2026-11-08T10:00:00Z","expiresAt":"2026-11-08T11:00:00Z",
             "clickLimit":0,"clickCount":0}
            """.formatted(UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> mapper.readValue(json, Link.class));
    }
}
