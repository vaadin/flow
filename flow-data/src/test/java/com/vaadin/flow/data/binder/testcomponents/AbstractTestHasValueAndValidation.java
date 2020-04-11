package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.data.binder.HasValidation;
import com.vaadin.flow.function.SerializableFunction;

public abstract class AbstractTestHasValueAndValidation<C extends AbstractSinglePropertyField<C, T>, T>
        extends AbstractSinglePropertyField<C, T> implements HasValidation<T> {

    public AbstractTestHasValueAndValidation(T defaultValue,
            SerializableFunction<String, T> propertyToValue,
            SerializableFunction<T, String> valueToProperty) {
        super("value", defaultValue, String.class, propertyToValue,
                valueToProperty);
    }

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
}
