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
package com.vaadin.flow.data.renderer;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ComponentDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;

/**
 * Base class for all renderers that support arbitrary {@link Component}s.
 * <p>
 * Components that support renderers should use the appropriate method from this
 * class to provide component rendering: {@link #render(Element, KeyMapper)} for
 * components that uses {@code <template>}, and {@link #createComponent(Object)}
 * for components that use light-dom.
 * 
 * @author Vaadin Ltd.
 *
 * @param <COMPONENT>
 *            the type of the output component
 * @param <SOURCE>
 *            the type of the input model object
 */
public class ComponentRenderer<COMPONENT extends Component, SOURCE>
        extends Renderer<SOURCE> {

    private SerializableSupplier<COMPONENT> componentSupplier;
    private SerializableFunction<SOURCE, COMPONENT> componentFunction;
    private SerializableBiConsumer<COMPONENT, SOURCE> itemConsumer;
    private String componentRendererTag = "flow-component-renderer";

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
    public ComponentRenderer(SerializableSupplier<COMPONENT> componentSupplier,
            SerializableBiConsumer<COMPONENT, SOURCE> itemConsumer) {

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
    public ComponentRenderer(
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
    public ComponentRenderer(
            SerializableFunction<SOURCE, COMPONENT> componentFunction) {
        this.componentFunction = componentFunction;
    }

    /**
     * Default constructor, that can be used by subclasses which supports
     * different ways of creating components, other than those defined in the
     * other constructors.
     */
    protected ComponentRenderer() {
    }

    @Override
    public Rendering<SOURCE> render(Element container,
            KeyMapper<SOURCE> keyMapper) {

        ComponentRendering rendering = new ComponentRendering(
                keyMapper == null ? null : keyMapper::key);
        rendering.setTemplateElement(new Element("template", false));

        container.getNode()
                .runWhenAttached(ui -> ui.getInternals().getStateTree()
                        .beforeClientResponse(container.getNode(),
                                () -> setupTemplateWhenAttached(ui, container,
                                        rendering, keyMapper)));

        return rendering;
    }

    /**
     * Sets the tag of the webcomponent used at the client-side to manage
     * component rendering inside {@code <template>}. By default it uses
     * {@code <flow-component-renderer>}.
     * 
     * @param componentRendererTag
     *            the tag of the client-side webcomponent for component
     *            rendering, not <code>null</code>
     */
    public void setComponentRendererTag(String componentRendererTag) {
        Objects.requireNonNull(componentRendererTag,
                "The componentRendererTag should not be null");
        this.componentRendererTag = componentRendererTag;
    }

    private void setupTemplateWhenAttached(UI ui, Element owner,
            ComponentRendering rendering, KeyMapper<SOURCE> keyMapper) {
        String appId = ui.getInternals().getAppId();
        Element templateElement = rendering.getTemplateElement().get();
        owner.appendChild(templateElement);

        Element container = new Element("div", false);
        owner.appendVirtualChild(container);
        rendering.setContainer(container);
        String templateInnerHtml;

        if (keyMapper != null) {
            String nodeIdPropertyName = "_renderer_"
                    + templateElement.getNode().getId();

            templateInnerHtml = String.format(
                    "<%s appid=\"%s\" nodeid=\"[[item.%s]]\"></%s>",
                    componentRendererTag, appId, nodeIdPropertyName,
                    componentRendererTag);
            rendering.setNodeIdPropertyName(nodeIdPropertyName);
        } else {
            COMPONENT component = createComponent(null);
            if (component != null) {
                container.appendChild(component.getElement());

                templateInnerHtml = String.format(
                        "<%s appid=\"%s\" nodeid=\"%s\"></%s>",
                        componentRendererTag, appId,
                        component.getElement().getNode().getId(),
                        componentRendererTag);
            } else {
                templateInnerHtml = "";
            }
        }

        templateElement.setProperty("innerHTML", templateInnerHtml);
    }

    /**
     * Creates a component for a given object model item. Subclasses can
     * override this method to provide specific behavior.
     * 
     * @param item
     *            the model item, possibly <code>null</code>
     * @return a component instance representing the provided item
     */
    public COMPONENT createComponent(SOURCE item) {
        if (componentFunction != null) {
            return componentFunction.apply(item);
        }
        COMPONENT component = componentSupplier.get();
        if (itemConsumer != null) {
            itemConsumer.accept(component, item);
        }
        return component;
    }

    private class ComponentRendering extends ComponentDataGenerator<SOURCE>
            implements Rendering<SOURCE> {

        private Element templateElement;

        public ComponentRendering(ValueProvider<SOURCE, String> keyMapper) {
            super(ComponentRenderer.this, keyMapper);
        }

        public void setTemplateElement(Element templateElement) {
            this.templateElement = templateElement;
        }

        @Override
        public Optional<Element> getTemplateElement() {
            return Optional.ofNullable(templateElement);
        }

        @Override
        public Optional<DataGenerator<SOURCE>> getDataGenerator() {
            return Optional.of(this);
        }
    }

}
