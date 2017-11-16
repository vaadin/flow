/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

/**
 * Normal tests with @Before are not implemented because each @Test starts new
 * Chrome process.
 */
public class ListInsideListBindingIT extends ChromeBrowserTest {

    @Test
    public void listDataBinding() {
        int initialSize = 4;
        open();

        WebElement template = findElement(By.id("template"));

        checkMessagesRemoval(template, initialSize);
        getInShadowRoot(template, By.id("reset")).click();
        checkAllElementsUpdated(template, initialSize);
    }

    private void checkMessagesRemoval(WebElement template, int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            List<WebElement> currentMessages = findInShadowRoot(template,
                    By.className("submsg"));
            Assert.assertEquals("Wrong amount of nested messages",
                    initialSize - i, currentMessages.size());

            WebElement messageToRemove = currentMessages.iterator().next();
            String messageToRemoveText = messageToRemove.getText();
            messageToRemove.click();

            String removedMessageLabelText = getInShadowRoot(template,
                    By.id("removedMessage")).getText();
            Assert.assertEquals("Expected removed message text to appear",
                    "Removed message: " + messageToRemoveText,
                    removedMessageLabelText);
        }
    }

    private void checkAllElementsUpdated(WebElement template, int initialSize) {
        getInShadowRoot(template, By.id("updateAllElements")).click();
        List<WebElement> msgs = findInShadowRoot(template,
                By.className("submsg"));
        Assert.assertEquals("Wrong amount of nested messages", initialSize,
                msgs.size());
        msgs.forEach(msg -> Assert.assertEquals("Message was not updated",
                ListInsideListBindingView.UPDATED_TEXT, msg.getText()));
    }
}
