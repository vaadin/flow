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

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.BasicElementBinder;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.StaticBinding;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.EventRemover;
import elemental.json.JsonObject;

/**
 * Binds a template node and a state node to an element instance.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TemplateElementBinder {

    private TemplateElementBinder() {
        // Static methods only
    }

    /**
     * Creates and binds a DOM element based on the root template for the given
     * state node.
     *
     * @param stateNode
     *            the state node to bind to, not <code>null</code>
     * @return the created and bound DOM node
     */
    public static Node createAndBind(StateNode stateNode) {
        assert stateNode != null;

        assert stateNode.hasFeature(NodeFeatures.TEMPLATE);

        int templateId = stateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeFeatures.ROOT_TEMPLATE_ID)
                .getValueOrDefault(-1);

        return createAndBind(stateNode, templateId);
    }

    private static Node createAndBind(StateNode stateNode, int templateId) {
        assert templateId != -1;

        TemplateNode templateNode = stateNode.getTree().getRegistry()
                .getTemplateRegistry().get(templateId);

        return createAndBind(stateNode, templateNode);
    }

    /**
     * Creates and binds a DOM element based on the given state node and
     * template.
     *
     * @param stateNode
     *            the state node to bind to, not <code>null</code>
     * @param templateNode
     *            the template to use for binding, not <code>null</code>
     * @return the created and bound DOM node
     */
    public static Node createAndBind(StateNode stateNode,
            TemplateNode templateNode) {
        assert stateNode != null;
        assert templateNode != null;

        switch (templateNode.getType()) {
        case com.vaadin.hummingbird.template.ElementTemplateNode.TYPE:
            return createAndBindElement(stateNode,
                    (ElementTemplateNode) templateNode);
        case com.vaadin.hummingbird.template.TextTemplateNode.TYPE:
            return createAndBindText(stateNode,
                    (TextTemplateNode) templateNode);
        default:
            throw new IllegalArgumentException(
                    "Unsupported template type: " + templateNode.getType());
        }
    }

    private static Node createAndBindText(StateNode stateNode,
            TextTemplateNode templateNode) {
        String text = getBindingValue(templateNode.getTextBinding());

        return Browser.getDocument().createTextNode(text);
    }

    private static Node createAndBindElement(StateNode stateNode,
            ElementTemplateNode templateNode) {
        String tag = templateNode.getTag();
        Element element = Browser.getDocument().createElement(tag);

        JsonObject properties = templateNode.getProperties();
        if (properties != null) {
            for (String name : properties.keys()) {
                Binding binding = WidgetUtil.crazyJsCast(properties.get(name));
                WidgetUtil.setJsProperty(element, name,
                        getBindingValue(binding));
            }
        }

        JsArray<Double> children = templateNode.getChildren();
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                int childTemplateId = children.get(i).intValue();

                Node child = createAndBind(stateNode, childTemplateId);

                element.appendChild(child);
            }
        }

        MapProperty overrideProperty = stateNode
                .getMap(NodeFeatures.TEMPLATE_OVERRIDES)
                .getProperty(String.valueOf(templateNode.getId()));
        if (overrideProperty.hasValue()) {
            /*
             * Bind right away. We don't need to listen to property value
             * changes since the value will never change once it has been set.
             */
            bindOverrideNode(element, overrideProperty);
        } else {
            /*
             * React in case an override nodes appears later on.
             */
            EventRemover remover = overrideProperty.addChangeListener(
                    e -> bindOverrideNode(element, overrideProperty));
            stateNode.addUnregisterListener(e -> remover.remove());
        }

        return element;
    }

    private static void bindOverrideNode(Element element,
            MapProperty overrideProperty) {
        StateNode overrideNode = (StateNode) overrideProperty.getValue();

        BasicElementBinder bind = BasicElementBinder.bind(overrideNode,
                element);

        overrideNode.addUnregisterListener(e -> bind.remove());
    }

    private static String getBindingValue(Binding binding) {
        assert binding != null;

        // Nothing to "bind" yet with only static bindings
        assert binding.getType().equals(StaticBinding.TYPE);

        Object value = binding.getValue();
        if (value == null) {
            return "";
        } else {
            return String.valueOf(value);
        }
    }

}
