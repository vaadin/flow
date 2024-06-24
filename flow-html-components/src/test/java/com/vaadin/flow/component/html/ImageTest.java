/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class ImageTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addOptionalStringProperty("alt");
        addStringProperty("src", "");
    }

    @Test
    public void setEmptyAltInConstructor_altPropertExists() {
        Image img = new Image("test.png", "");
        Assert.assertTrue(
                "'alt' property should have been retained with constructor",
                img.getElement().hasProperty("alt"));

        img.setAlt("");

        Assert.assertTrue("'alt' property should have been cleared with setAlt",
                img.getElement().hasProperty("alt"));
    }
}
