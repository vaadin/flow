/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
