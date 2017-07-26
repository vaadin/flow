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

import com.vaadin.components.data.HasValue;
import com.vaadin.generated.vaadin.checkbox.GeneratedVaadinCheckbox;
import com.vaadin.shared.Registration;

/**
 * Server-side component for the {@code vaadin-checkbox} element.
 * 
 * @author Vaadin Ltd
 */
public class VaadinCheckbox extends GeneratedVaadinCheckbox<VaadinCheckbox>
        implements HasValue<VaadinCheckbox, Boolean> {

    @Override
    public Registration addValueChangeListener(
            ValueChangeListener<VaadinCheckbox, Boolean> listener) {
        return get().getElement().addPropertyChangeListener("checked",
                event -> listener.onComponentEvent(new ValueChangeEvent<>(get(),
                        this, (Boolean) event.getOldValue(),
                        event.isUserOriginated())));
    }

    /**
     * Default constructor.
     */
    public VaadinCheckbox() {
        super();
    }

    /**
     * Constructs a VaadinCheckbox with the initial label text.
     * 
     * @param labelText
     *            the label text to set
     */
    public VaadinCheckbox(String labelText) {
        setLabelText(labelText);
    }

    /**
     * Get the current label text.
     * 
     * @return the current label text
     */
    public String getLabelText() {
        return getElement().getText();
    }

    /**
     * Set the current label text of this checkbox.
     * 
     * @param labelText
     *            the label text to set
     * @return this instance, for method chaining
     */
    public VaadinCheckbox setLabelText(String labelText) {
        getElement().setText(labelText);
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
    public VaadinCheckbox setAriaLabel(String ariaLabel) {
        getElement().setAttribute("aria-label", ariaLabel);
        return get();
    }

    /**
     * Set whether this checkbox should be checked. Given a null value as an
     * argument the checkbox state will be set to be indeterminate.
     * 
     * @see {@link #isIndeterminate()}
     * @param value
     *            the value to set
     * @return this instance, for method chaining
     */
    @Override
    public VaadinCheckbox setValue(Boolean value) {
        if (value == null) {
            setIndeterminate(true);
            return get();
        }
        setIndeterminate(false);
        setChecked(value);
        return get();
    }

    /**
     * Get the current checked state of this checkbox. A null return value
     * implies that the checked state is indeterminate.
     * 
     * @see {@link #isIndeterminate()}
     * @return the checked state of this checkbox
     */
    @Override
    public Boolean getValue() {
        if (isEmpty()) {
            return null;
        }
        return isChecked();
    }

    /**
     * Returns whether the value of this checkbox is indeterminate.
     * 
     * @see {@link #isIndeterminate()}
     * @return whether the checkbox' value is indeterminate
     */
    @Override
    public boolean isEmpty() {
        return isIndeterminate();
    }
}
