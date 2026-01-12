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
import java.util.Objects;

import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.impl.BasicElementStyle;
import com.vaadin.flow.internal.StateNode;

/**
 * Map for element style values.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementStylePropertyMap extends AbstractPropertyMap {

    /**
     * Creates a new element style map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ElementStylePropertyMap(StateNode node) {
        super(node);
    }

    @Override
    public void setProperty(String name, Serializable value,
            boolean emitChange) {
        assert value instanceof String;
        assert ElementUtil.isValidStylePropertyValue((String) value);
        super.setProperty(name, value, emitChange);
    }

    @Override
    public void setPropertyFromSignal(String name, Object value) {
        Serializable currentRaw = super.get(name);
        Serializable currentEffective;
        if (currentRaw instanceof SignalBinding binding) {
            currentEffective = binding.value();
        } else {
            currentEffective = currentRaw;
        }

        // Compare against the effectively stored value (unwrapped), but
        // when null is incoming from signal, we will emit an empty string
        // to the client to force style removal while preserving the binding.
        Object newEffective = (value == null) ? "" : value;
        if (Objects.equals(currentEffective, newEffective)) {
            return;
        }

        if (value == null) {
            // Emit empty string so that a client removes the style property,
            // but keep the SignalBinding on the server side.
            super.setProperty(name, "", true);
        } else {
            // Delegate to validated setter for non-null values
            setProperty(name, (Serializable) value, true);
        }
    }

    @Override
    protected Serializable get(String key) {
        Serializable value = super.get(key);
        if (value instanceof SignalBinding binding) {
            Serializable signalValue = binding.value();
            if (signalValue instanceof String stringValue
                    && stringValue.isEmpty()) {
                // Treat empty string from signal as removed style
                return null;
            }
            return signalValue;
        }
        return value;
    }

    @Override
    public void removeAllProperties() {
        // Dispose of any effect registrations and forget bindings
        for (String key : getPropertyNames().toList()) {
            Serializable raw = super.get(key);
            if (raw instanceof SignalBinding binding) {
                if (binding.registration() != null) {
                    binding.registration().remove();
                }
            }
        }
        super.removeAllProperties();
    }

    @Override
    public boolean hasProperty(String name) {
        Serializable raw = super.get(name);
        if (raw instanceof SignalBinding binding) {
            Serializable signalValue = binding.value();
            if (signalValue instanceof String stringValue
                    && stringValue.isEmpty()) {
                return false;
            }
            return signalValue != null;
        }
        return super.hasProperty(name);
    }

    /**
     * Returns a style instance for managing element inline styles.
     *
     * @return a Style instance connected to this map
     */
    public Style getStyle() {
        return new BasicElementStyle(this);
    }

}
