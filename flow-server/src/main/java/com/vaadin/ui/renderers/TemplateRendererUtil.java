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
package com.vaadin.ui.renderers;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.function.SerializableConsumer;
import com.vaadin.function.ValueProvider;
import com.vaadin.ui.UI;

/**
 * Class used internally by components that support {@link TemplateRenderer}. It
 * contains helper methods to register events triggered by rendered templates.
 * 
 * @author Vaadin Ltd.
 *
 */
public class TemplateRendererUtil {

    private TemplateRendererUtil() {
    }

    /**
     * Registers the event handlers associated to a {@link TemplateRenderer}, if
     * any. The consumers returned by
     * {@link TemplateRenderer#getEventHandlers()} are processed and properly
     * configured to trigger messages from the client to the server, so event
     * handlers can catch the events and execute custom logic.
     * 
     * @param <T>
     *            the type of the renderer and the associated keyMapper
     * @param renderer
     *            the TemplateRenderer to be evaluated
     * @param contentTemplate
     *            the {@code <template>} element that contains the children that
     *            fire events
     * @param templateDataHost
     *            the data host of the events - typically the parent of the
     *            contentTemplate, but not necessarily
     * @param keyMapper
     *            a value provider that knows how to return items by a given key
     */
    public static <T> void registerEventHandlers(TemplateRenderer<T> renderer,
            Element contentTemplate, Element templateDataHost,
            ValueProvider<String, T> keyMapper) {

        Map<String, SerializableConsumer<T>> eventHandlers = renderer
                .getEventHandlers();

        if (!eventHandlers.isEmpty()) {
            /*
             * This code allows the template to use Polymer specific syntax for
             * events, such as on-click (instead of the native onclick). The
             * principle is: we set a new function inside the column, and the
             * function is called by the rendered template. For that to work,
             * the template element must have the "__dataHost" property set with
             * the column element.
             */
            templateDataHost.getNode().runWhenAttached(
                    ui -> processTemplateRendererEventHandlers(ui,
                            templateDataHost, eventHandlers, keyMapper));

            contentTemplate.getNode()
                    .runWhenAttached(ui -> ui.getPage().executeJavaScript(
                            "$0.__dataHost = $1;", contentTemplate,
                            templateDataHost));
        }
    }

    private static <T> void processTemplateRendererEventHandlers(UI ui,
            Element colElement,
            Map<String, SerializableConsumer<T>> eventConsumers,
            Function<String, T> keyMapper) {
        eventConsumers.forEach(
                (handlerName, consumer) -> setupTemplateRendererEventHandler(ui,
                        colElement, handlerName, consumer, keyMapper));
    }

    private static <T> void setupTemplateRendererEventHandler(UI ui,
            Element eventOrigin, String handlerName, Consumer<T> consumer,
            Function<String, T> keyMapper) {

        // vaadin.sendEventMessage is an exported function at the client
        // side
        ui.getPage().executeJavaScript(String.format(
                "$0.%s = function(e) {vaadin.sendEventMessage(%d, '%s', {key: e.model.__data.item.key})}",
                handlerName, eventOrigin.getNode().getId(), handlerName),
                eventOrigin);

        eventOrigin.addEventListener(handlerName,
                event -> processEventFromTemplateRenderer(event, handlerName,
                        consumer, keyMapper));
    }

    private static <T> void processEventFromTemplateRenderer(DomEvent event,
            String handlerName, Consumer<T> consumer,
            Function<String, T> keyMapper) {
        if (event.getEventData() != null) {
            String itemKey = event.getEventData().getString("key");
            T item = keyMapper.apply(itemKey);

            if (item != null) {
                consumer.accept(item);
            } else {
                LoggerFactory.getLogger(TemplateRendererUtil.class.getName())
                        .info("Received an event for the handler '{}' with item key '{}', but the item is not present in the KeyMapper. Ignoring event.",
                                handlerName, itemKey);
            }
        } else {
            LoggerFactory.getLogger(TemplateRendererUtil.class.getName())
                    .info("Received an event for the handler '{}' without any data. Ignoring event.",
                            handlerName);
        }
    }

}
