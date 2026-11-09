package io.github.cunninglittlevixen.urlshortener.infra;

import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryNotificationRepository implements NotificationRepository {

    private final ConcurrentMap<UUID, List<Notification>> notifications = new ConcurrentHashMap<>();

    @Override
    public void add(Notification notification) {
        notifications.compute(notification.ownerId(), (owner, current) -> {
            List<Notification> updated = new ArrayList<>();
            if (current != null) {
                updated.addAll(current);
            }
            updated.add(notification);
            return List.copyOf(updated);
        });
    }

    @Override
    public List<Notification> takeAll(UUID ownerId) {
        List<Notification> taken = notifications.remove(ownerId);
        return taken == null ? List.of() : taken;
    }

    @Override
    public List<Notification> findAll() {
        return notifications.values().stream()
            .flatMap(List::stream)
            .toList();
    }
}
