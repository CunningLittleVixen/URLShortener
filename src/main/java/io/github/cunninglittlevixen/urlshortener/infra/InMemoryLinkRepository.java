package io.github.cunninglittlevixen.urlshortener.infra;

import io.github.cunninglittlevixen.urlshortener.core.Link;
import io.github.cunninglittlevixen.urlshortener.core.LinkRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryLinkRepository implements LinkRepository {

    private final ConcurrentMap<String, Link> links = new ConcurrentHashMap<>();

    @Override
    public boolean add(Link link) {
        return links.putIfAbsent(link.code(), link) == null;
    }

    @Override
    public void save(Link link) {
        links.put(link.code(), link);
    }

    @Override
    public Optional<Link> findByCode(String code) {
        return Optional.ofNullable(links.get(code));
    }

    @Override
    public List<Link> findByOwner(UUID ownerId) {
        return links.values().stream()
            .filter(link -> link.ownerId().equals(ownerId))
            .toList();
    }

    @Override
    public List<Link> findAll() {
        return List.copyOf(links.values());
    }

    @Override
    public boolean delete(String code) {
        return links.remove(code) != null;
    }
}
