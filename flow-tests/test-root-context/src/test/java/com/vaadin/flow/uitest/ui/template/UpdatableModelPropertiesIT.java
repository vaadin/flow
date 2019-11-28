/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class UpdatableModelPropertiesIT extends ChromeBrowserTest {

    @Test
    public void updateName_propertyIsSentToServer() {
        open();

        WebElement name = getElement("name");
        name.click();

        assertUpdate("foo");
    }

    @Test
    public void updateAge_propertyIsNotSentToServerIfIsNotSynced_propertyIsSentWhenSynced() {
        open();

        WebElement age = getElement("age");
        age.click();

        String value = age.getText();

        assertNoUpdate(value);

        getElement("syncAge").click();

        age.click();

        value = age.getText();
        assertUpdate(value);
    }

    @Test
    public void updateEmail_propertyIsSentToServer() {
        open();

        WebElement email = getElement("email");
        email.click();

        assertUpdate(email.getText());
    }

    @Test
    public void updateText_propertyIsNotSentToServer() {
        open();

        WebElement text = getElement("text");
        text.click();

        String value = text.getText();

        assertNoUpdate(value);
    }

    private WebElement getElement(String id) {
        TestBenchElement template = $(TestBenchElement.class).id("template");
        return template.$(TestBenchElement.class).id(id);
    }

    private void waitUpdate() {
        waitUntil(driver -> getElement("updateStatus").getText()
                .startsWith("Update Done"));
    }

    private void assertUpdate(String expectedValue) {
        waitUpdate();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        WebElement value = template.$(TestBenchElement.class)
                .id("property-value");
        Assert.assertEquals(expectedValue, value.getText());
    }

    private void assertNoUpdate(String unexpectedValue) {
        waitUpdate();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        WebElement value = template.$(TestBenchElement.class)
                .id("property-value");
        Assert.assertNotEquals(unexpectedValue, value.getText());
    }

}
