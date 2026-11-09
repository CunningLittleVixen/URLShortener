package io.github.cunninglittlevixen.urlshortener.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShortenerScenariosTest {

    private static final Instant START = Instant.parse("2026-11-08T10:00:00Z");
    private static final String URL = "https://www.baeldung.com/java-9-http-client";

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(START);

    private TestApp app(Duration ttl) {
        return new TestApp(dir, clock, ttl);
    }

    private TestApp app() {
        return app(Duration.ofHours(24));
    }

    @Test
    void sameUrlFromDifferentUsersGetsDifferentCodes() {
        TestApp app = app();
        String first = app.run("create " + URL);
        app.run("logout");
        String second = app.run("create " + URL);

        assertNotEquals(TestApp.codeIn(first), TestApp.codeIn(second));
        assertNotEquals(TestApp.uuidIn(first), TestApp.uuidIn(second));
        String list = app.run("list");
        assertTrue(list.contains(TestApp.codeIn(second)));
        assertFalse(list.contains(TestApp.codeIn(first)));
    }

    @Test
    void exhaustedLinkIsBlockedAndOwnerIsNotified() {
        TestApp app = app();
        String code = TestApp.codeIn(app.run("create " + URL + " 2"));

        app.run("open " + code);
        String last = app.run("open " + code);
        String blocked = app.run("open " + code);

        assertTrue(last.contains("Уведомление:"));
        assertTrue(blocked.contains("исчерпан"));
        assertEquals(2, app.browser.opened().size());
        assertTrue(app.run("list").contains("лимит исчерпан"));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 24})
    void expiredLinkIsRemovedAndOwnerIsNotified(long hours) {
        Duration ttl = Duration.ofHours(hours);
        TestApp app = app(ttl);
        String code = TestApp.codeIn(app.run("create " + URL));

        clock.advance(ttl.minusSeconds(1));
        app.cleanup();
        assertTrue(app.run("list").contains("действует"));

        clock.advance(Duration.ofSeconds(1));
        app.cleanup();
        String afterRemoval = app.run("whoami");

        assertTrue(afterRemoval.contains("Уведомление:"));
        assertTrue(afterRemoval.contains("истёк"));
        assertTrue(app.run("list").contains("нет ссылок"));
        assertTrue(app.run("open " + code).contains("не найдена"));
    }

    @Test
    void linksAndCountersSurviveRestart() {
        TestApp first = app();
        String created = first.run("create " + URL + " 2");
        String code = TestApp.codeIn(created);
        String uuid = TestApp.uuidIn(created);
        first.run("open " + code);

        TestApp second = app();
        second.run("login " + uuid);

        assertTrue(second.run("list").contains("переходы: 1 из 2"));
        assertTrue(second.run("open " + code).contains("Уведомление:"));
    }

    @Test
    void unreadNotificationsSurviveRestartAndAreShownOnce() {
        Duration ttl = Duration.ofHours(1);
        TestApp first = app(ttl);
        String uuid = TestApp.uuidIn(first.run("create " + URL));
        clock.advance(ttl);
        first.cleanup();

        TestApp second = app(ttl);

        assertTrue(second.run("login " + uuid).contains("истёк"));
        assertTrue(second.run("login " + uuid).contains("не найден"));
    }

    @Test
    void onlyOwnerCanChangeOrDeleteLinkButAnyoneCanOpenIt() {
        TestApp app = app();
        String code = TestApp.codeIn(app.run("create " + URL + " 5"));
        assertTrue(app.run("edit-limit " + code + " 10").contains("Лимит изменён"));
        app.run("logout");
        app.run("create " + URL);

        assertTrue(app.run("edit-limit " + code + " 20").contains("другому пользователю"));
        assertTrue(app.run("delete " + code).contains("другому пользователю"));

        app.run("logout");
        assertTrue(app.run("open " + code).contains("Открываю"));
        assertEquals(List.of(URL), app.browser.opened());
    }
}
