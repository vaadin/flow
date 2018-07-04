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
package com.vaadin.flow.component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.dom.Element;

/**
 * Factory methods for creating {@link PropertyDescriptor} instances.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class PropertyDescriptors {

    private static final class PropertyDescriptorImpl<S, G>
            implements PropertyDescriptor<S, G> {
        private String name;
        private S defaultValue;
        private Consumer<Element> remover;
        private Function<Element, S> getter;
        private BiFunction<S, S, G> returnWrapper;
        private BiConsumer<Element, S> setter;

        private PropertyDescriptorImpl(String name, S defaultValue,
                BiConsumer<Element, S> setter, Consumer<Element> remover,
                Function<Element, S> getter,
                BiFunction<S, S, G> returnWrapper) {
            assert name != null;
            assert defaultValue != null;
            assert setter != null;
            assert remover != null;
            assert getter != null;

            this.name = name;
            this.defaultValue = defaultValue;
            this.setter = setter;
            this.remover = remover;
            this.getter = getter;
            this.returnWrapper = returnWrapper;
        }

        @Override
        public void set(Element element, S value) {
            assert element != null;

            if (value == null) {
                throw new IllegalArgumentException(
                        "Cannot set " + name + " to null");
            } else if (defaultValue.equals(value)) {
                remover.accept(element);
            } else {
                setter.accept(element, value);
            }
        }

        @Override
        public G get(Element element) {
            assert element != null;

            S rawValue = getter.apply(element);

            return returnWrapper.apply(rawValue, defaultValue);
        }

        @Override
        public String getPropertyName() {
            return name;
        }
    }

    private static final BiFunction<Object, Object, Object> NULL_TO_DEFAULT = new BiFunction<Object, Object, Object>() {
        @Override
        public Object apply(Object rawValue, Object defaultValue) {
            if (rawValue == null) {
                return defaultValue;
            } else {
                return rawValue;
            }
        }
    };

    private static final BiFunction<Object, Object, Optional<Object>> DEFAULT_TO_EMPTY = new BiFunction<Object, Object, Optional<Object>>() {
        @Override
        public Optional<Object> apply(Object rawValue, Object defaultValue) {
            if (Objects.equals(rawValue, defaultValue)) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(rawValue);
            }
        }
    };

    private PropertyDescriptors() {
        // Only static helpers
    }

    /**
     * Creates a descriptor for a property of the component's root element with
     * a non-null default value.
     *
     * @param name
     *            the name of the element property, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<String, String> propertyWithDefault(
            String name, String defaultValue) {
        return new PropertyDescriptorImpl<>(name, defaultValue,
                (element, value) -> element.setProperty(name, value),
                element -> element.removeProperty(name),
                element -> element.getProperty(name, defaultValue),
                nullToDefault());
    }

    /**
     * Creates a descriptor for a property of the component's root element with
     * a non-null default value.
     *
     * @param name
     *            the name of the element property, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<Integer, Integer> propertyWithDefault(
            String name, Integer defaultValue) {
        return new PropertyDescriptorImpl<>(name, defaultValue,
                (element, value) -> element.setProperty(name, value),
                element -> element.removeProperty(name),
                element -> element.getProperty(name, defaultValue),
                nullToDefault());
    }

    /**
     * Creates a descriptor for a property of the component's root element with
     * a non-null default value.
     *
     * @param name
     *            the name of the element property, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<Double, Double> propertyWithDefault(
            String name, Double defaultValue) {
        return new PropertyDescriptorImpl<>(name, defaultValue,
                (element, value) -> element.setProperty(name, value),
                element -> element.removeProperty(name),
                element -> element.getProperty(name, defaultValue),
                nullToDefault());
    }

    /**
     * Creates a descriptor for a property of the component's root element with
     * a non-null default value.
     *
     * @param name
     *            the name of the element property, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<Boolean, Boolean> propertyWithDefault(
            String name, Boolean defaultValue) {
        return new PropertyDescriptorImpl<>(name, defaultValue,
                (element, value) -> element.setProperty(name, value),
                element -> element.removeProperty(name),
                element -> element.getProperty(name, defaultValue),
                nullToDefault());
    }

    /**
     * Creates a descriptor for an attribute of the component's root element
     * with a non-null default value.
     *
     * @param name
     *            the name of the element attribute, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<String, String> attributeWithDefault(
            String name, String defaultValue) {
        return attributeWithDefault(name, defaultValue, true);
    }

    /**
     * Creates a descriptor for an attribute of the component's root element
     * with a non-null default value.
     *
     * @param name
     *            the name of the element attribute, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     * @param removeDefault
     *            if {@code true} then attribute with default value will be
     *            removed, otherwise attribute with the default value will be
     *            kept as is
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<String, String> attributeWithDefault(
            String name, String defaultValue, boolean removeDefault) {
        return attribute(name, defaultValue, removeDefault, nullToDefault());
    }

    /**
     * Creates a descriptor for an optional attribute of the component's root
     * element with a non-null default value. The getter will return an empty
     * optional if the element doesn't have the attribute, or if its value is
     * the default value.
     *
     * @param name
     *            the name of the element attribute, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<String, Optional<String>> optionalAttributeWithDefault(
            String name, String defaultValue) {
        return optionalAttributeWithDefault(name, defaultValue, true);
    }

    /**
     * Creates a descriptor for an optional attribute of the component's root
     * element with a non-null default value. The getter will return an empty
     * optional if the element doesn't have the attribute, or if its value is
     * the default value.
     *
     * @param name
     *            the name of the element attribute, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     * @param removeDefault
     *            if {@code true} then attribute with default value will be
     *            removed, otherwise attribute with the default value will be
     *            kept as is
     *
     * @return a property descriptor, not <code>null</code>
     */
    public static PropertyDescriptor<String, Optional<String>> optionalAttributeWithDefault(
            String name, String defaultValue, boolean removeDefault) {
        return attribute(name, defaultValue, removeDefault, defaultToEmpty());
    }

    /**
     * Helper for creating an attribute descriptor with a specific return value
     * wrapper.
     *
     * @param name
     *            the name of the element attribute, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     * @param removeDefault
     *            if {@code true} then attribute with default value will be
     *            removed, otherwise attribute with the default value will be
     *            kept as is
     * @param returnWrapper
     *            a callback that returns the actual value given the attribute
     *            value and the default value
     * @return a property descriptor, not <code>null</code>
     */
    private static <T> PropertyDescriptor<String, T> attribute(String name,
            String defaultValue, boolean removeDefault,
            BiFunction<String, String, T> returnWrapper) {
        return new PropertyDescriptorImpl<>(name, defaultValue,
                (element, value) -> element.setAttribute(name, value),
                removeDefault ? element -> element.removeAttribute(name)
                        : element -> element.setAttribute(name, defaultValue),
                element -> element.getAttribute(name), returnWrapper);
    }

    /**
     * Creates a return value wrapper that wraps the provided value in an
     * optional, which is empty if the value is the provided default value or
     * <code>null</code>.
     *
     * @return the return value wrapper, not <code>null</code>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> BiFunction<T, T, Optional<T>> defaultToEmpty() {
        return (BiFunction) DEFAULT_TO_EMPTY;
    }

    /**
     * Creates a return value wrapper that returns the default value if the
     * original value is <code>null</code>, otherwise the original value is
     * returned.
     *
     * @return the return value wrapper, not <code>null</code>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> BiFunction<T, T, T> nullToDefault() {
        return (BiFunction) NULL_TO_DEFAULT;
    }

}
