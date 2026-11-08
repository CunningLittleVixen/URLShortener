package io.github.cunninglittlevixen.urlshortener.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {

    boolean add(Link link);

    void save(Link link);

    Optional<Link> findByCode(String code);

    List<Link> findByOwner(UUID ownerId);

    List<Link> findAll();

    boolean delete(String code);
}
