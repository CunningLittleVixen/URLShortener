package io.github.cunninglittlevixen.urlshortener.core;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class LinkService {

    private static final int MAX_CODE_ATTEMPTS = 10;
    private static final String NOT_FOUND_MESSAGE = "Ссылка не найдена или уже удалена";

    private final LinkRepository repository;
    private final NotificationRepository notificationRepository;
    private final CodeGenerator codeGenerator;
    private final UrlValidator urlValidator;
    private final AppConfig config;
    private final Clock clock;

    public LinkService(LinkRepository repository, NotificationRepository notificationRepository, CodeGenerator codeGenerator,
                       UrlValidator urlValidator, AppConfig config, Clock clock) {
        this.repository = repository;
        this.notificationRepository = notificationRepository;
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

    public String open(String input) {
        String code = extractCode(input);
        Instant now = clock.instant();
        Link opened = repository.update(code, current -> registerClick(current, now))
            .orElseThrow(() -> new ServiceException(NOT_FOUND_MESSAGE));
        if (opened.isLimitReached()) {
            notifyOwner(opened, NotificationType.LIMIT_REACHED, now);
        }
        return opened.originalUrl();
    }

    private Link registerClick(Link link, Instant now) {
        if (link.isExpired(now)) {
            throw new ServiceException("Срок действия ссылки истёк. Создайте новую ссылку командой create.");
        }
        if (link.isLimitReached()) {
            throw new ServiceException("Лимит переходов по ссылке исчерпан. Создайте новую ссылку командой create.");
        }
        return link.withClickRegistered();
    }

    public List<Link> removeExpired() {
        Instant now = clock.instant();
        List<Link> removed = new ArrayList<>();
        for (Link link : repository.findAll()) {
            if (link.isExpired(now) && repository.delete(link.code())) {
                removed.add(link);
                notifyOwner(link, NotificationType.EXPIRED, now);
            }
        }
        return removed;
    }

    public Link editLimit(UUID ownerId, String input, int newLimit) {
        Objects.requireNonNull(ownerId, "ownerId");
        String code = extractCode(input);
        Instant now = clock.instant();
        return repository.update(code, current -> applyLimit(current, ownerId, newLimit, now))
            .orElseThrow(() -> new ServiceException(NOT_FOUND_MESSAGE));
    }

    public void deleteLink(UUID ownerId, String input) {
        Objects.requireNonNull(ownerId, "ownerId");
        String code = extractCode(input);
        Link link = repository.findByCode(code)
            .orElseThrow(() -> new ServiceException(NOT_FOUND_MESSAGE));
        requireOwner(link, ownerId);
        if (!repository.delete(code)) {
            throw new ServiceException(NOT_FOUND_MESSAGE);
        }
    }

    public List<Link> listLinks(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return repository.findByOwner(ownerId).stream()
            .sorted(Comparator.comparing(Link::createdAt))
            .toList();
    }

    public int minEditableLimit(Link link) {
        return Math.max(config.minLimit(), link.clickCount() + 1);
    }

    public int maxEditableLimit() {
        return config.maxLimit();
    }

    private Link applyLimit(Link link, UUID ownerId, int newLimit, Instant now) {
        requireOwner(link, ownerId);
        if (link.isExpired(now)) {
            throw new ServiceException("Срок действия ссылки истёк, изменить лимит нельзя.");
        }
        if (link.isLimitReached()) {
            throw new ServiceException("Лимит переходов исчерпан, изменить его нельзя. "
                + "Создайте новую ссылку командой create.");
        }
        int min = minEditableLimit(link);
        int max = maxEditableLimit();
        if (newLimit < min || newLimit > max) {
            throw new ServiceException("Допустимый лимит для этой ссылки: от " + min + " до " + max);
        }
        return link.withClickLimit(newLimit);
    }

    private void requireOwner(Link link, UUID ownerId) {
        if (!link.ownerId().equals(ownerId)) {
            throw new ServiceException("Эта ссылка принадлежит другому пользователю. "
                + "Менять и удалять её может только владелец.");
        }
    }

    private void notifyOwner(Link link, NotificationType type, Instant now) {
        notificationRepository.add(
            new Notification(link.ownerId(), type, link.code(), link.originalUrl(), now));
    }

    private String extractCode(String input) {
        String text = input == null ? "" : input.strip();
        if (text.startsWith(config.prefix())) {
            text = text.substring(config.prefix().length());
        }
        if (text.isBlank()) {
            throw new ServiceException("Введите короткую ссылку, например: " + config.prefix() + "aB3xYz");
        }
        return text;
    }
}
