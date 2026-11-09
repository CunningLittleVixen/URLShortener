package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.NotificationType;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonNotificationRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    @TempDir
    Path dir;

    private JsonNotificationRepository open() {
        return new JsonNotificationRepository(new InMemoryNotificationRepository(),
            new JsonFileStore(JsonMappers.create(), dir.resolve("notifications.json")));
    }

    private Notification notification(UUID owner, String code) {
        return new Notification(owner, NotificationType.EXPIRED, code, "https://example.com", NOW);
    }

    @Test
    void addedNotificationsSurviveRestartInOrder() {
        UUID owner = UUID.randomUUID();
        Notification first = notification(owner, "aaa111");
        Notification second = notification(owner, "bbb222");
        JsonNotificationRepository repository = open();
        repository.add(first);
        repository.add(second);

        assertEquals(List.of(first, second), open().takeAll(owner));
    }

    @Test
    void takenNotificationsAreRemovedFromFile() {
        UUID owner = UUID.randomUUID();
        JsonNotificationRepository repository = open();
        repository.add(notification(owner, "aaa111"));
        repository.takeAll(owner);

        assertTrue(open().takeAll(owner).isEmpty());
    }

    @Test
    void otherOwnersKeepTheirNotifications() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        JsonNotificationRepository repository = open();
        repository.add(notification(alice, "aaa111"));
        repository.add(notification(bob, "bbb222"));
        repository.takeAll(alice);

        assertEquals(1, open().takeAll(bob).size());
    }
}
