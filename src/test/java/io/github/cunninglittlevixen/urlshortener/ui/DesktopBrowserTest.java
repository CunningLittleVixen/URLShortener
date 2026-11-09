
package io.github.cunninglittlevixen.urlshortener.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class DesktopBrowserTest {

    @Test
    void malformedAddressIsNotOpened() {
        assertFalse(new DesktopBrowser().open("not a valid address with spaces"));
    }
}
