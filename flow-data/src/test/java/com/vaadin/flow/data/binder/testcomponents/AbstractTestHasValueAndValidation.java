/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
