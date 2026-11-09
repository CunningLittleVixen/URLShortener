package io.github.cunninglittlevixen.urlshortener.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import io.github.cunninglittlevixen.urlshortener.core.CodeGenerator;
import io.github.cunninglittlevixen.urlshortener.core.LinkService;
import io.github.cunninglittlevixen.urlshortener.core.UrlValidator;
import io.github.cunninglittlevixen.urlshortener.core.UserIdParser;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryLinkRepository;
import io.github.cunninglittlevixen.urlshortener.infra.InMemoryNotificationRepository;
import io.github.cunninglittlevixen.urlshortener.infra.RandomCodeGenerator;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleAppTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final String URL = "https://example.com/page";

    private final AppConfig config = new AppConfig(Duration.ofHours(24), 1, 1000, 500, 6,
        "clck.ru/", Duration.ofMinutes(1), Path.of("data/links.json"),
        Path.of("data/notifications.json"));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final FakeBrowser browser = new FakeBrowser();

    private String run(CodeGenerator generator, String input) {
        LinkService service = new LinkService(new InMemoryLinkRepository(),
            new InMemoryNotificationRepository(), generator, new UrlValidator(), config, clock);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        CommandProcessor processor = new CommandProcessor(service, new UserIdParser(),
            new Session(), browser, config, clock, out);
        new ConsoleApp(processor, new BufferedReader(new StringReader(input)), out).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private String run(String input) {
        return run(new RandomCodeGenerator(6), input);
    }

    @Test
    void loopRunsCommandsUntilExit() {
        String text = run("help\nexit\nwhoami\n");

        assertTrue(text.contains("Команды"));
        assertTrue(text.contains("До свидания"));
        assertTrue(!text.contains("Вы гость. Выполните"));
    }

    @Test
    void endOfInputEndsProgramPolitely() {
        assertTrue(run("").contains("Ввод завершён"));
    }

    @Test
    void userErrorsDoNotStopTheLoop() {
        String text = run("create not-a-url\nwhoami\nexit\n");

        assertTrue(text.contains("Вы гость"));
        assertTrue(text.contains("До свидания"));
    }

    @Test
    void ownerSeesNotificationRightAfterLastAllowedClick() {
        String text = run(() -> "aaa111", "create " + URL + " 1\nopen aaa111\nexit\n");

        assertEquals(List.of(URL), browser.opened());
        assertTrue(text.contains("Открываю в браузере"));
        assertTrue(text.contains("Уведомление:"));
        assertTrue(text.contains("исчерпан"));
    }

    @Test
    void unexpectedErrorIsReportedAndLoopContinues() {
        String text = run(() -> "same11",
            "create " + URL + "\ncreate " + URL + "\nwhoami\nexit\n");

        assertTrue(text.contains("Внутренняя ошибка"));
        assertTrue(text.contains("Ваш UUID"));
    }
}
