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

import com.vaadin.generated.vaadin.text.field.GeneratedVaadinTextField;

/**
 * Server-side component for the {@code vaadin-text-field} element.
 * 
 * @author Vaadin Ltd
 */
public class VaadinTextField extends GeneratedVaadinTextField<VaadinTextField> {

    /**
     * Constructs an empty {@code VaadinTextField}.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     */
    public VaadinTextField() {
        getElement().synchronizeProperty("hasValue", "value-changed");
        clear();
    }

    /**
     * Constructs an empty {@code VaadinTextField} with the given label.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     * 
     * @param labelText
     *            the text to set as the label
     */
    public VaadinTextField(String labelText) {
        this();
        setLabel(labelText);
    }

    /**
     * Constructs an empty {@code VaadinTextField} with the given label and
     * placeholder text.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     * 
     * @param labelText
     *            the text to set as the label
     * @param placeholder
     *            the placeholder text to set
     */
    public VaadinTextField(String labelText, String placeholder) {
        this(labelText);
        setPlaceholder(placeholder);
    }

    @Override
    public String getEmptyValue() {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return !hasValue();
    }
}
