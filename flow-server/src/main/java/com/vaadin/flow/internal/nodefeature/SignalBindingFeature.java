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
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.Signal;

/**
 * Node feature for binding {@link Signal}s to various properties of a node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class SignalBindingFeature extends ServerSideFeature {

    public static final String ENABLED = "enabled";

    private Map<String, SignalBinding> values;

    private record SignalBinding(Signal<?> signal,
            Registration registration) implements Serializable {
    }

    /**
     * Creates a SignalBindingFeature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public SignalBindingFeature(StateNode node) {
        super(node);
    }

    /**
     * Sets a binding for the given key.
     *
     * @param key
     *            the key
     * @param registration
     *            the registration
     * @param signal
     *            the signal
     */
    public void setBinding(String key, Registration registration,
            Signal<?> signal) {
        ensureValues();
        values.put(key, new SignalBinding(signal, registration));
    }

    /**
     * Checks whether there is a binding for the given key.
     * 
     * @param key
     *            the key
     * @return true if there is a binding for the given key, false otherwise
     */
    public boolean hasBinding(String key) {
        if (values == null) {
            return false;
        }
        SignalBinding binding = values.get(key);
        return binding != null && binding.signal != null
                && binding.registration != null;
    }

    /**
     * Removes the binding for the given key.
     * 
     * @param key
     *            the key
     */
    public void removeBinding(String key) {
        if (values == null) {
            return;
        }
        SignalBinding binding = values.get(key);
        if (binding != null && binding.registration != null) {
            binding.registration.remove();
        }
        values.remove(key);
    }

    private void ensureValues() {
        if (values == null) {
            values = new HashMap<>();
        }
    }
}
