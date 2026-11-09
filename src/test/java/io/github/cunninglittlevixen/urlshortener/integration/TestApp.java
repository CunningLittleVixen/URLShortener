package io.github.cunninglittlevixen.urlshortener.integration;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import io.github.cunninglittlevixen.urlshortener.core.LinkRepository;
import io.github.cunninglittlevixen.urlshortener.core.LinkService;
import io.github.cunninglittlevixen.urlshortener.core.NotificationRepository;
import io.github.cunninglittlevixen.urlshortener.core.UrlValidator;
import io.github.cunninglittlevixen.urlshortener.core.UserIdParser;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryNotificationRepository;
import io.github.cunninglittlevixen.urlshortener.infra.JsonFileStore;
import io.github.cunninglittlevixen.urlshortener.infra.JsonLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.JsonMappers;
import io.github.cunninglittlevixen.urlshortener.infra.JsonNotificationRepository;
import io.github.cunninglittlevixen.urlshortener.infra.RandomCodeGenerator;
import io.github.cunninglittlevixen.urlshortener.ui.CommandProcessor;
import io.github.cunninglittlevixen.urlshortener.ui.FakeBrowser;
import io.github.cunninglittlevixen.urlshortener.ui.Session;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

final class TestApp {

    private static final Pattern CODE = Pattern.compile("clck\\.ru/([0-9A-Za-z]{6})");
    private static final Pattern UUID_TEXT = Pattern.compile("UUID: ([0-9a-f-]{36})");

    final FakeBrowser browser = new FakeBrowser();
    private final LinkService service;
    private final CommandProcessor processor;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    TestApp(Path dir, Clock clock, Duration ttl) {
        AppConfig config = new AppConfig(ttl, 1, 1000, 1000, 6, "clck.ru/",
            Duration.ofMinutes(1), dir.resolve("links.json"), dir.resolve("notifications.json"));
        JsonMapper mapper = JsonMappers.create();
        LinkRepository links = new JsonLinkRepository(new InMemoryLinkRepository(),
            new JsonFileStore(mapper, config.storageFile()));
        NotificationRepository notifications = new JsonNotificationRepository(
            new InMemoryNotificationRepository(),
            new JsonFileStore(mapper, config.notificationsFile()));
        service = new LinkService(links, notifications, new RandomCodeGenerator(6),
            new UrlValidator(), config, clock);
        processor = new CommandProcessor(service, new UserIdParser(), new Session(),
            browser, config, clock, new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }

    String run(String line) {
        buffer.reset();
        if (processor.execute(line)) {
            processor.showPendingNotifications();
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    void cleanup() {
        service.removeExpired();
    }

    static String codeIn(String output) {
        return find(CODE, output);
    }

    static String uuidIn(String output) {
        return find(UUID_TEXT, output);
    }

    private static String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new AssertionError("В выводе не найдено " + pattern + ":\n" + text);
        }
        return matcher.group(1);
    }
}
