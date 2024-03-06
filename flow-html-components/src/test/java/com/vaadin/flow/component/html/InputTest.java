/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.beans.IntrospectionException;
import org.junit.Test;

public class InputTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    public void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        whitelistProperty("valueChangeMode");
        whitelistProperty("valueChangeTimeout");
        super.setup();
    }

    @Override
    protected void addProperties() {
        addStringProperty("type", "text");
        // Object.class because of generics
        addProperty("value", Object.class, "", "foo", false, false);
        addOptionalStringProperty("placeholder");
    }

    @Test
    @Override
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }
}
