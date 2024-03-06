/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class ImageTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
    }

    @Test
    @Override
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    public void emptyAltKeepsAttribute() {
        Image img = new Image("test.png", "");
        Assert.assertEquals("", img.getAlt().get());
        Assert.assertTrue(img.getElement().hasAttribute("alt"));
        img.setAlt(null);
        Assert.assertEquals(Optional.empty(), img.getAlt());
        Assert.assertFalse(img.getElement().hasAttribute("alt"));
    }
}
