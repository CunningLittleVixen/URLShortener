package io.github.cunninglittlevixen.urlshortener.core;

import java.nio.file.Path;
import java.time.Duration;


public record AppConfig(
        Duration linkTtl,
        int minLimit,
        int maxLimit,
        int codeLength,
        String prefix,
        Duration cleanupInterval,
        Path storageFile) {

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
        if (codeLength < 1) {
            throw new IllegalArgumentException("Длина кода ссылки должна быть не меньше 1");
        }
    }
}
