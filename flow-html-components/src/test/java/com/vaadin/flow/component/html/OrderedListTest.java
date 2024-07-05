/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.html.OrderedList.NumberingType;

public class OrderedListTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("type", NumberingType.class, NumberingType.NUMBER,
                NumberingType.LOWERCASE_ROMAN, false, true);
    }
}
