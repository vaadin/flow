package com.vaadin.flow.webcomponent;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class EmbeddingChromeBrowserTest extends ChromeBrowserTest {

    @Override
    protected void open() {
        super.open();

        // Wait for at least one shadow root to appear. This is to avoid race
        // conditions where the test starts before the shadow root has been
        // attached #8329
        this.waitUntil((driver) -> Boolean.TRUE
                .equals(this.getCommandExecutor().executeScript(
                        "return Array.from(document.getElementsByTagName('*')).some(x => x.shadowRoot !== null)")));
    }

}
