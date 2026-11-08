package io.github.cunninglittlevixen.urlshortener.core;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Link(
    String code,
    String originalUrl,
    UUID ownerId,
    Instant createdAt,
    Instant expiresAt,
    int clickLimit,
    int clickCount) {

    public Link {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(originalUrl, "originalUrl");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(createdAt, "createAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Код ссылки не должен быть пустым");
        }
        if (clickLimit < 1) {
            throw new IllegalArgumentException("Лимит переходов должен быть не меньше 1");
        }
        if (clickCount < 0 || clickCount > clickLimit) {
            throw new IllegalArgumentException("Счётчик переходов вне допустимых границ: от 0 до 1000");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Срок действия ссылки должен заканчиваться после ее создания");
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLimitReached() {
        return clickCount >= clickLimit;
    }

    public boolean isAvailable(Instant now) {
        return !isExpired(now) && !isLimitReached();
    }

    public LinkStatus status(Instant now) {
        if (isExpired(now)) {
            return LinkStatus.EXPIRED;
        }
        if (isLimitReached()) {
            return LinkStatus.LIMIT_REACHED;
        }
        return LinkStatus.ACTIVE;
    }

    public Link withClickRegistered() {
        return new Link(code, originalUrl, ownerId, createdAt, expiresAt, clickLimit, clickCount + 1);
    }

    public Link withClickLimit(int newLimit) {
        return new Link(code, originalUrl, ownerId, createdAt, expiresAt, newLimit, clickCount);
    }
}


