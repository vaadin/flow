/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

public class ParamTest extends ComponentTest {

    @Override
    protected void addProperties() {
        addStringProperty("name", "");
        addOptionalStringProperty("value");
    }
}
