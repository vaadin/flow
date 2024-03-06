/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.html.OrderedList.NumberingType;
import org.junit.Test;

public class OrderedListTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("type", NumberingType.class, NumberingType.NUMBER,
                NumberingType.LOWERCASE_ROMAN, false, true);
    }

    @Test
    @Override
    public void testHasAriaLabelIsNotImplemented() {
        // Don't use aria-label or aria-labelledby on any other non-interactive
        // content such as p, legend, li, or ul, because it is ignored.
        // Source: https://www.w3.org/TR/using-aria/#label-support
        super.testHasAriaLabelIsNotImplemented();
    }
}
