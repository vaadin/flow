package com.vaadin.flow.uitest.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractDebounceSynchronizeIT extends ChromeBrowserTest {

    private static final int MAX_THROTTLE_ATTEMPTS = 5;

    protected void assertThrottle(WebElement input)
            throws InterruptedException {
        runThrottleTest(input, 0);
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

    private boolean runThrottleTest(WebElement input, int attempt)
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
        input.sendKeys("c");
        if (attempt == MAX_THROTTLE_ATTEMPTS) {
            assertMessages("a", "ab");
        } else if (!checkMessages("a", "ab")) {
            // run test one more time
            runThrottleTest(input, attempt + 1);
        }

        Thread.sleep(700);

        // T + 2200, third update registered
        assertMessages("a", "ab", "abc");
        return true;
    }

    private boolean checkMessages(String... expectedMessages) {
        ArrayList<String> messages = findElements(By.cssSelector("#messages p"))
                .stream().map(WebElement::getText)
                .map(text -> text.replaceFirst("Value: ", ""))
                .collect(Collectors.toCollection(ArrayList::new));
        return messages
                .equals(new ArrayList<String>(Arrays.asList(expectedMessages)));
    }

}
