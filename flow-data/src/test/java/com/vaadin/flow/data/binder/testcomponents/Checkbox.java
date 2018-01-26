package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Tag;

@Tag("test-checkbox")
public class Checkbox extends AbstractHasValueAndValidation<Checkbox, Boolean> {

    private String label = null;

    public Checkbox() {
    }

    public Checkbox(String label) {
        setLabel(label);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    protected Boolean fromString(String string) {
        return Boolean.parseBoolean(string);
    }

    @Override
    protected String toString(Boolean t) {
        return String.valueOf(t);
    }

}
