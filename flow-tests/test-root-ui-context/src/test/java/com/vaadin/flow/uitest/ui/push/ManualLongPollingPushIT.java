package com.vaadin.flow.uitest.ui.push;

import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(PushTests.class)
public class ManualLongPollingPushIT extends ChromeBrowserTest {

    @Test
    public void doubleManualPushDoesNotFreezeApplication() {
        open();
        $(TestBenchElement.class).id("double-manual-push").click();
        waitUntilLogText(
                "2. Second message logged after 1s, followed by manual push");
        $(TestBenchElement.class).id("manaul-push").click();
        waitUntilLogText("3. Logged after 1s, followed by manual push");
    }

    private void waitUntilLogText(final String expected) {
        waitUntil(new ExpectedCondition<Boolean>() {
            private String actual;

            @Override
            public Boolean apply(WebDriver arg0) {
                List<WebElement> logs = findElements(By.className("log"));
                actual = logs.get(logs.size() - 1).getText();
                return expected.equals(actual);
            }

            @Override
            public String toString() {
                return String.format("log text to become '%s' (was: '%s')",
                        expected, actual);
            }
        });
    }
}