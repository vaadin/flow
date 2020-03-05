package com.vaadin.flow.uitest.ui.push;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.testbench.TestBenchElement;

@Category(PushTests.class)
public class ManualLongPollingPushIT extends AbstractLogTest {

    @Test
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