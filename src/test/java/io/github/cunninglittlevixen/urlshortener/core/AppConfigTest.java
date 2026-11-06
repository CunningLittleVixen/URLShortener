package io.github.cunninglittlevixen.urlshortener.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;

public class AppConfigTest {

    private AppConfig create(int minLimit, int maxLimit, int defaultLimit) {
        return new AppConfig(Duration.ofHours(24), minLimit, maxLimit, defaultLimit, 6, "clck.ru/", Duration.ofMinutes(1), Path.of("data/links.json"));
    }

    @Test
    void createsConfigWithValidValues() {
        AppConfig config = create(1, 1000, 1000);

        assertEquals(1000, config.maxLimit());
    }

    @Test
    void failWhenMinLimitIsGreaterThanMaxLimit() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> create(5, 1, 1));

        assertTrue(e.getMessage().contains("Границы лимита"));
    }

    @Test
    void failsWhenDefaultLimitIsOutOfRange() {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> create(1, 1000, 2000));

        assertTrue(e.getMessage().contains("по умолчанию"));
    }
}
