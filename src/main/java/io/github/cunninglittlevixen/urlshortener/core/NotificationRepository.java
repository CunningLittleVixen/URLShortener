package io.github.cunninglittlevixen.urlshortener.core;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository {

    void add(Notification notification);

    List<Notification> takeAll(UUID ownerId);
}
