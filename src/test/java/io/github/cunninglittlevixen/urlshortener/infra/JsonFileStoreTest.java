package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.Link;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonFileStoreTest {

    private static final Instant CREATED = Instant.parse("2026-11-08T10:00:00Z");

    @TempDir
    Path dir;

    private Link link(String code) {
        return new Link(code, "https://example.com", UUID.randomUUID(),
            CREATED, CREATED.plusSeconds(3600), 5, 0);
    }

    private JsonFileStore store(Path file) {
        return new JsonFileStore(JsonMappers.create(), file);
    }

    @Test
    void missingFileGivesEmptyList() {
        assertTrue(store(dir.resolve("links.json")).readList(Link[].class).isEmpty());
    }

    @Test
    void writtenLinksCanBeReadBack() {
        JsonFileStore store = store(dir.resolve("links.json"));
        List<Link> links = List.of(link("aaa111"), link("bbb222"));

        store.write(links);

        assertEquals(links, store.readList(Link[].class));
    }

    @Test
    void writeCreatesMissingDirectories() {
        Path file = dir.resolve("data/nested/links.json");

        store(file).write(List.of(link("aaa111")));

        assertTrue(Files.exists(file));
    }

    @Test
    void writeReplacesOldContentAndLeavesNoTempFile() {
        Path file = dir.resolve("links.json");
        JsonFileStore store = store(file);
        store.write(List.of(link("aaa111"), link("bbb222")));

        store.write(List.of(link("ccc333")));

        assertEquals(1, store.readList(Link[].class).size());
        assertFalse(Files.exists(dir.resolve("links.json.tmp")));
    }

    @Test
    void corruptFileIsReportedAndLeftUntouched() throws IOException {
        Path file = dir.resolve("links.json");
        Files.writeString(file, "{это не json");

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> store(file).readList(Link[].class));

        assertTrue(error.getMessage().contains("links.json"));
        assertEquals("{это не json", Files.readString(file));
    }

    @Test
    void invalidLinkInFileIsTreatedAsCorruption() throws IOException {
        Path file = dir.resolve("links.json");
        Files.writeString(file, """
            [{"code":"abc123","originalUrl":"https://example.com","ownerId":"%s",
              "createdAt":"2026-11-08T10:00:00Z","expiresAt":"2026-11-08T11:00:00Z",
              "clickLimit":0,"clickCount":0}]
            """.formatted(UUID.randomUUID()));

        assertThrows(IllegalStateException.class, () -> store(file).readList(Link[].class));
    }
}
