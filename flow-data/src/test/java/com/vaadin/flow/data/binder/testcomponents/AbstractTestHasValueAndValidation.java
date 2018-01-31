package com.vaadin.flow.data.binder.testcomponents;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;

public abstract class AbstractTestHasValueAndValidation<C extends Component, T>
        extends Component implements HasValue<C, T>, HasValidation {

    private T value;
    private String errorMessage = "";
    private boolean invalid;

    @Override
    public void setErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            errorMessage = "";
        }
        this.errorMessage = errorMessage;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    @Override
    public boolean isInvalid() {
        return invalid;
    }

    @Override
    public void setValue(T value) {
        if (Objects.equals(getValue(), value))
            return;
        getElement().setProperty(getClientValuePropertyName(),
                value == null ? null : toString(value));

    }

    @Override
    public T getValue() {
        String v = getElement().getProperty(getClientValuePropertyName(), null);
        return v == null ? null : fromString(v);
    }

    protected abstract T fromString(String string);

    protected abstract String toString(T t);
}
