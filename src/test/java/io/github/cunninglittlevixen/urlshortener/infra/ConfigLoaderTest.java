package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    private Properties validProperties() {
        Properties p = new Properties();
        p.setProperty("link.ttl", "PT24H");
        p.setProperty("link.min-limit", "1");
        p.setProperty("link.max-limit", "1000");
        p.setProperty("link.default-limit", "1000");
        p.setProperty("link.code-length", "6");
        p.setProperty("link.prefix", "clck.ru/");
        p.setProperty("cleanup.interval", "PT1M");
        p.setProperty("storage.file", "data/links.json");
        return p;
    }

    @Test
    void readsValidProperties() {
        AppConfig config = loader.fromProperties(validProperties());

        assertEquals(Duration.ofHours(24), config.linkTtl());
        assertEquals(1000, config.maxLimit());
    }

    @Test
    void failsWhenKeyIsMissing() {
        Properties p = validProperties();
        p.remove("link.ttl");

        IllegalStateException e =
            assertThrows(IllegalStateException.class, () -> loader.fromProperties(p));

        assertTrue(e.getMessage().contains("link.ttl"));
    }

    @Test
    void failsWhenValueIsBlank() {
        Properties p = validProperties();
        p.setProperty("link.prefix", "    ");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> loader.fromProperties(p));

        assertTrue(e.getMessage().contains("link.prefix"));
    }

    @Test
    void failsWhenTtlHasWrongFormat() {
        Properties p = validProperties();
        p.setProperty("link.ttl", "сутки");

        assertThrows(IllegalStateException.class, () -> loader.fromProperties(p));
    }

    @Test
    void failsWhenLimitIsNotNumber() {
        Properties p = validProperties();
        p.setProperty("link.max-limit", "abc");

        assertThrows(IllegalStateException.class, () -> loader.fromProperties(p));
    }

    @Test
    void failsWhenResourceIsMissing() {
        assertThrows(IllegalStateException.class, () -> loader.load("/no-such-file.properties"));
    }

    @Test
    void loadsRealConfigFile() {
        AppConfig config = loader.load();

        assertEquals("clck.ru/", config.prefix());
    }
}
