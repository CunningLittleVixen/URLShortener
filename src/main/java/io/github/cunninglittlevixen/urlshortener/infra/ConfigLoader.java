package io.github.cunninglittlevixen.urlshortener.infra;

import io.github.cunninglittlevixen.urlshortener.core.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public final class ConfigLoader {

    private static final String DEFAULT_RESOURCE = "/app.properties";

    public AppConfig load() {
        return load(DEFAULT_RESOURCE);
    }

    public AppConfig load(String resourceName) {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getResourceAsStream(resourceName)) {
            if (in == null ) {
                throw new IllegalStateException("Файл конфигурации не найден: " + resourceName);
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать конфигурацию: " + resourceName, e);
        }
        return fromProperties(props);
    }

    public AppConfig fromProperties(Properties props) {
        try {
            return new AppConfig(
                Duration.parse(required(props, "link.ttl")),
                Integer.parseInt(required(props, "link.min-limit")),
                Integer.parseInt(required(props, "link.max-limit")),
                Integer.parseInt(required(props, "link.default-limit")),
                Integer.parseInt(required(props, "link.code-length")),
                required(props, "link.prefix"),
                Duration.parse(required(props, "cleanup.interval")),
                Path.of(required(props, "storage.file"))
            );
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalStateException("Неверное значение в конфигурации: " + e.getMessage(), e);
        }
    }

    private String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("В конфигурации не задан параметр: " + key);
        }
        return value.trim();
    }
}
