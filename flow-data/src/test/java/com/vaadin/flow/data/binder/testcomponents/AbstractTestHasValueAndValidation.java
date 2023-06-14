package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.function.SerializableFunction;

public abstract class AbstractTestHasValueAndValidation<C extends AbstractSinglePropertyField<C, T>, T>
        extends AbstractSinglePropertyField<C, T> implements HasValidation {

    public AbstractTestHasValueAndValidation(T defaultValue,
            SerializableFunction<String, T> propertyToValue,
            SerializableFunction<T, String> valueToProperty) {
        super("value", defaultValue, String.class, propertyToValue,
                valueToProperty);
    }

    private String errorMessage = "";
    private boolean invalid;
    private boolean internalValidationDisabled;

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
    public void setInternalValidationDisabled(boolean disabled) {
        this.internalValidationDisabled = disabled;
    }

    @Override
    public boolean isInternalValidationDisabled() {
        return this.internalValidationDisabled;
    }
}
