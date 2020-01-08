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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.component.HasStyle;
import com.vaadin.generator.metadata.ComponentBasicType;

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
    private static final Set<String> TAG_EXCLUSION_REGISTRY = new HashSet<>();
    private static final Set<ComponentBasicType> PROPERTY_TYPE_EXCLUSION_REGISTRY = new HashSet<>();

    static {
        excludePropertyType(ComponentBasicType.FUNCTION);
        excludeProperty("vaadin-combo-box", "dataProvider");
        excludeProperty("vaadin-radio-group", "value");
        excludeProperty("vaadin-tabs", "selected");
        excludeProperty("vaadin-tabs", "orientation");
        excludeProperty("vaadin-tabs", "items");
        excludeProperty("vaadin-text-field", "hasValue");
        excludeProperty("vaadin-text-area", "hasValue");
        excludeProperty("vaadin-dialog", "noCloseOnEsc");
        excludeProperty("vaadin-dialog", "noCloseOnOutsideClick");
        excludeProperty("vaadin-list-box", "selected");
        excludeProperty("vaadin-list-box", "items");
        excludeProperty("vaadin-combo-box-overlay", "owner");
        excludeProperty("vaadin-combo-box-overlay", "model");
        excludeProperty("vaadin-context-menu-overlay", "owner");
        excludeProperty("vaadin-context-menu-overlay", "model");
        excludeProperty("vaadin-date-picker-overlay", "owner");
        excludeProperty("vaadin-date-picker-overlay", "model");
        excludeProperty("vaadin-dialog-overlay", "owner");
        excludeProperty("vaadin-dialog-overlay", "model");
        excludeEvent("vaadin-combo-box", "change");
        excludeEvent("vaadin-combo-box", "valued-changed");
        excludeEvent("vaadin-text-area", "iron-resize");
        excludeMethod("vaadin-text-field", "clear");
        excludeMethod("vaadin-text-area", "clear");
        excludeEvent("vaadin-rich-text-editor", "html-value-changed");
        excludeBehaviorOrMixin("vaadin-date-picker",
                "Polymer.GestureEventListeners");
        excludeInterface("vaadin-dialog", HasStyle.class);
        excludeInterface("vaadin-notification", HasStyle.class);

        // this is a workaround
        // current generator generates wrong file for this element
        // https://github.com/vaadin/flow/issues/4477
        // https://github.com/vaadin/flow/issues/4479
        excludeTag("vaadin-time-picker-text-field");
        // generates not-needed component that doesn't work due to invalid generics inheritance
        excludeTag("vaadin-select-text-field");
        // Polymer lifecycle callbacks
        excludeMethod(null, "connectedCallback");
        excludeMethod(null, "disconnectedCallback");
        excludeMethod(null, "attributeChangedCallback");
    }

    private ExclusionRegistry() {
    }

    /**
     * Excludes the element generation denoted by its tag.
     * 
     * @param elementTag
     *            the tag of the element, which is going to be skipped
     *            generation.
     */
    public static void excludeTag(String elementTag) {
        Objects.requireNonNull(elementTag, "elementTag cannot be null");
        if (!TAG_EXCLUSION_REGISTRY.contains(elementTag)) {
            TAG_EXCLUSION_REGISTRY.add(elementTag);
        }
    }

    /**
     * Excludes all the properties that have the given type from all the
     * components.
     * 
     * @param type
     *            the property type to exclude
     */
    public static void excludePropertyType(ComponentBasicType type) {
        Objects.requireNonNull(type, "type cannot be null");
        if (!PROPERTY_TYPE_EXCLUSION_REGISTRY.contains(type)) {
            PROPERTY_TYPE_EXCLUSION_REGISTRY.add(type);
        }
    }

    /**
     * Gets whether an Element should be excluded or not from the generation.
     * 
     * @param elementTag
     *            the tag of the element
     * @return <code>true</code> if the element should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isTagExcluded(String elementTag) {
        return TAG_EXCLUSION_REGISTRY.contains(elementTag);
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
     * @param type
     *            the type of the property
     * @return <code>true</code> if the property should be excluded,
     *         <code>false</code> otherwise
     */
    public static boolean isPropertyExcluded(String elementTag,
            String propertyName, Set<ComponentBasicType> type) {

        boolean hasExcludedType = type != null && type.stream()
                .anyMatch(PROPERTY_TYPE_EXCLUSION_REGISTRY::contains);
        if (hasExcludedType) {
            return true;
        }

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
