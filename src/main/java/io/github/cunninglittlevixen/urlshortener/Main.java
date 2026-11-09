package io.github.cunninglittlevixen.urlshortener;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import io.github.cunninglittlevixen.urlshortener.core.LinkRepository;
import io.github.cunninglittlevixen.urlshortener.core.LinkService;
import io.github.cunninglittlevixen.urlshortener.core.NotificationRepository;
import io.github.cunninglittlevixen.urlshortener.core.UrlValidator;
import io.github.cunninglittlevixen.urlshortener.core.UserIdParser;
import io.github.cunninglittlevixen.urlshortener.infra.CleanupScheduler;
import io.github.cunninglittlevixen.urlshortener.infra.ConfigLoader;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryNotificationRepository;
import io.github.cunninglittlevixen.urlshortener.infra.JsonFileStore;
import io.github.cunninglittlevixen.urlshortener.infra.JsonLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.JsonMappers;
import io.github.cunninglittlevixen.urlshortener.infra.JsonNotificationRepository;
import io.github.cunninglittlevixen.urlshortener.infra.RandomCodeGenerator;
import io.github.cunninglittlevixen.urlshortener.ui.CommandProcessor;
import io.github.cunninglittlevixen.urlshortener.ui.ConsoleApp;
import io.github.cunninglittlevixen.urlshortener.ui.DesktopBrowser;
import io.github.cunninglittlevixen.urlshortener.ui.Session;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import tools.jackson.databind.json.JsonMapper;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try {
            run(out);
        } catch (IllegalStateException | IllegalArgumentException e) {
            out.println("Не удалось запустить приложение: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(PrintStream out) {
        AppConfig config = new ConfigLoader().load();
        Clock clock = Clock.systemDefaultZone();
        JsonMapper mapper = JsonMappers.create();

        LinkRepository links = new JsonLinkRepository(new InMemoryLinkRepository(),
            new JsonFileStore(mapper, config.storageFile()));
        NotificationRepository notifications = new JsonNotificationRepository(
            new InMemoryNotificationRepository(),
            new JsonFileStore(mapper, config.notificationsFile()));

        LinkService service = new LinkService(links, notifications,
            new RandomCodeGenerator(config.codeLength()), new UrlValidator(), config, clock);
        CommandProcessor processor = new CommandProcessor(service, new UserIdParser(),
            new Session(), new DesktopBrowser(), config, clock, out);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8));

        CleanupScheduler scheduler = new CleanupScheduler(
            config.cleanupInterval(), service::removeExpired);
        scheduler.start();
        try {
            new ConsoleApp(processor, in, out).run();
        } finally {
            scheduler.stop();
        }
    }
}
