package io.github.cunninglittlevixen.urlshortener.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface LinkRepository {

    boolean add(Link link);

    Optional<Link> update(String code, UnaryOperator<Link> updater);

    Optional<Link> findByCode(String code);

    List<Link> findByOwner(UUID ownerId);

    List<Link> findAll();

    boolean delete(String code);
}
