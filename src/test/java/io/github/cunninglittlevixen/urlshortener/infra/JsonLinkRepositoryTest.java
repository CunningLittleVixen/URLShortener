package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.Link;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonLinkRepositoryTest {

    private static final Instant CREATED = Instant.parse("2026-11-08T10:00:00Z");

    @TempDir
    Path dir;

    private JsonLinkRepository open() {
        return new JsonLinkRepository(new InMemoryLinkRepository(),
            new JsonFileStore(JsonMappers.create(), dir.resolve("links.json")));
    }

    private Link link(String code) {
        return new Link(code, "https://example.com", UUID.randomUUID(),
            CREATED, CREATED.plusSeconds(3600), 1000, 0);
    }

    @Test
    void startsEmptyWithoutFile() {
        assertTrue(open().findAll().isEmpty());
    }

    @Test
    void addedLinkSurvivesRestart() {
        Link link = link("aaa111");
        open().add(link);

        assertEquals(link, open().findByCode("aaa111").orElseThrow());
    }

    @Test
    void updatedCounterSurvivesRestart() {
        JsonLinkRepository first = open();
        first.add(link("aaa111"));
        first.update("aaa111", Link::withClickRegistered);

        assertEquals(1, open().findByCode("aaa111").orElseThrow().clickCount());
    }

    @Test
    void failedUpdateIsNotSaved() {
        JsonLinkRepository first = open();
        first.add(link("aaa111"));

        assertThrows(IllegalStateException.class, () -> first.update("aaa111", current -> {
            throw new IllegalStateException("сбой");
        }));

        assertEquals(0, open().findByCode("aaa111").orElseThrow().clickCount());
    }

    @Test
    void deletedLinkDoesNotComeBack() {
        JsonLinkRepository first = open();
        first.add(link("aaa111"));
        first.delete("aaa111");

        assertTrue(open().findByCode("aaa111").isEmpty());
    }

    @Test
    void corruptFileStopsStartup() throws IOException {
        Files.writeString(dir.resolve("links.json"), "[[[");

        assertThrows(IllegalStateException.class, this::open);
    }

    @Test
    void parallelClicksAreAllSaved() {
        JsonLinkRepository first = open();
        first.add(link("aaa111"));

        IntStream.range(0, 100).parallel()
            .forEach(i -> first.update("aaa111", Link::withClickRegistered));

        assertEquals(100, open().findByCode("aaa111").orElseThrow().clickCount());
    }
}
