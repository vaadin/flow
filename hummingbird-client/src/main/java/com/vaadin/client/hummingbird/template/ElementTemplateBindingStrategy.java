/*
 * Copyright 2000-2016 Vaadin Ltd.
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
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.binding.BinderContext;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;

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
        void handle(Event event, JavaScriptObject serverProxy);
    }

    @Override
    protected String getTemplateType() {
        return com.vaadin.hummingbird.template.ElementTemplateNode.TYPE;
    }

    @Override
    protected Element create(StateTree tree, int templateId) {
        String tag = ((ElementTemplateNode) getTemplateNode(tree, templateId))
                .getTag();
        return Browser.getDocument().createElement(tag);
    }

    @Override
    protected void bind(StateNode stateNode, Element element, int templateId,
            BinderContext context) {
        ElementTemplateNode templateNode = (ElementTemplateNode) getTemplateNode(
                stateNode.getTree(), templateId);
        bindProperties(stateNode, templateNode, element);

        JsonObject attributes = templateNode.getAttributes();
        if (attributes != null) {
            for (String name : attributes.keys()) {
                Binding binding = WidgetUtil.crazyJsCast(attributes.get(name));
                // Nothing to "bind" yet with only static bindings
                assert binding.getType()
                        .equals(StaticBindingValueProvider.TYPE);
                element.setAttribute(name, getStaticBindingValue(binding));
            }
        }

        JsArray<Double> children = templateNode.getChildren();
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                int childTemplateId = children.get(i).intValue();

                Node child = createAndBind(stateNode, childTemplateId, context);

                element.appendChild(child);
            }
        }

        registerEventHandlers(stateNode, templateNode, element);

        MapProperty overrideProperty = stateNode
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
            EventRemover remover = overrideProperty.addChangeListener(
                    e -> bindOverrideNode(element, overrideProperty, context));

            /*
             * Should preferably remove the change listener immediately when the
             * first event is fired, but Java makes it so difficult to reference
             * the remover from inside the event handler.
             */
            stateNode.addUnregisterListener(e -> remover.remove());
        }
    }

    private void registerEventHandlers(StateNode stateNode,
            ElementTemplateNode templateNode, Element element) {
        JsonObject eventHandlers = templateNode.getEventHandlers();
        if (eventHandlers != null) {
            for (String event : eventHandlers.keys()) {
                String handler = WidgetUtil
                        .crazyJsCast(eventHandlers.get(event));
                EventHandler eventHandler = NativeFunction.create("$event",
                        "$server", handler);
                element.addEventListener(event, evt -> eventHandler.handle(evt,
                        createServerProxy(stateNode)));
            }
        }
    }

    private void bindProperties(StateNode stateNode,
            ElementTemplateNode templateNode, Element element) {
        JsonObject properties = templateNode.getProperties();
        if (properties != null) {
            for (String name : properties.keys()) {
                Binding binding = WidgetUtil.crazyJsCast(properties.get(name));
                bind(stateNode, binding, value -> WidgetUtil
                        .setJsProperty(element, name, value.orElse(null)));
            }
        }
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

    private JavaScriptObject createServerProxy(StateNode node) {
        JavaScriptObject proxy = JavaScriptObject.createObject();

        if (node.hasFeature(NodeFeatures.TEMPLATE_METADATA)) {
            NodeList list = node.getList(NodeFeatures.TEMPLATE_METADATA);
            for (int i = 0; i < list.length(); i++) {
                attachServerProxyMethod(proxy, node, list.get(i).toString());
            }
        }
        return proxy;
    }

    private static native void attachServerProxyMethod(JavaScriptObject proxy,
            StateNode node, String methodName)
    /*-{
        proxy[methodName] = function() {
            var tree = node.@com.vaadin.client.hummingbird.StateNode::getTree()();
            tree.@com.vaadin.client.hummingbird.StateTree::requestCallServerMethod(*)(node, methodName);
        };
    }-*/;

}