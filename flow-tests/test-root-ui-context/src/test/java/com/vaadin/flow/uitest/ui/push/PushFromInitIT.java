package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Tag;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.testbench.BrowserTest;

@Tag(TestTag.PUSH_TESTS)
public class PushFromInitIT extends AbstractLogTest {
    @BrowserTest
    public void pushFromInit() {
        open();

        waitUntil(driver -> ("3. " + PushFromInitUI.LOG_AFTER_INIT)
                .equals(getLastLog().getText()));

    }
}
