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
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class InvisibleDefaultPropertyValueIT extends ChromeBrowserTest {

    @Ignore("https://github.com/vaadin/flow/issues/7356 "
            + "Worked due to a side effect that was removed in 3.0 due to not all synchronized "
            + "properties being updated for all sync-events. Also related (but not same): "
            + "https://github.com/vaadin/flow/issues/3556")
    @Test
    public void clientDefaultPropertyValues_invisibleElement_propertiesAreNotSent() {
        open();

        // template is initially invisible
        TestBenchElement template = $("default-property").first();
        Assert.assertEquals(Boolean.TRUE.toString(),
                template.getAttribute("hidden"));

        // The element is not bound -> not value for "text" property
        WebElement text = template.$(TestBenchElement.class).id("text");
        Assert.assertEquals("", text.getText());

        // "message" property has default cleint side value
        WebElement message = template.$(TestBenchElement.class).id("message");
        Assert.assertEquals("msg", message.getText());

        // Show email value which has default property defined on the client
        // side
        WebElement showEmail = $(TestBenchElement.class).id("show-email");
        showEmail.click();

        WebElement emailValue = $(TestBenchElement.class).id("email-value");
        // default property is not sent to the server side
        Assert.assertEquals("", emailValue.getText());

        // make the element visible
        WebElement button = $(TestBenchElement.class).id("set-visible");
        button.click();

        // properties that has server side values are updated
        Assert.assertEquals("foo", text.getText());

        WebElement name = template.$(TestBenchElement.class).id("name");
        Assert.assertEquals("bar", name.getText());

        Assert.assertEquals("updated-message", message.getText());

        WebElement email = template.$(TestBenchElement.class).id("email");
        Assert.assertEquals("foo@example.com", email.getText());

        // Now check the email value on the server side
        showEmail = $(TestBenchElement.class).id("show-email");
        showEmail.click();
        Assert.assertEquals("foo@example.com", emailValue.getText());
    }
}
