/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.ui.passwordfield;

import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.common.HasValidation;
import com.vaadin.ui.common.HasValue;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.textfield.GeneratedVaadinPasswordField;

/**
 * Server-side component for the {@code vaadin-password-field} element.
 *
 * @author Vaadin Ltd.
 */
public class PasswordField extends GeneratedVaadinPasswordField<PasswordField>
        implements HasSize, HasValidation, HasValue<PasswordField, String> {

    /**
     * Constructs an empty {@code PasswordField}.
     */
    public PasswordField() {
        getElement().synchronizeProperty("hasValue", "value-changed");
    }

    /**
     * Constructs an empty {@code PasswordField} with the given label.
     *
     * @param label
     *            the text to set as the label
     */
    public PasswordField(String label) {
        this();
        setLabel(label);
    }

    /**
     * Constructs an empty {@code PasswordField} with the given label and
     * placeholder text.
     *
     * @param label
     *            the text to set as the label
     * @param placeholder
     *            the placeholder text to set
     */
    public PasswordField(String label, String placeholder) {
        this(label);
        setPlaceholder(placeholder);
    }

    /**
     * Constructs an empty {@code PasswordField} with a value change listener.
     *
     * @param listener
     *            the value change listener
     *
     * @see #addValueChangeListener(com.vaadin.ui.common.HasValue.ValueChangeListener)
     */
    public PasswordField(ValueChangeListener<PasswordField, String> listener) {
        this();
        addValueChangeListener(listener);
    }

    /**
     * Constructs an empty {@code PasswordField} with a value change listener
     * and a label.
     *
     * @param label
     *            the text to set as the label
     * @param listener
     *            the value change listener
     *
     * @see #setLabel(String)
     * @see #addValueChangeListener(com.vaadin.ui.common.HasValue.ValueChangeListener)
     */
    public PasswordField(String label,
            ValueChangeListener<PasswordField, String> listener) {
        this(label);
        addValueChangeListener(listener);
    }

    /**
     * Constructs a {@code PasswordField} with a value change listener, a label
     * and an initial value.
     *
     * @param label
     *            the text to set as the label
     * @param initialValue
     *            the initial value
     * @param listener
     *            the value change listener
     *
     * @see #setLabel(String)
     * @see #setValue(String)
     * @see #addValueChangeListener(com.vaadin.ui.common.HasValue.ValueChangeListener)
     */
    public PasswordField(String label, String initialValue,
            ValueChangeListener<PasswordField, String> listener) {
        this(label);
        setValue(initialValue);
        addValueChangeListener(listener);
    }

    @Override
    public String getEmptyValue() {
        return "";
    }

    @Override
    @Synchronize("value-changed")
    public String getValue() {
        String value = super.getValue();
        return value == null ? getEmptyValue() : value;
    }
}
