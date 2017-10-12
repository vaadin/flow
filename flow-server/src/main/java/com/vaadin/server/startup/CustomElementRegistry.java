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
package com.vaadin.server.startup;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.AbstractTextElementStateProvider;
import com.vaadin.ui.Component;

/**
 * Registry holder for custom elements found on servlet initalization.
 */
public class CustomElementRegistry {

    private final Map<String, Class<? extends Component>> customElements = new HashMap<>();

    private static final CustomElementRegistry INSTANCE = new CustomElementRegistry();

    boolean initialized;

    private CustomElementRegistry() {
    }

    /**
     * Get instance of CustomElementRegistry.
     *
     * @return singleton instance of the registry
     */
    public static CustomElementRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Returns whether this registry has been initialized with custom elements
     * info.
     *
     * @return whether this registry has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Set registered custom elements.
     * <p>
     * Note! Custom elements can only be set once!
     *
     * @param customElements
     *            map of registered custom elements
     */
    public void setCustomElements(
            Map<String, Class<? extends Component>> customElements) {
        if (initialized) {
            throw new IllegalArgumentException(
                    "Custom element map has already been initialized");
        }
        this.customElements.clear();
        this.customElements.putAll(customElements);
        initialized = true;
    }

    /**
     * Check if a custom element for given tag is registered.
     *
     * @param tag
     *            tag to check
     * @return true if custom element class is found
     */
    public boolean isRegisteredCustomElement(String tag) {
        return customElements.containsKey(tag);
    }

    /**
     * Get the registered custom element for given tag.
     *
     * @param tag
     *            tag to get custom element class for
     * @return custom element class for tag
     */
    public Class<? extends Component> getRegisteredCustomElement(String tag) {
        return customElements.get(tag);
    }

    /**
     * Create a new component instance for given element without component.
     * <p>
     * Creation and linking requires that a custom element has been registered
     * for the given element tag name.
     *
     * @param element
     *            element to check and wrap
     */
    public void wrapElementIfNeeded(Element element) {
        // cancel wrap if AbstractTextElement as it doesn't support getTag()
        if (element
                .getStateProvider() instanceof AbstractTextElementStateProvider) {
            return;
        }

        String tag = element.getTag();
        if (isRegisteredCustomElement(tag)
                && !element.getComponent().isPresent()) {
            Component.from(element, getRegisteredCustomElement(tag));
        }
    }
}
