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

package com.vaadin.ui;

import java.util.Objects;

import com.vaadin.generated.vaadin.text.field.GeneratedVaadinPasswordField;

/**
 * Server-side component for the {@code vaadin-password-field} element.
 *
 * @author Vaadin Ltd.
 */
public class PasswordField extends GeneratedVaadinPasswordField<PasswordField>
        implements HasSize, HasValidation {

    /**
     * Constructs an empty {@code PasswordField}.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     */
    public PasswordField() {
        getElement().synchronizeProperty("hasValue", "value-changed");
        clear();
    }

    /**
     * Constructs an empty {@code PasswordField} with the given label.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
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
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
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

    @Override
    public String getEmptyValue() {
        return "";
    }

    @Override
    public void setValue(String value) {
        if (!Objects.equals(value, getValue())) {
            super.setValue(value);
        }
    }
}
