package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InMemoryNotificationRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    private final InMemoryNotificationRepository repository = new InMemoryNotificationRepository();

    private Notification notification(UUID owner, String code) {
        return new Notification(owner, NotificationType.EXPIRED, code, "https://example.com", NOW);
    }

    @Test
    void takeAllReturnsInOrderAndClears() {
        UUID owner = UUID.randomUUID();
        Notification first = notification(owner, "aaa111");
        Notification second = notification(owner, "bbb222");
        repository.add(first);
        repository.add(second);

        assertEquals(List.of(first, second), repository.takeAll(owner));
        assertTrue(repository.takeAll(owner).isEmpty());
    }

    @Test
    void takeAllForUnknownOwnerIsEmpty() {
        assertTrue(repository.takeAll(UUID.randomUUID()).isEmpty());
    }

    @Test
    void notificationsOfDifferentOwnersAreSeparate() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        Notification forAlice = notification(alice, "aaa111");
        repository.add(forAlice);
        repository.add(notification(bob, "bbb222"));

        assertEquals(List.of(forAlice), repository.takeAll(alice));
        assertEquals(1, repository.takeAll(bob).size());
    }

    @Test
    void parallelAddsAreNotLost() {
        UUID owner = UUID.randomUUID();

        IntStream.range(0, 1000).parallel()
            .forEach(i -> repository.add(notification(owner, "code" + i)));

        assertEquals(1000, repository.takeAll(owner).size());
    }

    @Test
    void findAllReturnsEverythingWithoutRemoving() {
        repository.add(notification(UUID.randomUUID(), "aaa111"));
        repository.add(notification(UUID.randomUUID(), "bbb222"));

        assertEquals(2, repository.findAll().size());
        assertEquals(2, repository.findAll().size());
    }
}
