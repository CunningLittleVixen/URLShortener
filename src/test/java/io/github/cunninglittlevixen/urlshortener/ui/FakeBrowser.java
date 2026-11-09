package io.github.cunninglittlevixen.urlshortener.ui;

import java.util.ArrayList;
import java.util.List;

final class FakeBrowser implements Browser {

    boolean succeeds = true;
    private final List<String> opened = new ArrayList<>();

    @Override
    public boolean open(String url) {
        opened.add(url);
        return succeeds;
    }

    List<String> opened() {
        return opened;
    }
}
