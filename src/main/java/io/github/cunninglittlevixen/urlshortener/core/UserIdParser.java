package io.github.cunninglittlevixen.urlshortener.core;

import java.util.UUID;

public final class UserIdParser {

    private static final int UUID_LENGTH = 36;

    public UUID parse(String text) {
        String value = text == null ? "" : text.strip();
        if (value.length() != UUID_LENGTH) {
            throw invalid();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private ServiceException invalid() {
        return new ServiceException(
            "Некорректный UUID. Пример: 123e4567-e89b-12d3-a456-426614174000");
    }
}
