package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.Tag;

@Tag("test-text-field")
public class TestTextField
        extends AbstractTestHasValueAndValidation<TestTextField, String> {

    public TestTextField() {
        setValue("");
    }

    @Override
    protected String fromString(String string) {
        return string;
    }

    @Override
    protected String toString(String t) {
        return t;
    }

    @Override
    public String getEmptyValue() {
        return "";
    }

}
