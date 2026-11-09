package io.github.cunninglittlevixen.urlshortener.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTest {

    private final Session session = new Session();

    @Test
    void startsAsGuest() {
        assertTrue(session.isGuest());
        assertTrue(session.currentUser().isEmpty());
    }

    @Test
    void loginMakesUserActive() {
        UUID id = UUID.randomUUID();

        session.login(id);

        assertFalse(session.isGuest());
        assertEquals(id, session.currentUser().orElseThrow());
    }

    @Test
    void logoutReturnsToGuest() {
        session.login(UUID.randomUUID());

        session.logout();

        assertTrue(session.isGuest());
    }

    @Test
    void loginRejectsNull() {
        assertThrows(NullPointerException.class, () -> session.login(null));
    }
}
