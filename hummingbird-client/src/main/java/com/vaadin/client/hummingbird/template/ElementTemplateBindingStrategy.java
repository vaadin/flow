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
package com.vaadin.client.hummingbird.template;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.ConstantPool;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.binding.BinderContext;
import com.vaadin.client.hummingbird.binding.ServerEventHandlerBinder;
import com.vaadin.client.hummingbird.binding.ServerEventObject;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.dom.DomApi;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeMap;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
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
     * Just a context class whose instance is passed as a parameter between the
     * operations of various kind to be able to access the data like listeners,
     * node and element which they operate on.
     * <p>
     * It's used to avoid having methods with a long numbers of parameters and
     * because the strategy instance is stateless.
     *
     */
    private static class BindingContext {
        private final Element element;
        private final StateNode node;

        private final JsMap<String, Computation> listenerBindings = JsCollections
                .map();
        private final JsMap<String, EventRemover> listenerRemovers = JsCollections
                .map();

        private BindingContext(StateNode node, Element element) {
            this.node = node;
            this.element = element;
        }
    }

    /**
     * Callback interface for an event data expression parsed using new
     * Function() in JavaScript.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    private interface EventDataExpression {
        /**
         * Callback interface for an event data expression parsed using new
         * Function() in JavaScript.
         *
         * @param event
         *            Event to expand
         * @param element
         *            target Element
         * @return Result of evaluated function
         */
        JsonValue evaluate(Event event, Element element);
    }

    /**
     * Event handler listener interface.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    private interface EventHandler {
        void handle(Event event, JavaScriptObject serverProxy, Element element);
    }

    private static final JsMap<String, EventDataExpression> expressionCache = JsCollections
            .map();

    @Override
    protected String getTemplateType() {
        return com.vaadin.hummingbird.template.angular.ElementTemplateNode.TYPE;
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
        bindPolymerEventHandlerNames(
                new BindingContext(templateStateNode, element));
    }

    private EventRemover bindPolymerEventHandlerNames(BindingContext context) {
        NodeMap elementListeners = context.node
                .getMap(NodeFeatures.POLYMER_EVENT_LISTENERS);
        elementListeners.forEachProperty((property,
                name) -> bindEventHandlerProperty(property, context));
        elementListeners.addPropertyAddListener(
                event -> bindEventHandlerProperty(event.getProperty(),
                        context));

        return ServerEventHandlerBinder.bindServerEventHandlerNames(
                () -> WidgetUtil.crazyJsoCast(context.element), context.node,
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS);
    }

    private void bindEventHandlerProperty(MapProperty eventHandlerProperty,
            BindingContext context) {
        String name = eventHandlerProperty.getName();
        assert !context.listenerBindings.has(name);

        Computation computation = Reactive.runWhenDepedenciesChange(() -> {
            boolean hasValue = eventHandlerProperty.hasValue();
            boolean hasListener = context.listenerRemovers.has(name);

            if (hasValue != hasListener) {
                if (hasValue) {
                    addEventHandler(name, context);
                } else {
                    removeEventHandler(name, context);
                }
            }
        });

        context.listenerBindings.set(name, computation);

    }

    private void removeEventHandler(String eventType, BindingContext context) {
        EventRemover remover = context.listenerRemovers.get(eventType);
        context.listenerRemovers.delete(eventType);

        assert remover != null;
        remover.remove();
    }

    private void addEventHandler(String eventType, BindingContext context) {
        assert !context.listenerRemovers.has(eventType);

        EventRemover remover = context.element.addEventListener(eventType,
                event -> handleDomEvent(event, context.element, context.node),
                false);

        context.listenerRemovers.set(eventType, remover);
    }

    private void handleDomEvent(Event event, Element element, StateNode node) {
        String type = event.getType();

        NodeMap listenerMap = node.getMap(NodeFeatures.ELEMENT_LISTENERS);

        ConstantPool constantPool = node.getTree().getRegistry()
                .getConstantPool();
        String expressionConstantKey = (String) listenerMap.getProperty(type)
                .getValue();
        assert expressionConstantKey != null;

        assert constantPool.has(expressionConstantKey);

        JsArray<String> dataExpressions = constantPool
                .get(expressionConstantKey);

        JsonObject eventData = null;
        if (!dataExpressions.isEmpty()) {
            eventData = Json.createObject();

            for (int i = 0; i < dataExpressions.length(); i++) {
                String expressionString = dataExpressions.get(i);

                EventDataExpression expression = getOrCreateExpression(
                        expressionString);

                JsonValue expressionValue = expression.evaluate(event, element);

                eventData.put(expressionString, expressionValue);
            }
        }

        node.getTree().sendEventToServer(node, type, eventData);
    }

    private static EventDataExpression getOrCreateExpression(
            String expressionString) {
        EventDataExpression expression = expressionCache.get(expressionString);

        if (expression == null) {
            expression = NativeFunction.create("event", "element",
                    "return (" + expressionString + ")");
            expressionCache.set(expressionString, expression);
        }

        return expression;
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
