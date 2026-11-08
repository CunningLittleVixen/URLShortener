package io.github.cunninglittlevixen.urlshortener.core;

import java.util.List;

public record LoginResult(List<Link> links, List<Notification> notifications) {

    public boolean isKnown() {
        return !links.isEmpty() || !notifications.isEmpty();
    }
}
