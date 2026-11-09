package io.github.cunninglittlevixen.urlshortener.infra;

import io.github.cunninglittlevixen.urlshortener.core.Link;
import io.github.cunninglittlevixen.urlshortener.core.LinkRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class JsonLinkRepository implements LinkRepository {

    private final LinkRepository delegate;
    private final JsonFileStore store;

    public JsonLinkRepository(LinkRepository delegate, JsonFileStore store) {
        this.delegate = delegate;
        this.store = store;
        for (Link link : store.readList(Link[].class)) {
            delegate.add(link);
        }
    }

    @Override
    public synchronized boolean add(Link link) {
        boolean added = delegate.add(link);
        if (added) {
            save();
        }
        return added;
    }

    @Override
    public synchronized Optional<Link> update(String code, UnaryOperator<Link> updater) {
        Optional<Link> updated = delegate.update(code, updater);
        if (updated.isPresent()) {
            save();
        }
        return updated;
    }

    @Override
    public Optional<Link> findByCode(String code) {
        return delegate.findByCode(code);
    }

    @Override
    public List<Link> findByOwner(UUID ownerId) {
        return delegate.findByOwner(ownerId);
    }

    @Override
    public List<Link> findAll() {
        return delegate.findAll();
    }

    @Override
    public synchronized boolean delete(String code) {
        boolean deleted = delegate.delete(code);
        if (deleted) {
            save();
        }
        return deleted;
    }

    private void save() {
        store.write(delegate.findAll());
    }
}
