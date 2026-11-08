package io.github.cunninglittlevixen.urlshortener.core;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Notification(
    UUID ownerId,
    NotificationType type,
    String code,
    String originalUrl,
    Instant createdAt) {

    public Notification {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(originalUrl, "originalUrl");
        Objects.requireNonNull(createdAt, "createdAt");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Код ссылки в уведомлении не должен быть пустым");
        }
    }
}
