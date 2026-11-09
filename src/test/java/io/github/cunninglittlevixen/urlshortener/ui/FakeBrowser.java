package io.github.cunninglittlevixen.urlshortener.ui;

import java.util.ArrayList;
import java.util.List;

public final class FakeBrowser implements Browser {

    public boolean succeeds = true;
    private final List<String> opened = new ArrayList<>();

    @Override
    public boolean open(String url) {
        opened.add(url);
        return succeeds;
    }

    public List<String> opened() {
        return opened;
    }
}
