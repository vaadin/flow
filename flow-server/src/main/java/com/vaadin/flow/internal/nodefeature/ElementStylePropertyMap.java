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

import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.impl.BasicElementStyle;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.Signal;

/**
 * Map for element style values.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementStylePropertyMap extends AbstractPropertyMap {

    private Signal<String> signal;

    private Registration signalRegistration;

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

    /**
     * Returns a style instance for managing element inline styles.
     *
     * @return a Style instance connected to this map
     */
    public Style getStyle() {
        return new BasicElementStyle(this);
    }

    /**
     * Binds the given signal to this map. <code>null</code> signal unbinds
     * existing binding.
     *
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @param bindAction
     *            the action to perform the binding, may be <code>null</code>
     */
    public void bindSignal(Signal<String> signal,
            SerializableSupplier<Registration> bindAction) {
        var previousSignal = this.signal;
        if (signal != null && previousSignal != null) {
            throw new IllegalStateException("Binding is already active");
        }
        Registration registration = bindAction != null ? bindAction.get()
                : null;
        if (registration != null) {
            signalRegistration = registration;
        }
        if (signal == null && signalRegistration != null) {
            signalRegistration.remove();
            signalRegistration = null;
            this.signal = null;
        } else {
            this.signal = signal;
        }
    }

    public Signal<String> getSignal() {
        return signal;
    }
}
