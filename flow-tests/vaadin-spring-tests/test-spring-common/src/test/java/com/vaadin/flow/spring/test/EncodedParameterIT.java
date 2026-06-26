/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;

import static com.vaadin.flow.spring.test.EncodedParameter.DECODED_CONTENT;
import static com.vaadin.flow.spring.test.EncodedParameter.ENCODED_CONTENT;

public class EncodedParameterIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/param/a%2bb";
    }

    @Test
    public void encodedUrlParameter_isNotUnencodedBeforeSetParameter() {
        open();
        waitForElementPresent(By.id(ENCODED_CONTENT));
        WebElement element = $(DivElement.class).id(ENCODED_CONTENT);

        Assert.assertEquals(
                "Element parameter should be received with encoding.", "a%2bb",
                element.getText());

        element = $(DivElement.class).id(DECODED_CONTENT);

        Assert.assertEquals("Element parameter should decode to +.", "a+b",
                element.getText());
    }

}
