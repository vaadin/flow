package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Tag(TestTag.PUSH_TESTS)
public class EnableDisablePushIT extends ChromeBrowserTest {
    @BrowserTest
    public void testEnablePushWhenUsingPolling() throws Exception {
        open();

        Assertions.assertEquals("1. Push enabled", getLogRow(0));

        $(TestBenchElement.class).id("disable-push").click();
        Assertions.assertEquals("3. Push disabled", getLogRow(2));

        $(TestBenchElement.class).id("enable-polling").click();
        Assertions.assertEquals("5. Poll enabled", getLogRow(4));

        $(TestBenchElement.class).id("enable-push").click();
        Assertions.assertEquals("7. Push enabled", getLogRow(6));

        $(TestBenchElement.class).id("disable-polling").click();
        Assertions.assertEquals("9. Poll disabled", getLogRow(8));

        $(TestBenchElement.class).id("thread-re-enable-push").click();
        Thread.sleep(2500);
        Assertions.assertEquals("16. Polling disabled, push enabled",
                getLogRow(15));

        $(TestBenchElement.class).id("disable-push").click();
        Assertions.assertEquals("18. Push disabled", getLogRow(17));
    }

    private String getLogRow(int index) {
        return findElements(By.className("log")).get(index).getText();
    }

}
