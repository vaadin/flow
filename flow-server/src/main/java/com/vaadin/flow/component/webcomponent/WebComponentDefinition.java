/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.component.webcomponent;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonValue;

public interface WebComponentDefinition<C extends Component> {

    <P> PropertyConfiguration<C, P> addProperty(String name, P defaultValue,
                                                Class<P> type);

    default <P> PropertyConfiguration<C, P> addProperty(
            String name, Class<P> type) {
        return addProperty(name, null, type);
    }

    default PropertyConfiguration<C, Integer> addProperty(
            String name, int defaultValue) {
        return addProperty(name, defaultValue, Integer.class);
    }

    default PropertyConfiguration<C, Double> addProperty(
            String name, double defaultValue) {
        return addProperty(name, defaultValue, Double.class);
    }

    default PropertyConfiguration<C, String> addProperty(
            String name, String defaultValue) {
        return addProperty(name, defaultValue, String.class);
    }

    default PropertyConfiguration<C, Boolean> addProperty(
            String name, boolean defaultValue) {
        return addProperty(name, defaultValue, Boolean.class);
    }

    default PropertyConfiguration<C, JsonValue> addProperty(
            String name, JsonValue defaultValue) {
        return addProperty(name, defaultValue, JsonValue.class);
    }

    void setInstanceConfigurator(InstanceConfigurator<C> configurator);

    <P> PropertyConfiguration<C, List<P>> addListProperty(
            String name, Class<P> entryClass);

    <P> PropertyConfiguration<C, List<P>> addListProperty(String name,
                                                          List<P> defaultValue);

    default <P> PropertyConfiguration<C, List<P>> addListProperty(
            String name, P... defaultValues) {
        return addListProperty(name, Arrays.asList(defaultValues));
    }

    Registration exposeEvent(Class<? extends ComponentEvent<?>> eventType);

    <T extends ComponentEvent<?>, R> Registration exposeEvent(
            Class<T> eventType, SerializableFunction<T, R> converter);
}
