package com.vaadin.flow.uitest.ui.push;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Tag(TestTag.PUSH_TESTS)
public abstract class SendMultibyteCharactersTest
        extends AbstractBrowserConsoleTest {

    @BrowserTest
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

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));

        Assertions.assertTrue(
                messages.stream().anyMatch(msg -> msg.startsWith("Received ")
                        && msg.contains("message:")));
    }

}
