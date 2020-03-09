package com.vaadin.flow.uitest.ui.push;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.testbench.TestBenchElement;

@Category(PushTests.class)
public abstract class SendMultibyteCharactersTest
        extends AbstractBrowserConsoleTest {

    @Test
    public void transportSupportsMultibyteCharacters() {
        open();

        TestBenchElement textArea = $(TestBenchElement.class).id("text");

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            text.append("之は日本語です、テストです。");
        }

        /*
         * clean up logs up to the current state
         */
        getBrowserLogs(true);

        textArea.sendKeys(text.toString());
        textArea.sendKeys(Keys.TAB);

        findElement(By.id("label")).click();
        List<String> messages = new ArrayList<>();
        waitUntil(driver -> getBrowserLogs(true).stream()
                .filter(String.class::isInstance).anyMatch(msg -> {
                    messages.add(msg.toString());
                    return msg.equals("Handling message from server");
                }), 15);

        checkLogsForErrors();

        Assert.assertTrue(
                messages.stream().anyMatch(msg -> msg.startsWith("Received ")
                        && msg.contains("message:")));
    }

}