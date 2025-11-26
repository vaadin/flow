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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

/**
 * Abstract class to be used as a parent for node maps which supports setting
 * properties in a map.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractPropertyMap extends NodeMap {

    /**
     * Creates a new element property map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public AbstractPropertyMap(StateNode node) {
        super(node);
    }

    /**
     * Sets a property to the given value.
     *
     * @param name
     *            the property name
     * @param value
     *            the value, must be a string, a boolean, a double or
     *            <code>null</code>
     * @param emitChange
     *            true to create a change event for the client side
     */
    public void setProperty(String name, Serializable value,
            boolean emitChange) {
        assert name != null;
        assert isValidValueType(value);

        if (hasSignal(name)) {
            SignalBinding b = (SignalBinding) super.get(name);
            put(name, new SignalBinding(b.signal(), b.registration(), value),
                    emitChange);
        } else {
            put(name, value, emitChange);
        }
    }

    /**
     * Checks whether there is a property of the given name.
     *
     * @param name
     *            the name of the property
     * @return <code>true</code> if there is a property with the given name;
     *         <code>false</code> if there is no property
     */
    public boolean hasProperty(String name) {
        return contains(name);
    }

    /**
     * Removes the given property.
     *
     * @param name
     *            the name of the property to remove
     */
    public void removeProperty(String name) {
        super.remove(name);
    }

    /**
     * Removes all properties.
     *
     */
    public void removeAllProperties() {
        super.clear();
    }

    /**
     * Gets the value of the given property.
     *
     * @param name
     *            the name of the property
     * @return the property value; <code>null</code> if there is no property or
     *         if the value is explicitly set to null
     */
    public Serializable getProperty(String name) {
        return get(name);
    }

    /**
     * Gets the property names.
     *
     * @return a stream containing all the property names that have been set
     */
    public Stream<String> getPropertyNames() {
        return keySet().stream();
    }

    /**
     * Checks if the given value is of a supported type.
     *
     * @param value
     *            the value to check, may be null
     * @return <code>true</code> if the type is supported, <code>false</code>
     *         otherwise
     */
    public static boolean isValidValueType(Serializable value) {
        if (value == null) {
            return true;
        }
        Class<?> type = ReflectTools.convertPrimitiveType(value.getClass());
        return JacksonCodec.canEncodeWithoutTypeInfo(type)
                || StateNode.class.isAssignableFrom(type);
    }

    public boolean hasSignal(String key) {
        return super.get(key) instanceof SignalBinding binding
                && binding.signal() != null && binding.registration() != null;
    }

    @Override
    public void updateFromClient(String key, Serializable value) {
        if (hasSignal(key)) {
            SignalBinding b = (SignalBinding) super.get(key);
            super.updateFromClient(key,
                    new SignalBinding(b.signal(), b.registration(), value));
        } else {
            super.updateFromClient(key, value);
        }
    }

    /**
     * Binds the given signal to the given property. <code>null</code> signal
     * unbinds existing binding.
     *
     * @param owner
     *            the element owning the property, not <code>null</code>
     * @param name
     *            the name of the property
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding for the
     *             given property
     */
    public void bindSignal(Element owner, String name, Signal<?> signal) {
        SignalBinding previousSignalBinding;
        if (super.get(name) instanceof SignalBinding binding) {
            previousSignalBinding = binding;
        } else {
            previousSignalBinding = null;
        }
        if (signal != null && hasSignal(name)) {
            throw new BindingActiveException();
        }
        Registration registration = signal != null
                ? ElementEffect.bind(owner, signal,
                        (element, value) -> setPropertyFromSignal(name, value))
                : null;
        if (signal == null && previousSignalBinding != null) {
            if (previousSignalBinding.registration() != null) {
                previousSignalBinding.registration().remove();
            }
            // revert to plain stored value (may be null)
            put(name, get(name), false);
        } else {
            put(name, new SignalBinding(signal, registration, get(name)),
                    false);
        }
    }

    /**
     * Applies a value coming from a signal to the property while preserving an
     * existing binding.
     *
     * @param name
     *            the property name
     * @param value
     *            the value to apply; <code>null</code> removes the property on
     *            the client but keeps the binding and last-applied value as
     *            <code>null</code>
     */
    protected abstract void setPropertyFromSignal(String name, Object value);
}
