package io.github.cunninglittlevixen.urlshortener.infra;

import io.github.cunninglittlevixen.urlshortener.core.CodeGenerator;
import java.security.SecureRandom;

public final class RandomCodeGenerator implements CodeGenerator {

    private static final String ALPHABET =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public RandomCodeGenerator(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Длина кода должна быть не меньше 1");
        }
        this.length = length;
    }

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
