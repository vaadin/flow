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
