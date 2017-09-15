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
    private static final String PATTERN_PROPERTY_NAME = "pattern";
    private static final String REQUIRED_PROPERTY_NAME = "required";

    /**
     * Constructs an empty {@code PasswordField}.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     */
    public PasswordField() {
        addAttachListener(event -> {
            Page page = event.getUI().getPage();
            page.executeJavaScript(
                    "$0.flowCheckValidityOld = $0.checkValidity;",
                    getElement());
            disableValidatorIfNotNeeded(page);
            getElement().addPropertyChangeListener(PATTERN_PROPERTY_NAME,
                    event -> disableValidatorIfNotNeeded(page));
            getElement().addPropertyChangeListener(REQUIRED_PROPERTY_NAME,
                    event -> disableValidatorIfNotNeeded(page));
        });

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

    // A stub that should be removed after this ticket is implemented:
    // https://github.com/vaadin/vaadin-text-field/issues/130
    private void disableValidatorIfNotNeeded(Page page) {
        String patternProperty = getElement()
                .getProperty(PATTERN_PROPERTY_NAME);
        boolean isPatternPropertyEmpty = patternProperty == null
                || patternProperty.isEmpty();
        if (isPatternPropertyEmpty && !Boolean.parseBoolean(
                getElement().getProperty(REQUIRED_PROPERTY_NAME))) {
            page.executeJavaScript("$0.checkValidity = function() {};",
                    getElement());
        } else {
            page.executeJavaScript(
                    "$0.checkValidity = $0.flowCheckValidityOld;",
                    getElement());
        }
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
