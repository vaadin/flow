/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class NativeLabelTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addOptionalStringProperty("for");
    }

    @Test
    public void setForComponent() {
        NativeLabel otherComponent = new NativeLabel();
        otherComponent.setId("otherC");
        NativeLabel l = (NativeLabel) getComponent();
        l.setFor(otherComponent);
        Assert.assertEquals(otherComponent.getId().get(), l.getFor().get());
    }

}
