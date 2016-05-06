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

import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.client.Command;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.BasicElementBinder;
import com.vaadin.client.hummingbird.ElementBinder;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeMap;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;

import elemental.client.Browser;
import elemental.dom.Comment;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.Text;
import elemental.events.EventRemover;
import elemental.json.JsonObject;

/**
 * Binds a template node and a state node to an element instance.
 *
 * @author Vaadin Ltd
 */
public class TemplateElementBinder {

    private static final class ChildSlotBinder implements Command {
        private final Comment anchor;
        private final StateNode stateNode;

        private StateNode childNode;

        public ChildSlotBinder(Comment anchor, StateNode stateNode) {
            this.anchor = anchor;
            this.stateNode = stateNode;
        }

        @Override
        public void execute() {
            final StateNode oldChildNode = childNode;
            final StateNode newChildNode = (StateNode) stateNode
                    .getMap(NodeFeatures.TEMPLATE)
                    .getProperty(TemplateMap.CHILD_SLOT_CONTENT).getValue();
            childNode = newChildNode;

            // Do the actual work separately so that this
            // computation doesn't depend on the values used for
            // binding
            Reactive.addFlushListener(
                    () -> updateChildSlot(oldChildNode, newChildNode));
        }

        private void updateChildSlot(StateNode oldChildNode,
                StateNode newChildNode) {
            Element parent = anchor.getParentElement();

            if (oldChildNode != null) {
                Node oldChild = oldChildNode.getDomNode();
                assert oldChild.getParentElement() == parent;

                parent.removeChild(oldChild);
            }

            if (newChildNode != null) {
                Node newChild = ElementBinder.createAndBind(newChildNode);

                parent.insertBefore(newChild, anchor.getNextSibling());
            }
        }
    }

    private static final class ForTemplateNodeUpdate implements Command {

        private Comment anchor;
        private Node beforeNode;
        private final StateNode stateNode;
        private final ForTemplateNode templateNode;

        ForTemplateNodeUpdate(Comment anchor, StateNode stateNode,
                ForTemplateNode templateNode) {
            this.anchor = anchor;
            this.stateNode = stateNode;
            this.templateNode = templateNode;
        }

        @Override
        public void execute() {
            Element parent = anchor.getParentElement();
            if (beforeNode == null) {
                beforeNode = anchor.getNextSibling();
            }

            JsArray<Double> children = templateNode.getChildren();
            assert children.length() == 1;
            int childId = children.get(0).intValue();

            Node htmlNode = anchor.getNextSibling();
            while (htmlNode != beforeNode) {
                parent.removeChild(htmlNode);
                htmlNode = anchor.getNextSibling();
            }
            NodeMap model = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
            MapProperty property = model
                    .getProperty(templateNode.getCollectionVariable());
            if (property.getValue() != null) {
                StateNode node = (StateNode) property.getValue();
                EventRemover remover = ElementBinder.bindChildren(parent, node,
                        NodeFeatures.TEMPLATE_MODELLIST,
                        childNode -> createAndBind(childNode, childId),
                        beforeNode);
                node.addUnregisterListener(event -> remover.remove());
            }
        }
    }

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
        case com.vaadin.hummingbird.template.ForTemplateNode.TYPE:
            return createAndBindNgFor(stateNode,
                    (ForTemplateNode) templateNode);
        case com.vaadin.hummingbird.template.TextTemplateNode.TYPE:
            return createAndBindText(stateNode,
                    (TextTemplateNode) templateNode);
        case com.vaadin.hummingbird.template.ChildSlotNode.TYPE:
            return createAndBindChildSot(stateNode);
        default:
            throw new IllegalArgumentException(
                    "Unsupported template type: " + templateNode.getType());
        }
    }

    private static Node createAndBindChildSot(StateNode stateNode) {
        // Anchor to put in the DOM to know where to insert the actual content
        Comment anchor = Browser.getDocument().createComment(" @child@ ");

        Computation computation = Reactive.runWhenDepedenciesChange(
                new ChildSlotBinder(anchor, stateNode));
        stateNode.addUnregisterListener(e -> computation.stop());

        return anchor;
    }

    private static Node createAndBindText(StateNode stateNode,
            TextTemplateNode templateNode) {
        Binding binding = templateNode.getTextBinding();
        Text node = Browser.getDocument().createTextNode("");
        bind(stateNode, binding, value -> node
                .setTextContent(value.map(Object::toString).orElse("")));
        return node;
    }

    private static MapProperty getModelProperty(StateNode node,
            Binding binding) {
        NodeMap model = node.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        String key = binding.getValue();
        assert key != null;
        return model.getProperty(key);
    }

    private static Node createAndBindNgFor(StateNode stateNode,
            ForTemplateNode templateNode) {
        Comment anchor = Browser.getDocument().createComment(" *ngFor ");

        Computation computation = Reactive.runWhenDepedenciesChange(
                new ForTemplateNodeUpdate(anchor, stateNode, templateNode));
        stateNode.addUnregisterListener(event -> computation.stop());

        return anchor;
    }

    private static Node createAndBindElement(StateNode stateNode,
            ElementTemplateNode templateNode) {
        String tag = templateNode.getTag();
        Element element = Browser.getDocument().createElement(tag);

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

            /*
             * Should preferably remove the change listener immediately when the
             * first event is fired, but Java makes it so difficult to reference
             * the remover from inside the event handler.
             */
            stateNode.addUnregisterListener(e -> remover.remove());
        }

        return element;
    }

    private static void bindProperties(StateNode stateNode,
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

    private static void bind(StateNode stateNode, Binding binding,
            Consumer<Optional<Object>> setOperation) {
        if (ModelValueBindingProvider.TYPE.equals(binding.getType())) {
            Computation computation = Reactive.runWhenDepedenciesChange(
                    () -> setOperation.accept(Optional.ofNullable(
                            getModelProperty(stateNode, binding).getValue())));
            stateNode.addUnregisterListener(event -> computation.stop());
        } else {
            // Only static bindings is known as a final call
            assert binding.getType().equals(StaticBindingValueProvider.TYPE);
            setOperation.accept(Optional.of(getStaticBindingValue(binding)));
        }
    }

    private static void bindOverrideNode(Element element,
            MapProperty overrideProperty) {
        StateNode overrideNode = (StateNode) overrideProperty.getValue();

        /*
         * bind checks that the we haven't already bound the same state node
         * previously
         */
        BasicElementBinder bind = BasicElementBinder.bind(overrideNode,
                element);

        overrideNode.addUnregisterListener(e -> bind.remove());
    }

    private static String getStaticBindingValue(Binding binding) {
        assert binding != null;
        return Optional.ofNullable(binding.getValue()).orElse("");
    }

}
