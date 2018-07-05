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
package com.vaadin.flow.data.renderer;

import java.util.Objects;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.JsonSerializer;

/**
 * Helper class to create {@link Renderer} instances, with fluent API.
 * 
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the input object used inside the template
 * 
 * @see #of(String)
 * @see <a href=
 *      "https://www.polymer-project.org/2.0/docs/devguide/templates">https://www.polymer-project.org/2.0/docs/devguide/templates</a>
 */
public final class TemplateRenderer<SOURCE> extends Renderer<SOURCE> {

    /**
     * Creates a new TemplateRenderer based on the provided template. The
     * template accepts anything that is allowed inside a {@code <template>}
     * element, and works with Polymer data binding syntax.
     * <p>
     * Examples:
     * 
     * <pre>
     * {@code
     * // Prints the index of the item inside a repeating list
     * TemplateRenderer.of("[[index]]");
     * 
     * // Prints the property of an item
     * TemplateRenderer.of("<div>Property: [[item.property]]</div>");
     * }
     * </pre>
     * 
     * @param <SOURCE>
     *            the type of the input object used inside the template
     * @param template
     *            the template used to render items, not <code>null</code>
     * @return an initialized TemplateRenderer
     * @see TemplateRenderer#withProperty(String, ValueProvider)
     */
    public static <SOURCE> TemplateRenderer<SOURCE> of(String template) {
        Objects.requireNonNull(template);
        TemplateRenderer<SOURCE> renderer = new TemplateRenderer<>(template);
        return renderer;
    }

    private TemplateRenderer(String template) {
        super(template);
    }

    /**
     * Sets a property to be used inside the template. Each property is
     * referenced inside the template by using the {@code [[item.property]]}
     * syntax.
     * <p>
     * Examples:
     * 
     * <pre>
     * {@code
     * // Regular property
     * TemplateRenderer.<Person> of("<div>Name: [[item.name]]</div>")
     *          .withProperty("name", Person::getName);
     * 
     * // Property that uses a bean. Note that in this case the entire "Address" object will be sent to the template.
     * // Note that even properties of the bean which are not used in the template are sent to the client, so use
     * // this feature with caution.
     * TemplateRenderer.<Person> of("<span>Street: [[item.address.street]]</span>")
     *          .withProperty("address", Person::getAddress); 
     * 
     * // In this case only the street field inside the Address object is sent
     * TemplateRenderer.<Person> of("<span>Street: [[item.street]]</span>")
     *          .withProperty("street", person -> person.getAddress().getStreet());
     * }
     * </pre>
     * 
     * Any types supported by the {@link JsonSerializer} are valid types for the
     * TemplateRenderer.
     * 
     * @param property
     *            the name of the property used inside the template, not
     *            <code>null</code>
     * @param provider
     *            a {@link ValueProvider} that provides the actual value for the
     *            property, not <code>null</code>
     * @return this instance for method chaining
     */
    public TemplateRenderer<SOURCE> withProperty(String property,
            ValueProvider<SOURCE, ?> provider) {
        setProperty(property, provider);
        return this;
    }

    /**
     * Sets an event handler for events from elements inside the template. Each
     * event is referenced inside the template by using the {@code on-event}
     * syntax.
     * <p>
     * Examples:
     * 
     * <pre>
     * {@code
     * // Standard event
     * TemplateRenderer.of("<button on-click='handleClick'>Click me</button>")
     *          .withEventHandler("handleClick", object -> doSomething());
     * 
     * // You can handle custom events from webcomponents as well, using the same syntax
     * TemplateRenderer.of("<my-webcomponent on-customevent=
    'onCustomEvent'></my-webcomponent>")
     *          .withEventHandler("onCustomEvent", object -> doSomething());
     * }
     * </pre>
     * 
     * The name of the function used on the {@code on-event} attribute should be
     * the name used at the handlerName parameter. This name must be a valid
     * Javascript function name.
     * 
     * @param handlerName
     *            the name of the handler used inside the
     *            {@code on-event="handlerName"}, not <code>null</code>
     * @param handler
     *            the handler executed when the event is triggered, not
     *            <code>null</code>
     * @return this instance for method chaining
     * @see <a href=
     *      "https://www.polymer-project.org/2.0/docs/devguide/events">https://www.polymer-project.org/2.0/docs/devguide/events</a>
     */
    public TemplateRenderer<SOURCE> withEventHandler(String handlerName,
            SerializableConsumer<SOURCE> handler) {
        setEventHandler(handlerName, handler);
        return this;
    }
}
