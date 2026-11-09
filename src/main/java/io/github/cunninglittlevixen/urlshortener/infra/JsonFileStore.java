package io.github.cunninglittlevixen.urlshortener.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class JsonFileStore {

    private final JsonMapper mapper;
    private final Path file;

    public JsonFileStore(JsonMapper mapper, Path file) {
        this.mapper = mapper;
        this.file = file;
    }

    public <T> List<T> readList(Class<T[]> arrayType) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            String json = Files.readString(file);
            return List.of(mapper.readValue(json, arrayType));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Не удалось прочитать файл данных: " + file
                + ". Возможно, он повреждён: исправьте его или удалите.", e);
        }
    }

    public synchronized void write(List<?> items) {
        try {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            Path temp = directory.resolve(file.getFileName() + ".tmp");
            Files.writeString(temp, mapper.writeValueAsString(items));
            Files.move(temp, file,
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить файл данных: " + file, e);
        }
    }
}
