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
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.hummingbird.shared.Namespaces;
import com.vaadin.hummingbird.template.ElementTemplateNode;
import com.vaadin.hummingbird.template.StaticBinding;
import com.vaadin.hummingbird.template.TextTemplateNode;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
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

        assert stateNode.hasNamespace(Namespaces.TEMPLATE);

        int templateId = stateNode.getMapNamespace(Namespaces.TEMPLATE)
                .getProperty(Namespaces.ROOT_TEMPLATE_ID).getValueOrDefault(-1);

        return createAndBind(stateNode, templateId);
    }

    private static Node createAndBind(StateNode stateNode, int templateId) {
        assert templateId != -1;

        Template template = stateNode.getTree().getRegistry().getTemplates()
                .getTemplate(templateId);

        return createAndBind(stateNode, template);
    }

    /**
     * Creates and binds a DOM element based on the given state node and
     * template.
     *
     * @param stateNode
     *            the state node to bind to, not <code>null</code>
     * @param template
     *            the template to use for binding, not <code>null</code>
     * @return the created and bound DOM node
     */
    public static Node createAndBind(StateNode stateNode, Template template) {
        assert stateNode != null;
        assert template != null;

        switch (template.getType()) {
        case ElementTemplateNode.TYPE:
            return createAndBindElement(stateNode, (ElementTemplate) template);
        case TextTemplateNode.TYPE:
            return createAndBindText(stateNode, (TextTemplate) template);
        default:
            throw new IllegalArgumentException(
                    "Unsupported template type: " + template.getType());
        }
    }

    private static Node createAndBindText(StateNode stateNode,
            TextTemplate template) {
        String text = getBindingValue(template.getTextBinding());

        return Browser.getDocument().createTextNode(text);
    }

    private static Node createAndBindElement(StateNode stateNode,
            ElementTemplate template) {
        String tag = template.getTag();
        Element element = Browser.getDocument().createElement(tag);

        JsonObject attributes = template.getAttributes();
        if (attributes != null) {
            for (String name : attributes.keys()) {
                Binding binding = WidgetUtil.crazyJsCast(attributes.get(name));
                element.setAttribute(name, getBindingValue(binding));
            }
        }

        JsArray<Double> children = template.getChildren();
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                int childTemplateId = children.get(i).intValue();

                Node child = createAndBind(stateNode, childTemplateId);

                element.appendChild(child);
            }
        }

        return element;
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
