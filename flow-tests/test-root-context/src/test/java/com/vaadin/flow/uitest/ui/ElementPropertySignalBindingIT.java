/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ElementPropertySignalBindingIT extends ChromeBrowserTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void checkInitialPropertyValue_modifyPropertyValue_checkModifiedValue() {
        WebElement resultElement = findElement(
                By.id(ElementPropertySignalBindingView.RESULT_DIV_ID));
        WebElement signalValueElement = findElement(
                By.id(ElementPropertySignalBindingView.SIGNAL_VALUE_DIV_ID));
        WebElement listenerCountElement = findElement(
                By.id(ElementPropertySignalBindingView.LISTENER_COUNT_DIV_ID));

        Assert.assertEquals(ElementPropertySignalBindingView.TEST_PROPERTY_NAME
                + " changed to: foo", resultElement.getText());
        Assert.assertEquals("Signal value: foo", signalValueElement.getText());
        Assert.assertEquals(String.valueOf(1), listenerCountElement.getText());

        $(DivElement.class).id(ElementPropertySignalBindingView.TARGET_DIV_ID)
                .setProperty(
                        ElementPropertySignalBindingView.TEST_PROPERTY_NAME,
                        "changed-value");
        $(DivElement.class).id(ElementPropertySignalBindingView.TARGET_DIV_ID)
                .dispatchEvent("change");

        Assert.assertEquals(
                ElementPropertySignalBindingView.TEST_PROPERTY_NAME
                        + " changed to: changed-value",
                resultElement.getText());
        Assert.assertEquals("Signal value: changed-value",
                signalValueElement.getText());
        Assert.assertEquals(String.valueOf(2), listenerCountElement.getText());
    }

    @Test
    public void computedSignalBound_modifyPropertyValue_shouldThrow() {
        $(DivElement.class)
                .id(ElementPropertySignalBindingView.SHOULD_THROW_TARGET_DIV_ID)
                .setProperty(
                        ElementPropertySignalBindingView.TEST_PROPERTY_NAME,
                        "changed-value");
        $(DivElement.class)
                .id(ElementPropertySignalBindingView.SHOULD_THROW_TARGET_DIV_ID)
                .dispatchEvent("change");

        Assert.assertTrue("Internal error expected when attempting to "
                + "update a property bound to a (read-only) computed signal, "
                + "but no internal error is present.",
                isInternalErrorNotificationPresent());
    }

    private boolean isInternalErrorNotificationPresent() {
        if (!isElementPresent(By.className("v-system-error"))) {
            return false;
        }
        return findElement(By.className("v-system-error"))
                .getAttribute("innerHTML").contains("Internal error");
    }
}
