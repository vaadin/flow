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
package com.vaadin.ui.checkbox;

import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.common.HasValue;

import elemental.json.Json;

/**
 * Server-side component for the {@code vaadin-checkbox} element.
 *
 * @author Vaadin Ltd
 */
public class Checkbox extends GeneratedVaadinCheckbox<Checkbox>
        implements HasSize {

    /**
     * Default constructor.
     */
    public Checkbox() {
    }

    /**
     * Constructs a VaadinCheckbox with the initial label text.
     *
     * @see #setLabel(String)
     * @param labelText
     *            the label text to set
     */
    public Checkbox(String labelText) {
        setLabel(labelText);
    }

    /**
     * Constructs a VaadinCheckbox with the initial value.
     *
     * @see #setValue(Boolean)
     * @param initialValue
     *            the initial value
     */
    public Checkbox(boolean initialValue) {
        setValue(initialValue);
    }

    /**
     * Constructs a VaadinCheckbox with the initial value.
     *
     * @see #setLabel(String)
     * @see #setValue(Boolean)
     *
     * @param labelText
     *            the label text to set
     * @param initialValue
     *            the initial value
     */
    public Checkbox(String labelText, boolean initialValue) {
        this(labelText);
        setValue(initialValue);
    }

    /**
     * Constructs a VaadinCheckbox with the initial label text and value change
     * listener.
     *
     * @see #setLabel(String)
     * @see #addValueChangeListener(HasValue.ValueChangeListener)
     * @param label
     *            the label text to set
     * @param listener
     *            the value change listener to add
     */
    public Checkbox(String label,
            ValueChangeListener<Checkbox, Boolean> listener) {
        this(label);
        addValueChangeListener(listener);
    }

    /**
     * Get the current label text.
     *
     * @return the current label text
     */
    public String getLabel() {
        return getElement().getText();
    }

    /**
     * Set the current label text of this checkbox.
     *
     * @param label
     *            the label text to set
     * @return this instance, for method chaining
     */
    public Checkbox setLabel(String label) {
        getElement().setText(label);
        return get();
    }

    /**
     * Set the accessibility label of this checkbox.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/ARIA_Techniques/Using_the_aria-label_attribute"
     *      >aria-label at MDN</a>
     *
     * @param ariaLabel
     *            the accessibility label to set
     * @return this instance, for method chaining
     */
    public Checkbox setAriaLabel(String ariaLabel) {
        getElement().setAttribute("aria-label", ariaLabel);
        return get();
    }

    /**
     * Set whether this checkbox should be checked. Given a null value as an
     * argument the checkbox state will be set to be indeterminate.
     *
     * @see #isIndeterminate()
     * @param value
     *            the value to set
     */
    @Override
    public void setValue(Boolean value) {
        if (value == null) {
            setIndeterminate(true);
            getElement().setPropertyJson("checked", Json.createNull());
        } else {
            setIndeterminate(false);
            super.setValue(value);
        }
    }

    /**
     * Get the current checked state of this checkbox. A null return value
     * implies that the checked state is indeterminate.
     *
     * @see #isIndeterminate()
     * @return the checked state of this checkbox
     */
    @Override
    public Boolean getValue() {
        if (isEmpty()) {
            return null;
        }
        return super.getValue();
    }

    /**
     * Returns whether the value of this checkbox is indeterminate.
     *
     * @see #isIndeterminate()
     * @return whether the checkbox' value is indeterminate
     */
    @Override
    public boolean isEmpty() {
        return isIndeterminate();
    }
}
