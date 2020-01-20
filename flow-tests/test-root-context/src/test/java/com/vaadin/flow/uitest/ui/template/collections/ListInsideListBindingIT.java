/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
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
