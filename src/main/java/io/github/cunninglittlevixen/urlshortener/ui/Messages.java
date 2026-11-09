package io.github.cunninglittlevixen.urlshortener.ui;

import io.github.cunninglittlevixen.urlshortener.core.LinkStatus;
import io.github.cunninglittlevixen.urlshortener.core.Notification;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
}
