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
package com.vaadin.generator.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExclusionRegistry {

    private static final Map<String, Set<String>> PROPERTY_EXCLUSION_REGISTRY = new HashMap<>();
    private static final Map<String, Set<String>> EVENT_EXCLUSION_REGISTRY = new HashMap<>();
    private static final Map<String, Set<String>> METHOD_EXCLUSION_REGISTRY = new HashMap<>();

    static {
        excludeProperty("vaadin-combo-box", "selected-item");
        excludeEvent("vaadin-combo-box", "selected-item-changed");
        excludeEvent("vaadin-combo-box", "change");

        // Polymer lifecycle callbacks
        excludeMethod(null, "connectedCallback");
        excludeMethod(null, "disconnectedCallback");
        excludeMethod(null, "attributeChangedCallback");
    }

    private ExclusionRegistry() {
    }

    private static void put(String elementIdentifier, String name,
            Map<String, Set<String>> map) {
        Objects.requireNonNull(elementIdentifier,
                "elementIdentifier cannot be null.");
        Objects.requireNonNull(name, "propertyNameToMap cannot be null.");

        Set<String> list = map.computeIfAbsent(elementIdentifier,
                element -> new HashSet<>());
        list.add(name);
    }

    public static void excludeProperty(String elementIdentifier,
            String propertyName) {
        put(elementIdentifier, propertyName, PROPERTY_EXCLUSION_REGISTRY);
    }

    public static void excludeEvent(String elementIdentifier,
            String eventName) {
        put(elementIdentifier, eventName, EVENT_EXCLUSION_REGISTRY);
    }

    public static void excludeMethod(String elementIdentifier,
            String methodName) {
        put(elementIdentifier, methodName, METHOD_EXCLUSION_REGISTRY);
    }

    private static boolean isExcluded(String elementIdentifier, String name,
            Map<String, Set<String>> map) {
        return map
                .getOrDefault(elementIdentifier,
                        map.getOrDefault(null, Collections.emptySet()))
                .contains(name);
    }

    public static boolean isPropertyExcluded(String elementIdentifier,
            String propertyName) {
        return isExcluded(elementIdentifier, propertyName,
                PROPERTY_EXCLUSION_REGISTRY);
    }

    public static boolean isEventExcluded(String elementIdentifier,
            String eventName) {
        return isExcluded(elementIdentifier, eventName,
                EVENT_EXCLUSION_REGISTRY);
    }

    public static boolean isMethodExcluded(String elementIdentifier,
            String eventName) {
        return isExcluded(elementIdentifier, eventName,
                METHOD_EXCLUSION_REGISTRY);
    }
}
