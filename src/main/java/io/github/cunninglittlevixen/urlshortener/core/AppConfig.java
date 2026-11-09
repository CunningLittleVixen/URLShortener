package io.github.cunninglittlevixen.urlshortener.core;

import java.nio.file.Path;
import java.time.Duration;


public record AppConfig(
        Duration linkTtl,
        int minLimit,
        int maxLimit,
        int defaultLimit,
        int codeLength,
        String prefix,
        Duration cleanupInterval,
        Path storageFile,
        Path notificationsFile) {

    public AppConfig {
        if (linkTtl.isZero() || linkTtl.isNegative()) {
            throw new IllegalArgumentException("Время жизни ссылки должно быть положительным");
        }
        if (cleanupInterval.isZero() || cleanupInterval.isNegative()) {
            throw new IllegalArgumentException("Интервал очистки должен быть положительным");
        }
        if (minLimit < 1 || maxLimit < minLimit) {
            throw new IllegalArgumentException("Границы лимита заданы неверно: минимум не меньше 1, максимум не меньше минимума");
        }
        if (defaultLimit < minLimit || defaultLimit > maxLimit) {
            throw new IllegalArgumentException("Лимит переходов по умолчанию должен быть в пределах минимума и максимума");
        }
        if (codeLength < 1) {
            throw new IllegalArgumentException("Длина кода ссылки должна быть не меньше 1");
        }
        if (storageFile.toAbsolutePath().normalize().equals(notificationsFile.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Файлы ссылок и уведомлений должны быть разными");
        }
    }
}
