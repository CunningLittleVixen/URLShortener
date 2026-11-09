package io.github.cunninglittlevixen.urlshortener.ui;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Session {

    private UUID currentUser;

    public Optional<UUID> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isGuest() {
        return currentUser == null;
    }

    public void login(UUID userId) {
        this.currentUser = Objects.requireNonNull(userId, "userId");
    }

    public void logout() {
        this.currentUser = null;
    }
}
