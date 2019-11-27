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

public class InvisibleDefaultPropertyValueIT extends ChromeBrowserTest {

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
