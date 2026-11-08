package io.github.cunninglittlevixen.urlshortener.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LinkService {

    private static final int MAX_CODE_ATTEMPTS = 10;

    private final LinkRepository repository;
    private final CodeGenerator codeGenerator;
    private final UrlValidator urlValidator;
    private final AppConfig config;
    private final Clock clock;

    public LinkService(LinkRepository repository, CodeGenerator codeGenerator,
                       UrlValidator urlValidator, AppConfig config, Clock clock) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.urlValidator = urlValidator;
        this.config = config;
        this.clock = clock;
    }

    public Link createLink(UUID ownerId, String rawUrl) {
        return createLink(ownerId, rawUrl, config.defaultLimit());
    }

    public Link createLink(UUID ownerId, String rawUrl, int clickLimit) {
        Objects.requireNonNull(ownerId, "ownerId");
        String url = urlValidator.validate(rawUrl);
        if (clickLimit < config.minLimit() || clickLimit > config.maxLimit()) {
            throw new ServiceException("Лимит переходов должен быть от "
                + config.minLimit() + " до " + config.maxLimit());
        }
        Instant now = clock.instant();
        Instant expiresAt = now.plus(config.linkTtl());
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            Link link = new Link(codeGenerator.generate(), url, ownerId, now, expiresAt, clickLimit, 0);
            if (repository.add(link)) {
                return link;
            }
        }
        throw new IllegalStateException("Не удалось подобрать уникальный код ссылки");
    }

    public String shortUrl(Link link) {
        return config.prefix() + link.code();
    }
}
