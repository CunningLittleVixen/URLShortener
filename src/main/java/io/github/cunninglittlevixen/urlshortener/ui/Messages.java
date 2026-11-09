package io.github.cunninglittlevixen.urlshortener.ui;

import io.github.cunninglittlevixen.urlshortener.core.LinkStatus;
import io.github.cunninglittlevixen.urlshortener.core.Notification;
import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Messages {

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private Messages() {
    }

    public static String statusName(LinkStatus status) {
        return switch (status) {
            case ACTIVE -> "действует";
            case LIMIT_REACHED -> "лимит исчерпан";
            case EXPIRED -> "срок истёк";
        };
    }

    public static String notificationText(Notification notification, String shortUrl) {
        return switch (notification.type()) {
            case LIMIT_REACHED -> ("Лимит переходов по ссылке %s исчерпан: она больше не открывается "
                + "и будет удалена по окончании срока жизни (исходный адрес: %s). "
                + "Создайте новую ссылку командой create.")
                .formatted(shortUrl, notification.originalUrl());
            case EXPIRED -> ("Срок жизни ссылки %s истёк, ссылка удалена (исходный адрес: %s). "
                + "Создайте новую ссылку командой create.")
                .formatted(shortUrl, notification.originalUrl());
        };
    }

    public static String formatTime(Instant moment, ZoneId zone) {
        return TIME_FORMAT.format(moment.atZone(zone));
    }

    public static String help(AppConfig config) {
        return """
            Команды:
              create <url> [лимит]      создать короткую ссылку (лимит от %d до %d, по умолчанию %d)
              list                      показать ваши ссылки
              open <ссылка>             открыть ссылку в браузере, например: open clck.ru/aB3xYz
              edit-limit <код> <лимит>  изменить лимит переходов у действующей ссылки
              delete <код>              удалить свою ссылку
              whoami                    показать ваш UUID
              login <uuid>              войти по UUID (только из режима гостя)
              logout                    выйти (стать гостем)
              help                      показать эту справку
              exit                      завершить программу

            Адрес для сокращения: только http или https, с именем хоста, не длиннее 2048 символов.
            Пример: create https://www.baeldung.com/java-9-http-client 10
            """.formatted(config.minLimit(), config.maxLimit(), config.defaultLimit())
            .stripTrailing();
    }

    public static String newUserBanner(UUID userId) {
        return """
            ========================================
            Ваш новый UUID: %s
            СОХРАНИТЕ ЕГО! Он нужен, чтобы войти снова (команда login <uuid>).
            Восстановить потерянный UUID невозможно.
            ========================================
            """.formatted(userId).stripTrailing();
    }
}
