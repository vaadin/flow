package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Tag(TestTag.PUSH_TESTS)
public class PushToggleComponentVisibilityIT extends ChromeBrowserTest {

    private static final String HIDE = "hide";

    @BrowserTest
    public void ensureComponentVisible() {
        open();

        $(TestBenchElement.class).id(HIDE).click();
        Assertions.assertEquals("Please wait",
                $(TestBenchElement.class).id("label").getText());

        waitUntil(driver -> isElementPresent(By.id(HIDE)));
        $(TestBenchElement.class).id(HIDE).click();
        Assertions.assertEquals("Please wait",
                $(TestBenchElement.class).id("label").getText());
    }
}
