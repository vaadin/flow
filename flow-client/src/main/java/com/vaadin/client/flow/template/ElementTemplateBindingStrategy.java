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
package com.vaadin.client.flow.template;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.binding.BinderContext;
import com.vaadin.client.flow.binding.ServerEventHandlerBinder;
import com.vaadin.client.flow.binding.ServerEventObject;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.json.JsonObject;
import jsinterop.annotations.JsFunction;

/**
 * Element template binding strategy.
 *
 * @author Vaadin Ltd
 *
 */
public class ElementTemplateBindingStrategy
        extends AbstractTemplateStrategy<Element> {

    /**
     * Event handler listener interface.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    private interface EventHandler {
        void handle(Event event, JavaScriptObject serverProxy, Element element);
    }

    @Override
    protected String getTemplateType() {
        return com.vaadin.flow.template.angular.ElementTemplateNode.TYPE;
    }

    @Override
    protected Element create(StateTree tree, int templateId) {
        String tag = ((ElementTemplateNode) getTemplateNode(tree, templateId))
                .getTag();
        return Browser.getDocument().createElement(tag);
    }

    @Override
    protected void bind(StateNode modelNode, Element element, int templateId,
            TemplateBinderContext context) {
        ElementTemplateNode templateNode = (ElementTemplateNode) getTemplateNode(
                modelNode.getTree(), templateId);
        bindProperties(modelNode, templateNode, element);

        bindClassNames(modelNode, templateNode, element);

        bindAttributes(modelNode, templateNode, element);

        JsArray<Double> children = templateNode.getChildrenIds();
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                int childTemplateId = children.get(i).intValue();

                Node child = createAndBind(modelNode, childTemplateId, context);

                DomApi.wrap(element).appendChild(child);
            }
        }

        registerEventHandlers(context.getTemplateRoot(), templateNode, element);

        MapProperty overrideProperty = modelNode
                .getMap(NodeFeatures.TEMPLATE_OVERRIDES)
                .getProperty(String.valueOf(templateNode.getId()));
        if (overrideProperty.hasValue()) {
            /*
             * Bind right away. We don't need to listen to property value
             * changes since the value will never change once it has been set.
             */
            bindOverrideNode(element, overrideProperty, context);
        } else {
            /*
             * React in case an override nodes appears later on.
             */
            EventRemover remover = overrideProperty
                    .addChangeListener(e -> Reactive
                            .addFlushListener(() -> bindOverrideNode(element,
                                    overrideProperty, context)));

            /*
             * Should preferably remove the change listener immediately when the
             * first event is fired, but Java makes it so difficult to reference
             * the remover from inside the event handler.
             */
            modelNode.addUnregisterListener(e -> remover.remove());
        }

        if (element == context.getTemplateRoot().getDomNode()) {
            // Only bind element.$server for the template root element using the
            // data in the state node. Other elements might get their own
            // $server through an override node
            ServerEventHandlerBinder.bindServerEventHandlerNames(element,
                    context.getTemplateRoot());
        }
    }

    private void bindAttributes(StateNode stateNode,
            ElementTemplateNode templateNode, Element element) {
        bind(stateNode, templateNode.getAttributes(),
                (name, value) -> WidgetUtil.updateAttribute(element, name,
                        value.orElse(null)));
    }

    private void registerEventHandlers(StateNode templateStateNode,
            ElementTemplateNode templateNode, Element element) {
        JsonObject eventHandlers = templateNode.getEventHandlers();
        if (eventHandlers != null) {
            for (String event : eventHandlers.keys()) {
                String handler = WidgetUtil
                        .crazyJsCast(eventHandlers.get(event));
                EventHandler eventHandler = NativeFunction.create("$event",
                        "$server", "$element", handler);
                element.addEventListener(event,
                        evt -> eventHandler.handle(evt, ServerEventObject
                                .get((Element) templateStateNode.getDomNode()),
                                element));
            }
        }
    }

    private void bindProperties(StateNode stateNode,
            ElementTemplateNode templateNode, Element element) {
        bind(stateNode, templateNode.getProperties(),
                (name, value) -> WidgetUtil.setJsProperty(element, name,
                        value.orElse(null)));
    }

    private void bindClassNames(StateNode stateNode,
            ElementTemplateNode templateNode, Element element) {
        bind(stateNode, templateNode.getClassNames(), (name, value) -> {
            if (WidgetUtil.isTrueish(value.orElse(null))) {
                DomApi.wrap(element).getClassList().add(name);
            } else {
                DomApi.wrap(element).getClassList().remove(name);
            }
        });
    }

    private void bindOverrideNode(Element element, MapProperty overrideProperty,
            BinderContext context) {
        StateNode overrideNode = (StateNode) overrideProperty.getValue();

        /*
         * bind checks that the we haven't already bound the same state node
         * previously
         */
        context.bind(overrideNode, element);
    }

}
