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
package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;

import com.vaadin.flow.component.Component;

import elemental.json.JsonValue;

/**
 * WebComponent to be configured by {@link InstanceConfigurator}.
 * <p>
 * Enables high-level communication from server to client such as updating
 * property values on the client-side and firing custom events.
 *
 * @param <C>   {@code component} exported as web component
 */
public interface WebComponent<C extends Component> extends Serializable {

    /**
     * Fires a custom event on the client-side originating from the web
     * component. This event does not bubble in the DOM hierarchy.
     *
     * @param eventName
     *              name of the event, not null
     * @see #fireEvent(String, JsonValue, EventOptions) for full set of options
     */
    void fireEvent(String eventName);

    /**
     * Fires a custom event on the client-side originating from the web
     * component with custom event data. This event does not bubble in the DOM
     * hierarchy.
     *
     * @param eventName
     *              name of the event, not null
     * @param objectData
     *              data the event should carry. This data is placed as the
     *              {@code detail} property of the event, not null
     * @see #fireEvent(String, JsonValue, EventOptions) for full set of options
     */
    void fireEvent(String eventName, JsonValue objectData);

    /**
     * Fires a custom event on the client-side originating from the web
     * component with custom event data. Allows modifying the default event
     * behavior with {@link EventOptions}.
     *
     * @param eventName
     *              name of the event, not null
     * @param objectData
     *              data the event should carry. This data is placed as the
     *              {@code detail} property of the event, not null
     * @param options
     *              event options for {@code bubbles}, {@code cancelable},
     *              and {@code composed} flags, not null
     */
    void fireEvent(String eventName, JsonValue objectData,
                   EventOptions options);

    /**
     * Sets property value on the client-side to the given {@code value}. The
     * required {@link PropertyConfiguration} is received from
     * {@link WebComponentDefinition} when a new property is added for the
     * web component.
     *
     * @param propertyConfiguration
     *              identifies the property for which the value is being set,
     *              not null
     * @param value
     *              new value for the property, can be null
     * @param <P>
     *              type of the property value being set. If the type does
     *              not match the original property type, throws an exception
     */
    <P extends Serializable> void setProperty(
            PropertyConfiguration<C, P> propertyConfiguration, P value);
}
