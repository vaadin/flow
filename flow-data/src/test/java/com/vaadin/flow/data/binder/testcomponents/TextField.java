package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.Tag;

@Tag("test-text-field")
public class TextField
        extends AbstractHasValueAndValidation<TextField, String> {

    public TextField() {
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
