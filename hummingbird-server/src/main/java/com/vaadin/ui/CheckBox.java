/*
 * Copyright 2000-2014 Vaadin Ltd.
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

import com.vaadin.annotations.Tag;
import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusAndBlurServerRpcImpl;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.Element.EventRegistrationHandle;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.checkbox.CheckBoxServerRpc;

@Tag("span")
public class CheckBox extends AbstractField<Boolean>
        implements FocusNotifier, BlurNotifier {

    private Element inputElement;
    private Element labelElement;

    private CheckBoxServerRpc rpc = new CheckBoxServerRpc() {

        @Override
        public void setChecked(boolean checked,
                MouseEventDetails mouseEventDetails) {

        }
    };

    FocusAndBlurServerRpcImpl focusBlurRpc = new FocusAndBlurServerRpcImpl(
            this) {
        @Override
        protected void fireEvent(Event event) {
            CheckBox.this.fireEvent(event);
        }
    };

    /**
     * Creates a new checkbox.
     */
    public CheckBox() {
        inputElement = new Element("input");
        inputElement.setAttribute("type", "checkbox");

        labelElement = new Element("label");

        getElement().appendChild(inputElement);
        getElement().appendChild(labelElement);

        registerRpc(rpc);
        registerRpc(focusBlurRpc);
        setValue(Boolean.FALSE);
    }

    /**
     * Creates a new checkbox with a set caption.
     *
     * @param text
     *            the Checkbox caption.
     */
    public CheckBox(String text) {
        this();
        setText(text);
    }

    /**
     * Creates a new checkbox with a caption and a set initial state.
     *
     * @param text
     *            the caption of the checkbox
     * @param initialState
     *            the initial state of the checkbox
     */
    public CheckBox(String text, boolean initialState) {
        this(text);
        setValue(initialState);
    }

    /**
     * Creates a new checkbox that is connected to a boolean property.
     *
     * @param state
     *            the Initial state of the switch-button.
     * @param dataSource
     */
    public CheckBox(String text, Property<?> dataSource) {
        this(text);
        setPropertyDataSource(dataSource);
    }

    public void setText(String text) {
        labelElement.setTextContent(text);
    }

    public String getText() {
        return labelElement.getTextContent();
    }

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public void attach() {
        super.attach();

        if (inputElement.getAttribute("id") == null) {
            // We need an id to be able to map label to input
            inputElement.setAttribute("id", IdGenerator.generateId(getUI()));
        }

        labelElement.setAttribute("for", inputElement.getAttribute("id"));

    }

    @Override
    protected void setInternalValue(Boolean newValue) {
        super.setInternalValue(newValue);
        if (newValue == null) {
            newValue = false;
        }
        inputElement.setAttribute("checked", newValue);
    }

    @Override
    public void clear() {
        setValue(Boolean.FALSE);
    }

    @Override
    public boolean isEmpty() {
        return getValue() == null || getValue().equals(Boolean.FALSE);
    }

    private EventRegistrationHandle valueChangeEventRegistration = null;

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
        super.addValueChangeListener(listener);
        if (valueChangeEventRegistration == null) {
            inputElement.addEventData("change", "element.checked");
            valueChangeEventRegistration = getElement()
                    .addEventListener("change", e -> {
                        setValue(e.get("element.checked").asBoolean());
                    });
        }
    }

    @Override
    public void removeValueChangeListener(
            com.vaadin.data.Property.ValueChangeListener listener) {
        super.removeValueChangeListener(listener);
        if (!hasListeners(ValueChangeEvent.class)) {
            assert valueChangeEventRegistration != null;
            valueChangeEventRegistration.remove();
        }
    }

}
