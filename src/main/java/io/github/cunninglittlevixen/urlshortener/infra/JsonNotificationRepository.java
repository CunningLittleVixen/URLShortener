package io.github.cunninglittlevixen.urlshortener.infra;

import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.NotificationRepository;
import java.util.List;
import java.util.UUID;

public final class JsonNotificationRepository implements NotificationRepository {

    private final NotificationRepository delegate;
    private final JsonFileStore store;

    public JsonNotificationRepository(NotificationRepository delegate, JsonFileStore store) {
        this.delegate = delegate;
        this.store = store;
        for (Notification notification : store.readList(Notification[].class)) {
            delegate.add(notification);
        }
    }

    @Override
    public synchronized void add(Notification notification) {
        delegate.add(notification);
        save();
    }

    @Override
    public synchronized List<Notification> takeAll(UUID ownerId) {
        List<Notification> taken = delegate.takeAll(ownerId);
        if (!taken.isEmpty()) {
            save();
        }
        return taken;
    }

    @Override
    public List<Notification> findAll() {
        return delegate.findAll();
    }

    private void save() {
        store.write(delegate.findAll());
    }
}
