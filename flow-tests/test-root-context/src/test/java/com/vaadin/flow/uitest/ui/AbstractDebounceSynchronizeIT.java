package com.vaadin.flow.uitest.ui;

import java.util.Arrays;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractDebounceSynchronizeIT extends ChromeBrowserTest {

    protected void assertThrottle(WebElement input)
            throws InterruptedException {
        input.sendKeys("a");
        assertMessages("a");

        Thread.sleep(700);
        input.sendKeys("b");

        // T + 700, only first update registered
        assertMessages("a");

        Thread.sleep(800);

        // T + 1500, second update registered
        assertMessages("a", "ab");
        Long before = getTime();
        input.sendKeys("c");
        Long after = getTime();
        if (after - before < 1000) {
            assertMessages("a", "ab");
        }

        Thread.sleep(700);

        // T + 2200, third update registered
        assertMessages("a", "ab", "abc");
    }

    protected void assertDebounce(WebElement input)
            throws InterruptedException {
        // Should not sync while typing within 1000ms from last time
        for (String keys : Arrays.asList("a", "b", "c")) {
            input.sendKeys(keys);
            Thread.sleep(500);
            assertMessages();
        }

        // Should sync after some additional inactivity
        Thread.sleep(700);
        assertMessages("abc");
    }

    protected void assertMessages(String... expectedMessages) {
        Assert.assertArrayEquals(expectedMessages,
                findElements(By.cssSelector("#messages p")).stream()
                        .map(WebElement::getText)
                        .map(text -> text.replaceFirst("Value: ", ""))
                        .toArray(String[]::new));
    }

    protected void assertEager(WebElement input) {
        input.sendKeys("a");
        assertMessages("a");

        input.sendKeys("b");
        assertMessages("a", "ab");
    }

    private Long getTime() {
        return (Long) executeScript("return new Date().getTime();");
    }
}
