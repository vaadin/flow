package com.vaadin.flow.data.binder.testcomponents;

import java.time.LocalDate;

import com.vaadin.flow.component.Tag;

@Tag("test-date-picker")
public class TestDatePicker
        extends AbstractTestHasValueAndValidation<TestDatePicker, LocalDate> {

    private String label = null;

    public TestDatePicker() {
        super(null, LocalDate::parse,
                value -> value != null ? value.toString() : null);
    }

    public TestDatePicker(String label) {
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
