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
package com.vaadin.flow.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.ui.Component;

/**
 * A template renderer that allows the usage of regular {@link Component}s
 * inside templates.
 * <p>
 * Internally it uses a {@code <flow-component-renderer>} webcomponent to manage
 * the component instances at the client-side. Some components may used
 * different renderers, by calling {@link #getTemplate(String)}.
 *
 * @author Vaadin Ltd.
 *
 * @param <COMPONENT>
 *            the type of component this renderer can produce
 * @param <ITEM>
 *            the type of the input object that can be used by the rendered
 *            component
 *
 */
public class ComponentTemplateRenderer<COMPONENT extends Component, ITEM>
        extends TemplateRenderer<ITEM>
        implements ComponentRenderer<COMPONENT, ITEM> {

    private SerializableSupplier<COMPONENT> componentSupplier;
    private SerializableFunction<ITEM, COMPONENT> componentFunction;
    private SerializableBiConsumer<COMPONENT, ITEM> itemConsumer;
    private Map<String, String> rendererAttributes = new HashMap<>();

    /**
     * Creates a new ComponentRenderer that uses the componentSupplier to
     * generate new {@link Component} instances, and the itemConsumer to set the
     * related items.
     * <p>
     * Some components may support several rendered components at once, so
     * different component instances should be created for each different item
     * for those components.
     *
     * @param componentSupplier
     *            a supplier that can generate new component instances
     * @param itemConsumer
     *            a setter for the corresponding item for the rendered component
     */
    public ComponentTemplateRenderer(
            SerializableSupplier<COMPONENT> componentSupplier,
            SerializableBiConsumer<COMPONENT, ITEM> itemConsumer) {

        this.componentSupplier = componentSupplier;
        this.itemConsumer = itemConsumer;
    }

    /**
     * Creates a new ComponentRenderer that uses the componentSupplier to
     * generate new {@link Component} instances.
     * <p>
     * This constructor is a convenient way of providing components to a
     * template when the actual model item doesn't matter for the component
     * instance.
     * <p>
     * Some components may support several rendered components at once, so
     * different component instances should be created for each different item
     * for those components.
     *
     * @param componentSupplier
     *            a supplier that can generate new component instances
     */
    public ComponentTemplateRenderer(
            SerializableSupplier<COMPONENT> componentSupplier) {
        this(componentSupplier, null);
    }

    /**
     * Creates a new ComponentRenderer that uses the componentFunction to
     * generate new {@link Component} instances. The function takes a model item
     * and outputs a component instance.
     * <p>
     * Some components may support several rendered components at once, so
     * different component instances should be created for each different item
     * for those components.
     *
     * @param componentFunction
     *            a function that can generate new component instances
     */
    public ComponentTemplateRenderer(
            SerializableFunction<ITEM, COMPONENT> componentFunction) {
        this.componentFunction = componentFunction;
    }

    /**
     * Sets attributes to the internal component renderer, that can affect the
     * actual rendering.
     * <p>
     * It is used internally by components that support ComponentRenderers.
     *
     * @param attribute
     *            the attribute to set on the internal component renderer
     *            element
     * @param value
     *            the value of the attribute, not <code>null</code>
     */
    public void setTemplateAttribute(String attribute, String value) {
        rendererAttributes.put(attribute, value);
    }

    /**
     * Gets the final template to be used to render the components, using the
     * {@code <flow-component-renderer>} webcomponent as the client-side
     * renderer.
     * 
     * @return the template ready to be used
     * @see #getTemplate(String)
     */
    @Override
    public String getTemplate() {
        return getTemplate("flow-component-renderer");
    }

    /**
     * Gets the final template to be used to render the components. The {@code
     * componentRendererTag} parameter defines the tag of the client-side
     * webcomponent responsible for rendering the actual components.
     * 
     * @param componentRendererTag
     *            the tag of the client-side renderer webcomponent, not
     *            <code>null</code>
     * @return the template ready to be used
     */
    public String getTemplate(String componentRendererTag) {
        Objects.requireNonNull(componentRendererTag,
                "The componentRendererTag can not be null");
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(componentRendererTag);
        rendererAttributes.entrySet()
                .forEach(entry -> builder.append(" ").append(entry.getKey())
                        .append("=\"").append(entry.getValue()).append("\""));
        builder.append("></").append(componentRendererTag).append(">");

        return builder.toString();
    }

    /**
     * Effectively calls the functions to get component instances and set the
     * model items. This is called internally by the components which support
     * ComponentRenderers.
     *
     * @param item
     *            the corresponding model item for the component
     * @return a component instance
     */
    @Override
    public COMPONENT createComponent(ITEM item) {
        if (componentFunction != null) {
            return componentFunction.apply(item);
        }
        COMPONENT component = componentSupplier.get();
        if (itemConsumer != null) {
            itemConsumer.accept(component, item);
        }
        return component;
    }

}
