package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Tag;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Tag(TestTag.PUSH_TESTS)
public class ManualLongPollingPushIT extends AbstractLogTest {

    @BrowserTest
    public void doubleManualPushDoesNotFreezeApplication() {
        open();
        $(TestBenchElement.class).id("double-manual-push").click();
        waitUntil(textToBePresentInElement(() -> getLastLog(),
                "2. Second message logged after 1s, followed by manual push"));
        $(TestBenchElement.class).id("manaul-push").click();
        waitUntil(textToBePresentInElement(() -> getLastLog(),
                "3. Logged after 1s, followed by manual push"));
    }

}
