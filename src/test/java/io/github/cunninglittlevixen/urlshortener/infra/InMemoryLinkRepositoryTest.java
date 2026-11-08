package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cunninglittlevixen.urlshortener.core.Link;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryLinkRepositoryTest {

    private static final Instant CREATED = Instant.parse("2026-11-08T10:00:00Z");

    private final InMemoryLinkRepository repository = new InMemoryLinkRepository();

    private Link link(String code, UUID owner) {
        return new Link(code, "https://example.com", owner,
            CREATED, CREATED.plusSeconds(3600), 5, 0);
    }

    @Test
    void addStoresNewLinkAndRejectsDuplicateCode() {
        Link first = link("abc123", UUID.randomUUID());
        Link second = link("abc123", UUID.randomUUID());

        assertTrue(repository.add(first));
        assertFalse(repository.add(second));
        assertEquals(first, repository.findByCode("abc123").orElseThrow());
    }

    @Test
    void findByCodeReturnsEmptyForUnknownCode() {
        assertTrue(repository.findByCode("missing").isEmpty());
    }

    @Test
    void findByOwnerReturnsOnlyOwnLinks() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        repository.add(link("a1", alice));
        repository.add(link("a2", alice));
        repository.add(link("b1", bob));

        assertEquals(2, repository.findByOwner(alice).size());
        assertEquals(1, repository.findByOwner(bob).size());
    }

    @Test
    void saveReplacesExistingLink() {
        Link original = link("abc123", UUID.randomUUID());
        repository.add(original);

        repository.save(original.withClickRegistered());

        assertEquals(1, repository.findByCode("abc123").orElseThrow().clickCount());
    }

    @Test
    void deleteRemovesLinkOnlyOnce() {
        repository.add(link("abc123", UUID.randomUUID()));

        assertTrue(repository.delete("abc123"));
        assertFalse(repository.delete("abc123"));
        assertTrue(repository.findByCode("abc123").isEmpty());
    }

    @Test
    void findAllReturnsEveryLink() {
        repository.add(link("a1", UUID.randomUUID()));
        repository.add(link("b1", UUID.randomUUID()));

        assertEquals(2, repository.findAll().size());
    }
}
