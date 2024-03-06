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
import com.vaadin.flow.function.SerializableFunction;

@Tag("test-text-field")
public class TestTextField
        extends AbstractTestHasValueAndValidation<TestTextField, String> {

    public TestTextField() {
        super("", SerializableFunction.identity(),
                SerializableFunction.identity());
    }

}
