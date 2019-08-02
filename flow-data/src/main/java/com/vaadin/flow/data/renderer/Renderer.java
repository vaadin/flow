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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.JsonSerializer;

/**
 * Base class for all renderers - classes that take a given model object as
 * input and outputs a set of elements that represents that item in the UI.
 * <p>
 * The default renderer only supports template based rendering. For component
 * support, check {@link ComponentRenderer}.
 * 
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the input object used inside the template
 * 
 * @see ValueProvider
 * @see ComponentRenderer
 * @see TemplateRenderer
 * @see <a href=
 *      "https://www.polymer-project.org/2.0/docs/devguide/templates">https://www.polymer-project.org/2.0/docs/devguide/templates</a>
 *
 * @param <SOURCE>
 *            the object model type
 */
public class Renderer<SOURCE> implements Serializable {

    private String template;
    private Map<String, ValueProvider<SOURCE, ?>> valueProviders;
    private Map<String, SerializableConsumer<SOURCE>> eventHandlers;

    /**
     * Default constructor.
     */
    protected Renderer() {
    }

    /**
     * Builds a renderer with the specified template.
     * 
     * @param template
     *            the template used by the renderer
     */
    protected Renderer(String template) {
        this.template = template;
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
     * Renderer.
     * 
     * @param property
     *            the name of the property used inside the template, not
     *            <code>null</code>
     * @param provider
     *            a {@link ValueProvider} that provides the actual value for the
     *            property, not <code>null</code>
     */
    protected void setProperty(String property,
            ValueProvider<SOURCE, ?> provider) {
        Objects.requireNonNull(property, "The property must not be null");
        Objects.requireNonNull(provider, "The value provider must not be null");
        if (valueProviders == null) {
            valueProviders = new HashMap<>();
        }
        valueProviders.put(property, provider);
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
     * @see <a href=
     *      "https://www.polymer-project.org/2.0/docs/devguide/events">https://www.polymer-project.org/2.0/docs/devguide/events</a>
     */
    protected void setEventHandler(String handlerName,
            SerializableConsumer<SOURCE> handler) {
        Objects.requireNonNull(handlerName, "The handlerName must not be null");
        Objects.requireNonNull(handler, "The event handler must not be null");
        if (eventHandlers == null) {
            eventHandlers = new HashMap<>();
        }
        eventHandlers.put(handlerName, handler);
    }

    /**
     * Handles the rendering of the model objects by creating a new
     * {@code <template>} element in the given container.
     * 
     * @param container
     *            the element in which the template will be attached to
     * @param keyMapper
     *            mapper used internally to fetch items by key and to provide
     *            keys for given items. It is required when either event
     *            handlers or {@link DataGenerator} are supported
     * @return the context of the rendering, that can be used by the components
     *         to provide extra customization
     */
    public Rendering<SOURCE> render(Element container,
            DataKeyMapper<SOURCE> keyMapper) {
        return render(container, keyMapper, new Element("template"));
    }

    /**
     * Handles the rendering of the model objects by using the given
     * {@code <template>} element in the given container.
     * <p>
     * Subclasses of Renderer usually override this method to provide additional
     * features.
     * 
     * @param container
     *            the element in which the template will be attached to, not
     *            {@code null}
     * @param keyMapper
     *            mapper used internally to fetch items by key and to provide
     *            keys for given items. It is required when either event
     *            handlers or {@link DataGenerator} are supported
     * @param contentTemplate
     *            the {@code <template>} element to be used for rendering in the
     *            container, not {@code null}
     * @return the context of the rendering, that can be used by the components
     *         to provide extra customization
     */
    public Rendering<SOURCE> render(Element container,
            DataKeyMapper<SOURCE> keyMapper, Element contentTemplate) {
        Objects.requireNonNull(template,
                "The template string is null. Either build the Renderer by using the 'Renderer(String)' constructor or override the 'render' method to provide custom behavior");

        contentTemplate.setProperty("innerHTML", template);

        if (contentTemplate.getParent() != container) {
            container.appendChild(contentTemplate);
        }

        if (keyMapper != null) {
            RendererUtil.registerEventHandlers(this, contentTemplate, container,
                    keyMapper::get);
        }
        return new TemplateRendering(contentTemplate);
    }

    /**
     * Gets the property mapped to {@link ValueProvider}s in this renderer. The
     * returned map is immutable.
     * 
     * @return the mapped properties, never <code>null</code>
     */
    public Map<String, ValueProvider<SOURCE, ?>> getValueProviders() {
        return valueProviders == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(valueProviders);
    }

    /**
     * Gets the event handlers linked to this renderer. The returned map is
     * immutable.
     * 
     * @return the mapped event handlers, never <code>null</code>
     * @see #setEventHandler(String, SerializableConsumer)
     */
    public Map<String, SerializableConsumer<SOURCE>> getEventHandlers() {
        return eventHandlers == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(eventHandlers);
    }

    private class TemplateRendering implements Rendering<SOURCE> {

        private final Element templateElement;

        public TemplateRendering(Element templateElement) {
            this.templateElement = templateElement;
        }

        @Override
        public Optional<DataGenerator<SOURCE>> getDataGenerator() {
            if (valueProviders == null || valueProviders.isEmpty()) {
                return Optional.empty();
            }
            CompositeDataGenerator<SOURCE> composite = new CompositeDataGenerator<>();

            valueProviders.forEach((key, provider) -> composite
                    .addDataGenerator((item, jsonObject) -> jsonObject.put(key,
                            JsonSerializer.toJson(provider.apply(item)))));
            return Optional.of(composite);
        }

        @Override
        public Element getTemplateElement() {
            return templateElement;
        }

    }

}
