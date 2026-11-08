package io.github.cunninglittlevixen.urlshortener.core;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlValidator {

    private static final int MAX_LENGTH = 2048;

    public String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ServiceException("Введите URL, например: https://example.com/page");
        }
        String url = raw.trim();
        if (url.length() > MAX_LENGTH) {
            throw new ServiceException("URL слишком длинный: допустимо не более " + MAX_LENGTH + " символов");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new ServiceException("Некорректный URL. Пример правильного адреса: https://example.com/page");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ServiceException("Допустимы только ссылки, начинающиеся с http:// или https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ServiceException("В URL не указан адрес сайта. Пример: https://example.com");
        }
        return url;
    }
}
