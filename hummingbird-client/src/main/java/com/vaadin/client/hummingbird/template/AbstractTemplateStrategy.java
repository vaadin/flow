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

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.binding.BinderContext;
import com.vaadin.client.hummingbird.binding.BindingStrategy;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeMap;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;

import elemental.dom.Node;

/**
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractTemplateStrategy<T extends Node>
        implements BindingStrategy<T> {

    @Override
    public boolean isAppliable(StateNode node) {
        assert node != null;

        boolean isTempalate = node.hasFeature(NodeFeatures.TEMPLATE);
        if (isTempalate) {
            return isApplicable(node.getTree(), getTemplateId(node));
        }
        return false;
    }

    @Override
    public T create(StateNode node) {
        return create(node.getTree(), getTemplateId(node));
    }

    @Override
    public void bind(StateNode stateNode, T htmlNode, BinderContext context) {
        bind(stateNode, htmlNode, getTemplateId(stateNode), context);
    }

    protected boolean isApplicable(StateTree tree, int templateId) {
        TemplateNode templateNode = getTemplateNode(tree, templateId);
        return getTemplateType().equals(templateNode.getType());
    }

    protected abstract String getTemplateType();

    protected abstract T create(StateTree tree, int templateId);

    protected abstract void bind(StateNode stateNode, T node, int templateId,
            BinderContext context);

    protected void bind(StateNode stateNode, Binding binding,
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

    protected MapProperty getModelProperty(StateNode node, Binding binding) {
        NodeMap model = node.getMap(NodeFeatures.TEMPLATE_MODELMAP);
        String key = binding.getValue();
        assert key != null;
        return model.getProperty(key);
    }

    protected String getStaticBindingValue(Binding binding) {
        assert binding != null;
        return Optional.ofNullable(binding.getValue()).orElse("");
    }

    protected static TemplateNode getTemplateNode(StateTree tree,
            int templateId) {
        return tree.getRegistry().getTemplateRegistry().get(templateId);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static Node createAndBind(StateNode node, int templateId,
            BinderContext context) {
        JsArray<AbstractTemplateStrategy> strategies = context.getStrategies(
                strategy -> strategy instanceof AbstractTemplateStrategy<?>);
        StateTree tree = node.getTree();
        for (int i = 0; i < strategies.length(); i++) {
            AbstractTemplateStrategy strategy = strategies.get(i);
            if (strategy.isApplicable(tree, templateId)) {
                Node htmlNode = strategy.create(tree, templateId);
                strategy.bind(node, htmlNode, templateId, context);
                return htmlNode;
            }
        }
        throw new IllegalArgumentException("Unsupported template type: "
                + getTemplateNode(tree, templateId).getType());
    }

    private int getTemplateId(StateNode node) {
        return node.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeFeatures.ROOT_TEMPLATE_ID)
                .getValueOrDefault(-1);
    }

}
