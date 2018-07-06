/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.HasStyle;

/**
 * Registry for all exclusions in the generated files. Excluded properties,
 * events or methods are not generated.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ExclusionRegistry {

    private static final Map<String, Set<String>> PROPERTY_EXCLUSION_REGISTRY = new HashMap<>();
    private static final Map<String, Set<String>> EVENT_EXCLUSION_REGISTRY = new HashMap<>();
    private static final Map<String, Set<String>> METHOD_EXCLUSION_REGISTRY = new HashMap<>();
    private static final Map<String, Set<String>> BEHAVIOR_EXCLUSION_REGISTRY = new HashMap<>();
    private static final Map<String, Set<String>> INTERFACE_EXCLUSION_REGISTRY = new HashMap<>();

    static {
        excludeProperty("vaadin-combo-box", "value");
        excludeProperty("vaadin-radio-group", "value");
        excludeProperty("vaadin-combo-box", "itemLabelPath");
        excludeProperty("vaadin-combo-box", "itemValuePath");
        excludeProperty("vaadin-tabs", "selected");
        excludeProperty("vaadin-tabs", "orientation");
        excludeProperty("vaadin-tabs", "items");
        excludeProperty("vaadin-text-field", "hasValue");
        excludeProperty("vaadin-text-area", "hasValue");
        excludeProperty("vaadin-dialog", "noCloseOnEsc");
        excludeProperty("vaadin-dialog", "noCloseOnOutsideClick");
        excludeProperty("vaadin-list-box", "selected");
        excludeProperty("vaadin-list-box", "items");
        excludeEvent("vaadin-combo-box", "change");
        excludeEvent("vaadin-combo-box", "valued-changed");
        excludeBehaviorOrMixin("vaadin-date-picker",
                "Polymer.GestureEventListeners");
        excludeInterface("vaadin-dialog", HasStyle.class);
        excludeInterface("vaadin-notification", HasStyle.class);

        // Polymer lifecycle callbacks
        excludeMethod(null, "connectedCallback");
        excludeMethod(null, "disconnectedCallback");
        excludeMethod(null, "attributeChangedCallback");
    }

    private ExclusionRegistry() {
    }

    private static void put(String elementTag, String name,
            Map<String, Set<String>> map) {
        Objects.requireNonNull(name, "elementTag cannot be null.");

        map.computeIfAbsent(elementTag, element -> new HashSet<>()).add(name);
    }

    /**
     * Excludes a property from being generated for a specific element denoted
     * by its tag.
     *
     * @param elementTag
     *            the tag of the element which the property should be excluded
     *            from generation. Setting <code>null</code> makes the exclusion
     *            apply to all elements
     * @param propertyName
     *            the name of the property to be excluded
     *
     */
    public static void excludeProperty(String elementTag, String propertyName) {
        put(elementTag, propertyName, PROPERTY_EXCLUSION_REGISTRY);
    }

    /**
     * Excludes an event from being generated for a specific element denoted by
     * its tag.
     *
     * @param elementTag
     *            the tag of the element which the event should be excluded from
     *            generation. Setting <code>null</code> makes the exclusion
     *            apply to all elements
     *
     * @param eventName
     *            the name of the event to be excluded
     */
    public static void excludeEvent(String elementTag, String eventName) {
        put(elementTag, eventName, EVENT_EXCLUSION_REGISTRY);
    }

    /**
     * Excludes a method from being generated for a specific element denoted by
     * its tag.
     *
     * @param elementTag
     *            the tag of the element which the method should be excluded
     *            from generation. Setting <code>null</code> makes the exclusion
     *            apply to all elements
     * @param methodName
     *            the name of the method to be excluded
     */
    public static void excludeMethod(String elementTag, String methodName) {
        put(elementTag, methodName, METHOD_EXCLUSION_REGISTRY);
    }

    /**
     * Excludes a behavior or mixin from being evaluated for a specific element
     * denoted by its tag.
     * 
     * @param elementTag
     *            the tag of the element which the behavior should be excluded
     *            from generation. Setting <code>null</code> makes the exclusion
     *            apply to all elements
     * @param behaviorName
     *            the name of the behavior or mixin to be excluded
     */
    public static void excludeBehaviorOrMixin(String elementTag,
            String behaviorName) {
        put(elementTag, behaviorName, BEHAVIOR_EXCLUSION_REGISTRY);
    }

    /**
     * Excludes an interface from being evaluated for a specific element denoted
     * by its tag.
     * 
     * @param elementTag
     *            the tag of the element which the interface should be excluded
     *            from generation. Setting <code>null</code> makes the exclusion
     *            apply to all elements
     * @param interfaceClass
     *            the class of the Interface to be excluded
     */
    public static void excludeInterface(String elementTag,
            Class<?> interfaceClass) {
        put(elementTag, interfaceClass.getName(), INTERFACE_EXCLUSION_REGISTRY);
    }

    private static boolean isExcluded(String elementTag, String name,
            Map<String, Set<String>> map) {
        if (map.getOrDefault(null, Collections.emptySet()).contains(name)) {
            return true;
        }
        return map.getOrDefault(elementTag, Collections.emptySet())
                .contains(name);
    }

    /**
     * Gets whether a property should be excluded or not from the generation.
     *
     * @param elementTag
     *            the tag of the element
     * @param propertyName
     *            the name of the property
     * @return <code>true</code> if the property should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isPropertyExcluded(String elementTag,
            String propertyName) {
        return isExcluded(elementTag, propertyName,
                PROPERTY_EXCLUSION_REGISTRY);
    }

    /**
     * Gets whether an event should be excluded or not from the generation.
     *
     * @param elementTag
     *            the tag of the element
     * @param eventName
     *            the name of the event
     * @return <code>true</code> if the event should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isEventExcluded(String elementTag, String eventName) {
        return isExcluded(elementTag, eventName, EVENT_EXCLUSION_REGISTRY);
    }

    /**
     * Gets whether a method should be excluded or not from the generation.
     *
     * @param elementTag
     *            the tag of the element
     * @param methodName
     *            the name of the method
     * @return <code>true</code> if the method should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isMethodExcluded(String elementTag,
            String methodName) {
        return isExcluded(elementTag, methodName, METHOD_EXCLUSION_REGISTRY);
    }

    /**
     * Gets whether a behavior or mixin should be excluded or not from the
     * generation.
     * 
     * @param elementTag
     *            the tag of the element
     * @param behaviorName
     *            the name of the behavior or mixin
     * @return <code>true</code> if the behavior should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isBehaviorOrMixinExcluded(String elementTag,
            String behaviorName) {
        return isExcluded(elementTag, behaviorName,
                BEHAVIOR_EXCLUSION_REGISTRY);
    }

    /**
     * Gets whether an interface should be excluded or not from the generation.
     * 
     * @param elementTag
     *            the tag of the element
     * @param interfaceClass
     *            the class of the interface
     * @return <code>true</code> if the interface should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isInterfaceExcluded(String elementTag,
            Class<?> interfaceClass) {
        return isExcluded(elementTag, interfaceClass.getName(),
                INTERFACE_EXCLUSION_REGISTRY);
    }
}
