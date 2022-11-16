package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.testbench.BrowserTest;

@Tag(TestTag.PUSH_TESTS)
public class TrackMessageSizeIT extends AbstractLogTest {
    @BrowserTest
    public void runTests() {
        open();

        Assertions.assertEquals("1. All tests run", getLastLog().getText());
    }
}
