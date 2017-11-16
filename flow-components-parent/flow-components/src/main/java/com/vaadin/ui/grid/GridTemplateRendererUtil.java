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
package com.vaadin.ui.grid;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.data.provider.DataKeyMapper;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.function.SerializableConsumer;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.grid.Grid.GridDataGenerator;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.renderers.ComponentRendererUtil;
import com.vaadin.ui.renderers.TemplateRenderer;
import com.vaadin.util.JsonSerializer;

/**
 * Helper class with utility methods used internally by {@link Grid} to support
 * {@link TemplateRenderer}s inside cells, headers, footers and detail rows.
 * <p>
 * This class is not meant to be used outside the scope of the Grid.
 * 
 * @author Vaadin Ltd.
 */
class GridTemplateRendererUtil {

    /**
     * Internal object to hold {@link ComponentRenderer}s and their generated
     * {@link Component}s together.
     * 
     * @param <T>
     *            the model item attached to the component
     */
    static final class RendereredComponent<T> implements Serializable {
        private Component component;
        private ComponentRenderer<? extends Component, T> componentRenderer;

        /**
         * Default constructor.
         * 
         * @param component
         *            the generated component
         * @param componentRenderer
         *            the renderer that generated the component
         */
        public RendereredComponent(Component component,
                ComponentRenderer<? extends Component, T> componentRenderer) {
            this.component = component;
            this.componentRenderer = componentRenderer;
        }

        /**
         * Gets the current generated component.
         * 
         * @return the generated component by the renderer
         */
        public Component getComponent() {
            return component;
        }

        /**
         * Recreates the component by calling
         * {@link ComponentRenderer#createComponent(Object)}, and sets the
         * internal component returned by {@link #getComponent()}.
         * 
         * @param item
         *            the model item to be attached to the component instance
         * @return the new generated component returned by the renderer
         */
        public Component recreateComponent(T item) {
            component = componentRenderer.createComponent(item);
            return component;
        }
    }

    private GridTemplateRendererUtil() {
    }

    static <T> void setupTemplateRenderer(TemplateRenderer<T> renderer,
            Element contentTemplate, Element templateDataHost,
            GridDataGenerator<T> dataGenerator,
            Supplier<DataKeyMapper<T>> keyMapperSupplier) {

        renderer.getValueProviders()
                .forEach((key, provider) -> dataGenerator.addDataGenerator(
                        (item, jsonObject) -> jsonObject.put(key,
                                JsonSerializer.toJson(provider.apply(item)))));

        Map<String, SerializableConsumer<T>> eventConsumers = renderer
                .getEventHandlers();

        if (!eventConsumers.isEmpty()) {
            /*
             * This code allows the template to use Polymer specific syntax for
             * events, such as on-click (instead of the native onclick). The
             * principle is: we set a new function inside the column, and the
             * function is called by the rendered template. For that to work,
             * the template element must have the "__dataHost" property set with
             * the column element.
             */
            templateDataHost.getNode().runWhenAttached(
                    ui -> processTemplateRendererEventConsumers(ui,
                            templateDataHost, eventConsumers,
                            keyMapperSupplier));

            contentTemplate.getNode()
                    .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                            "$0.__dataHost = $1;", contentTemplate,
                            templateDataHost));
        }
    }

    private static <T> void processTemplateRendererEventConsumers(UI ui,
            Element colElement,
            Map<String, SerializableConsumer<T>> eventConsumers,
            Supplier<DataKeyMapper<T>> keyMapperSupplier) {
        eventConsumers.forEach(
                (handlerName, consumer) -> setupTemplateRendererEventHandler(ui,
                        colElement, handlerName, consumer, keyMapperSupplier));
    }

    private static <T> void setupTemplateRendererEventHandler(UI ui,
            Element eventOrigin, String handlerName, Consumer<T> consumer,
            Supplier<DataKeyMapper<T>> keyMapperSupplier) {

        // vaadin.sendEventMessage is an exported function at the client
        // side
        ui.getPage().executeJavaScript(String.format(
                "$0.%s = function(e) {vaadin.sendEventMessage(%d, '%s', {key: e.model.__data.item.key})}",
                handlerName, eventOrigin.getNode().getId(), handlerName),
                eventOrigin);

        eventOrigin.addEventListener(handlerName,
                event -> processEventFromTemplateRenderer(event, handlerName,
                        consumer, keyMapperSupplier));
    }

    private static <T> void processEventFromTemplateRenderer(DomEvent event,
            String handlerName, Consumer<T> consumer,
            Supplier<DataKeyMapper<T>> keyMapperSupplier) {
        if (event.getEventData() != null) {
            String itemKey = event.getEventData().getString("key");
            T item = keyMapperSupplier.get().get(itemKey);

            if (item != null) {
                consumer.accept(item);
            } else {
                Logger.getLogger(GridTemplateRendererUtil.class.getName())
                        .log(Level.INFO, () -> String.format(
                                "Received an event for the handler '%s' with item key '%s', but the item is not present in the KeyMapper. Ignoring event.",
                                handlerName, itemKey));
            }
        } else {
            Logger.getLogger(GridTemplateRendererUtil.class.getName()).log(Level.INFO,
                    () -> String.format(
                            "Received an event for the handler '%s' without any data. Ignoring event.",
                            handlerName));
        }
    }

    static <T> void setupHeaderOrFooterComponentRenderer(Component owner,
            ComponentRenderer<? extends Component, T> componentRenderer) {
        Element container = ComponentRendererUtil
                .createContainerForRenderers(owner);

        componentRenderer.setTemplateAttribute("key", "0");
        componentRenderer.setTemplateAttribute("keyname",
                "data-flow-renderer-item-key");
        componentRenderer.setTemplateAttribute("containerid",
                container.getAttribute("id"));

        Component renderedComponent = componentRenderer.createComponent(null);
        GridTemplateRendererUtil.registerRenderedComponent(componentRenderer, null,
                container, "0", renderedComponent);
    }

    static <T> void registerRenderedComponent(
            ComponentRenderer<? extends Component, T> componentRenderer,
            Map<String, RendereredComponent<T>> renderedComponents,
            Element container, String key, Component component) {
        component.getElement().setAttribute("data-flow-renderer-item-key", key);
        container.appendChild(component.getElement());

        if (renderedComponents != null) {
            renderedComponents.put(key,
                    new RendereredComponent<>(component, componentRenderer));
        }
    }

}
