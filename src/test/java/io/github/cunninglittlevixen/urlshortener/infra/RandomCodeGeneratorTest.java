package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomCodeGeneratorTest {

    @Test
    void generatesCodeOfRequestedLength() {
        RandomCodeGenerator generator = new RandomCodeGenerator(6);

        assertEquals(6, generator.generate().length());
    }

    @Test
    void usesOnlyLettersAndDigits() {
        RandomCodeGenerator generator = new RandomCodeGenerator(50);

        assertTrue(generator.generate().matches("[0-9A-Za-z]+"));
    }

    @Test
    void generatesDifferentCodes() {
        RandomCodeGenerator generator = new RandomCodeGenerator(10);
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }

        assertEquals(1000, codes.size());
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new RandomCodeGenerator(0));
    }
}
