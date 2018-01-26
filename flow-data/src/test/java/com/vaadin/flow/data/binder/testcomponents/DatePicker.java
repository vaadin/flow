package com.vaadin.flow.data.binder.testcomponents;

import java.time.LocalDate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Tag;

@Tag("test-date-picker")
public class DatePicker
        extends AbstractHasValueAndValidation<DatePicker, LocalDate> {

    private String label = null;

    public DatePicker() {

    }

    public DatePicker(String label) {
        setLabel(label);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    protected LocalDate fromString(String string) {
        return LocalDate.parse(string);
    }

    @Override
    protected String toString(LocalDate t) {
        return t.toString();
    }

}
