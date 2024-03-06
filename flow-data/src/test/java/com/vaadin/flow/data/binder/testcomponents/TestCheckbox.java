/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.Tag;

@Tag("test-checkbox")
public class TestCheckbox
        extends AbstractTestHasValueAndValidation<TestCheckbox, Boolean> {

    private String label = null;

    public TestCheckbox() {
        super(null, Boolean::parseBoolean, String::valueOf);
    }

    public TestCheckbox(String label) {
        this();
        setLabel(label);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
