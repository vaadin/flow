/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry that facilitates remapping of element property names to different
 * names for the generated components' java API.
 *
 * @author Vaadin ltd
 * @since 1.0
 */
public final class PropertyNameRemapRegistry {

    private static final Map<String, Map<String, String>> REGISTRY = new HashMap<>();
    static {
        put("vaadin-date-picker", "min", "minAsString");
        put("vaadin-date-picker", "max", "maxAsString");
    }

    private PropertyNameRemapRegistry() {
    }

    private static void put(String elementIdentifier, String propertyNameToMap,
            String mappedValue) {
        Objects.requireNonNull(elementIdentifier,
                "elementIdentifier cannot be null.");
        Objects.requireNonNull(propertyNameToMap,
                "propertyNameToMap cannot be null.");
        Objects.requireNonNull(mappedValue, "mappedValue cannot be null.");
        Map<String, String> map = REGISTRY.getOrDefault(elementIdentifier,
                new HashMap<>());
        map.put(propertyNameToMap, mappedValue);
        REGISTRY.put(elementIdentifier, map);
    }

    /**
     * Look for a property name remapping for the given element in this
     * registry. Returns an empty optional if no mapping exists.
     *
     * @param elementIdentifier
     *            the tag name for the element to look up mappings for
     * @param propertyName
     *            the property name to look for
     * @return an optional of the mapping, empty if no mapping exists
     */
    public static Optional<String> getOptionalMappingFor(
            String elementIdentifier, String propertyName) {
        if (!REGISTRY.containsKey(elementIdentifier)) {
            return Optional.empty();
        }
        return Optional
                .ofNullable(REGISTRY.get(elementIdentifier).get(propertyName));
    }
}
