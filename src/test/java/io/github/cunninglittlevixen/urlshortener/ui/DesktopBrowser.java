package io.github.cunninglittlevixen.urlshortener.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public final class DesktopBrowser implements Browser {

    @Override
    public boolean open(String url) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(new URI(url));
            return true;
        } catch (IOException | URISyntaxException
                 | SecurityException | UnsupportedOperationException e) {
            return false;
        }
    }
}
