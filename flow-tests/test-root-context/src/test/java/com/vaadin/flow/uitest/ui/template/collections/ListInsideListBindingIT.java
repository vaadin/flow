/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Normal tests with @Before are not implemented because each @Test starts new
 * Chrome process.
 */
public class ListInsideListBindingIT extends ChromeBrowserTest {

    @Test
    public void listDataBinding() {
        int initialSize = 4;
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        checkMessagesRemoval(template, initialSize);
        template.$(TestBenchElement.class).id("reset").click();
        checkAllElementsUpdated(template, initialSize);
    }

    private void checkMessagesRemoval(TestBenchElement template,
            int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            List<TestBenchElement> currentMessages = template
                    .$(TestBenchElement.class).attribute("class", "submsg")
                    .all();
            Assert.assertEquals("Wrong amount of nested messages",
                    initialSize - i, currentMessages.size());

            WebElement messageToRemove = currentMessages.iterator().next();
            String messageToRemoveText = messageToRemove.getText();
            messageToRemove.click();

            String removedMessageLabelText = template.$(TestBenchElement.class)
                    .id("removedMessage").getText();
            Assert.assertEquals("Expected removed message text to appear",
                    "Removed message: " + messageToRemoveText,
                    removedMessageLabelText);
        }
    }

    private void checkAllElementsUpdated(TestBenchElement template,
            int initialSize) {
        template.$(TestBenchElement.class).id("updateAllElements").click();
        List<TestBenchElement> msgs = template.$(TestBenchElement.class)
                .attribute("class", "submsg").all();
        Assert.assertEquals("Wrong amount of nested messages", initialSize,
                msgs.size());
        msgs.forEach(msg -> Assert.assertEquals("Message was not updated",
                ListInsideListBindingView.UPDATED_TEXT, msg.getText()));
    }
}
