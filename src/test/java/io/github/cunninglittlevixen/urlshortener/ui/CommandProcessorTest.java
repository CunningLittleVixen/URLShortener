package io.github.cunninglittlevixen.urlshortener.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import io.github.cunninglittlevixen.urlshortener.core.Link;
import io.github.cunninglittlevixen.urlshortener.core.LinkService;
import io.github.cunninglittlevixen.urlshortener.core.UrlValidator;
import io.github.cunninglittlevixen.urlshortener.core.UserIdParser;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryNotificationRepository;
import io.github.cunninglittlevixen.urlshortener.infra.RandomCodeGenerator;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommandProcessorTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final String URL = "https://example.com/page";

    private final AppConfig config = new AppConfig(Duration.ofHours(24), 1, 1000, 500, 6,
        "clck.ru/", Duration.ofMinutes(1), Path.of("data/links.json"),
        Path.of("data/notifications.json"));
    private final InMemoryLinkRepository links = new InMemoryLinkRepository();
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Session session = new Session();
    private final LinkService service = serviceAt(NOW);
    private final CommandProcessor processor = new CommandProcessor(service, new UserIdParser(),
        session, config, Clock.fixed(NOW, ZoneOffset.UTC),
        new PrintStream(buffer, true, StandardCharsets.UTF_8));

    private LinkService serviceAt(Instant moment) {
        return new LinkService(links, notifications, new RandomCodeGenerator(6),
            new UrlValidator(), config, Clock.fixed(moment, ZoneOffset.UTC));
    }

    private String run(String line) {
        buffer.reset();
        processor.execute(line);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void helpListsCommandsAndUrlRules() {
        String text = run("HELP");

        assertTrue(text.contains("create"));
        assertTrue(text.contains("edit-limit"));
        assertTrue(text.contains("http"));
    }

    @Test
    void emptyLineIsIgnored() {
        assertEquals("", run("   "));
    }

    @Test
    void unknownCommandShowsHint() {
        String text = run("dance");

        assertTrue(text.contains("Неизвестная команда"));
        assertTrue(text.contains("help"));
    }

    @Test
    void exitStopsProcessing() {
        assertFalse(processor.execute("exit"));
    }

    @Test
    void whoamiShowsGuestOrUuid() {
        assertTrue(run("whoami").contains("гость"));
        UUID id = UUID.randomUUID();
        session.login(id);

        assertTrue(run("whoami").contains(id.toString()));
    }

    @Test
    void createAsGuestIssuesUuidAndLogsIn() {
        String text = run("create " + URL + " 5");
        UUID id = session.currentUser().orElseThrow();

        assertTrue(text.contains(id.toString()));
        assertTrue(text.contains("СОХРАНИТЕ"));
        assertTrue(text.contains("clck.ru/"));
        assertEquals(5, service.listLinks(id).get(0).clickLimit());
    }

    @Test
    void secondCreateReusesUserWithoutNewUuid() {
        run("create " + URL);
        UUID id = session.currentUser().orElseThrow();

        String text = run("create " + URL);

        assertFalse(text.contains("СОХРАНИТЕ"));
        assertEquals(id, session.currentUser().orElseThrow());
        assertEquals(2, service.listLinks(id).size());
    }

    @Test
    void createWithBadInputShowsMessageAndKeepsGuest() {
        assertTrue(run("create").contains("Использование"));
        assertFalse(run("create not-a-url").isBlank());
        assertTrue(run("create " + URL + " abc").contains("целым числом"));
        assertTrue(run("create " + URL + " 0").contains("Лимит"));
        assertTrue(session.isGuest());
    }

    @Test
    void loginShowsLinksOfKnownUser() {
        UUID owner = UUID.randomUUID();
        service.createLink(owner, URL, 5);

        String text = run("login " + owner);

        assertEquals(owner, session.currentUser().orElseThrow());
        assertTrue(text.contains("clck.ru/"));
        assertTrue(text.contains("действует"));
        assertTrue(text.contains("переходы: 0 из 5"));
    }

    @Test
    void loginWithUnknownOrMalformedUuidKeepsGuest() {
        assertTrue(run("login " + UUID.randomUUID()).contains("не найден"));
        assertTrue(run("login abc").contains("UUID"));
        assertTrue(run("login").contains("Использование"));
        assertTrue(session.isGuest());
    }

    @Test
    void loginWhenAlreadyLoggedInIsRefused() {
        UUID first = UUID.randomUUID();
        session.login(first);

        String text = run("login " + UUID.randomUUID());

        assertTrue(text.contains("logout"));
        assertEquals(first, session.currentUser().orElseThrow());
    }

    @Test
    void loginWithOnlyNotificationsShowsThemOnceAndKeepsGuest() {
        UUID owner = UUID.randomUUID();
        Link link = service.createLink(owner, URL);
        serviceAt(link.expiresAt()).removeExpired();

        String first = run("login " + owner);
        String second = run("login " + owner);

        assertTrue(first.contains("истёк"));
        assertTrue(first.contains("create"));
        assertTrue(session.isGuest());
        assertTrue(second.contains("не найден"));
    }

    @Test
    void logoutReturnsToGuestAndRemindsUuid() {
        assertTrue(run("logout").contains("гость"));
        UUID id = UUID.randomUUID();
        session.login(id);

        String text = run("logout");

        assertTrue(session.isGuest());
        assertTrue(text.contains(id.toString()));
    }
}
