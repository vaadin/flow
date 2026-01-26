/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.util.Optional;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.signals.Signal;

/**
 * Component representing a <code>&lt;param&gt;</code> element for
 * <code>&lt;param&gt;</code> element.
 *
 * @see HtmlObject
 *
 * @author Vaadin Ltd
 *
 */
@Tag(Tag.PARAM)
public class Param extends HtmlComponent {

    private static final PropertyDescriptor<String, String> nameDescriptor = PropertyDescriptors
            .attributeWithDefault("name", "");

    private static final PropertyDescriptor<String, Optional<String>> valueDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("value", "");

    /**
     * Creates a new <code>&lt;param&gt;</code> component.
     */
    public Param() {
    }

    /**
     * Creates a new <code>&lt;param&gt;</code> component with given name and
     * value.
     *
     * @see #setName(String)
     * @see #setValue(String)
     *
     * @param name
     *            a name attribute value
     * @param value
     *            a value attribute value
     */
    public Param(String name, String value) {
        setName(name);
        setValue(value);
    }

    /**
     * Sets a "value" attribute.
     *
     * @param value
     *            "value" attribute value
     */
    public void setValue(String value) {
        set(valueDescriptor, value);
    }

    /**
     * Binds a signal's value to the "value" attribute so that the attribute is
     * updated when the signal's value is updated.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the "value" attribute. When unbinding, the current attribute value is
     * left unchanged.
     * <p>
     * While a binding for the "value" attribute is active, any attempt to set
     * the attribute manually throws
     * {@link com.vaadin.signals.BindingActiveException}. The same happens when
     * trying to bind a new Signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param valueSignal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setValue(String)
     * @see Element#bindAttribute(String, Signal)
     *
     * @since 25.1
     */
    public void bindValue(Signal<String> valueSignal) {
        getElement().bindAttribute("value", valueSignal);
    }

    /**
     * Sets a "name" attribute value.
     *
     * @param name
     *            a "name" attribute value
     */
    public void setName(String name) {
        set(nameDescriptor, name);
    }

    /**
     * Binds a signal's value to the "name" attribute so that the attribute is
     * updated when the signal's value is updated.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the "name" attribute. When unbinding, the current attribute value is
     * left unchanged.
     * <p>
     * While a binding for the "name" attribute is active, any attempt to set
     * the attribute manually throws
     * {@link com.vaadin.signals.BindingActiveException}. The same happens when
     * trying to bind a new Signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param nameSignal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setName(String)
     * @see Element#bindAttribute(String, Signal)
     *
     * @since 25.1
     */
    public void bindName(Signal<String> nameSignal) {
        getElement().bindAttribute("name", nameSignal);
    }

    /**
     * Gets the "name" attribute value.
     *
     * @see #setName(String)
     *
     * @return the "name" attribute value
     */
    public String getName() {
        return get(nameDescriptor);
    }

    /**
     * Gets the "value" attribute.
     *
     * @see #setValue(String)
     *
     * @return the "value" attribute value
     */
    public Optional<String> getValue() {
        return get(valueDescriptor);
    }

}
