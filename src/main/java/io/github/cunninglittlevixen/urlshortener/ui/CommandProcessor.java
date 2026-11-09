package io.github.cunninglittlevixen.urlshortener.ui;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import io.github.cunninglittlevixen.urlshortener.core.Link;
import io.github.cunninglittlevixen.urlshortener.core.LinkService;
import io.github.cunninglittlevixen.urlshortener.core.LinkStatus;
import io.github.cunninglittlevixen.urlshortener.core.LoginResult;
import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.ServiceException;
import io.github.cunninglittlevixen.urlshortener.core.UserIdParser;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CommandProcessor {

    private final LinkService service;
    private final UserIdParser userIdParser;
    private final Session session;
    private final Browser browser;
    private final AppConfig config;
    private final Clock clock;
    private final PrintStream out;

    public CommandProcessor(LinkService service, UserIdParser userIdParser, Session session, Browser browser,
                            AppConfig config, Clock clock, PrintStream out) {
        this.service = service;
        this.userIdParser = userIdParser;
        this.session = session;
        this.browser = browser;
        this.config = config;
        this.clock = clock;
        this.out = out;
    }

    /** Выполняет одну команду. Возвращает false, если программу нужно завершить. */
    public boolean execute(String line) {
        String text = line == null ? "" : line.strip();
        if (text.isEmpty()) {
            return true;
        }
        String[] parts = text.split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        try {
            switch (command) {
                case "help" -> out.println(Messages.help(config));
                case "whoami" -> whoami();
                case "login" -> login(args);
                case "logout" -> logout();
                case "create" -> create(args);
                case "list" -> list(args);
                case "open" -> open(args);
                case "edit-limit" -> editLimit(args);
                case "delete" -> delete(args);
                case "exit" -> {
                    out.println("До свидания!");
                    return false;
                }
                default -> throw new ServiceException("Неизвестная команда: " + parts[0]
                    + ". Введите help, чтобы увидеть список команд.");
            }
        } catch (ServiceException e) {
            out.println(e.getMessage());
        }
        return true;
    }

    public void showPendingNotifications() {
        session.currentUser().ifPresent(
            userId -> printNotifications(service.takeNotifications(userId)));
    }

    private void whoami() {
        session.currentUser().ifPresentOrElse(
            id -> out.println("Ваш UUID: " + id),
            () -> out.println("Вы гость. Выполните create <url>, чтобы создать ссылку, "
                + "или login <uuid>, чтобы войти."));
    }

    private void login(String[] args) {
        requireArgs(args, 1, 1, "login <uuid>");
        if (!session.isGuest()) {
            throw new ServiceException("Вы уже вошли как " + session.currentUser().orElseThrow()
                + ". Сначала выполните logout.");
        }
        UUID userId = userIdParser.parse(args[0]);
        LoginResult result = service.login(userId);
        if (!result.isKnown()) {
            out.println("Пользователь с таким UUID не найден (возможно, все его ссылки уже истекли). "
                + "Вы остаётесь гостем. Чтобы начать, выполните create <url>.");
            return;
        }
        printNotifications(result.notifications());
        if (result.links().isEmpty()) {
            out.println("У вас больше нет ссылок, поэтому вход не выполнен. "
                + "Чтобы продолжить, выполните create <url> — вам будет выдан новый UUID.");
            return;
        }
        session.login(userId);
        out.println("Вы вошли как " + userId + ".");
        printLinks(result.links());
    }

    private void logout() {
        if (session.isGuest()) {
            out.println("Вы и так гость.");
            return;
        }
        UUID userId = session.currentUser().orElseThrow();
        session.logout();
        out.println("Вы вышли. Ваш UUID: " + userId
            + " — сохраните его, он понадобится для входа (login <uuid>).");
    }

    private void create(String[] args) {
        requireArgs(args, 1, 2, "create <url> [лимит]");
        UUID owner = session.currentUser().orElseGet(UUID::randomUUID);
        Link link = args.length == 2
            ? service.createLink(owner, args[0], parseLimit(args[1]))
            : service.createLink(owner, args[0]);
        if (session.isGuest()) {
            session.login(owner);
            out.println(Messages.newUserBanner(owner));
        }
        out.println("Короткая ссылка создана: " + service.shortUrl(link));
        out.println("Исходный адрес: " + link.originalUrl());
        out.println("Лимит переходов: " + link.clickLimit() + ". Действует до: "
            + Messages.formatTime(link.expiresAt(), clock.getZone()));
    }

    private void list(String[] args) {
        requireArgs(args, 0, 0, "list");
        List<Link> links = service.listLinks(requireUser());
        if (links.isEmpty()) {
            out.println("У вас пока нет ссылок. Создайте первую командой create <url>.");
            return;
        }
        out.println("Ваши ссылки:");
        printLinks(links);
    }

    private void open(String[] args) {
        requireArgs(args, 1, 1, "open <ссылка>");
        String url = service.open(args[0]);
        if (browser.open(url)) {
            out.println("Открываю в браузере: " + url);
        } else {
            out.println("Не удалось открыть браузер автоматически. Переход засчитан. "
                + "Скопируйте адрес и откройте его вручную: " + url);
        }
    }

    private void editLimit(String[] args) {
        requireArgs(args, 2, 2, "edit-limit <код> <лимит>");
        UUID userId = requireUser();
        Link updated = service.editLimit(userId, args[0], parseLimit(args[1]));
        out.println("Лимит изменён: " + updated.clickLimit()
            + " (уже использовано переходов: " + updated.clickCount() + ").");
    }

    private void delete(String[] args) {
        requireArgs(args, 1, 1, "delete <код>");
        service.deleteLink(requireUser(), args[0]);
        out.println("Ссылка удалена.");
    }

    private UUID requireUser() {
        return session.currentUser().orElseThrow(
            () -> new ServiceException("Сначала выполните create <url> или login <uuid>."));
    }

    private void printNotifications(List<Notification> notifications) {
        for (Notification notification : notifications) {
            out.println("Уведомление: "
                + Messages.notificationText(notification, config.prefix() + notification.code()));
        }
    }

    private void printLinks(List<Link> links) {
        Instant now = clock.instant();
        for (Link link : links) {
            LinkStatus status = link.status(now);
            out.println("- " + service.shortUrl(link) + " -> " + link.originalUrl());
            out.println("    статус: " + Messages.statusName(status)
                + "; переходы: " + link.clickCount() + " из " + link.clickLimit()
                + "; срок до: " + Messages.formatTime(link.expiresAt(), clock.getZone()));
            if (status == LinkStatus.ACTIVE) {
                out.println("    лимит можно менять от " + service.minEditableLimit(link)
                    + " до " + service.maxEditableLimit());
            }
        }
    }

    private void requireArgs(String[] args, int min, int max, String usage) {
        if (args.length < min || args.length > max) {
            throw new ServiceException("Использование: " + usage);
        }
    }

    private int parseLimit(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ServiceException(
                "Лимит должен быть целым числом, например: 5");
        }
    }
}
