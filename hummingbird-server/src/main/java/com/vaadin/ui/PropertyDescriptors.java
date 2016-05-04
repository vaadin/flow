/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.hummingbird.dom.Element;

/**
 * Factory methods for creating {@link PropertyDescriptor} instances.
 *
 * @author Vaadin Ltd
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
        public void set(Component component, S value) {
            assert component != null;

            Element e = component.getElement();
            if (value == null) {
                throw new IllegalArgumentException(
                        "Cannot set " + name + " to null");
            } else if (defaultValue.equals(value)) {
                remover.accept(e);
            } else {
                setter.accept(e, value);
            }
        }

        @Override
        public G get(Component component) {
            assert component != null;

            S rawValue = getter.apply(component.getElement());

            return returnWrapper.apply(rawValue, defaultValue);
        }
    }

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
        return new PropertyDescriptorImpl<String, String>(name, defaultValue,
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
        return attribute(name, defaultValue, nullToDefault());
    }

    /**
     * Creates a descriptor for an optional attribute of the component's root
     * element with a non-null default value. The getter will return an empty
     * optional if the the element doesn't have the attribute, or if its value
     * is the default value.
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
        return attribute(name, defaultValue, defaultToEmpty());
    }

    /**
     * Helper for creating an attribute descriptor with a specific return value
     * wrapper.
     *
     * @param name
     *            the name of the element attribute, not <code>null</code>
     * @param defaultValue
     *            the default value of the property, not <code>null</code>
     * @param returnWrapper
     *            a callback that returns the actual value given the attribute
     *            value and the default value
     * @return a property descriptor, not <code>null</code>
     */
    private static <T> PropertyDescriptor<String, T> attribute(String name,
            String defaultValue, BiFunction<String, String, T> returnWrapper) {
        return new PropertyDescriptorImpl<String, T>(name, defaultValue,
                (element, value) -> element.setAttribute(name, value),
                element -> element.removeAttribute(name),
                element -> element.getAttribute(name), returnWrapper);
    }

    /**
     * Creates a return value wrapper that wraps the provided value in an
     * optional, which is empty if the value is the provided default value or
     * <code>null</code>.
     *
     * @return the return value wrapper, not <code>null</code>
     */
    private static <T> BiFunction<T, T, Optional<T>> defaultToEmpty() {
        return (rawValue, defaultValue) -> {
            if (Objects.equals(rawValue, defaultValue)) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(rawValue);
            }
        };
    }

    /**
     * Creates a return value wrapper that returns the default value if the
     * original value is <code>null</code>, otherwise the original value is
     * returned.
     *
     * @return the return value wrapper, not <code>null</code>
     */
    private static <T> BiFunction<T, T, T> nullToDefault() {
        return (rawValue, defaultValue) -> {
            if (rawValue == null) {
                return defaultValue;
            } else {
                return rawValue;
            }
        };
    }

}
