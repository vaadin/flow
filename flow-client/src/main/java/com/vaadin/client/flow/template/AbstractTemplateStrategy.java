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

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.binding.BinderContext;
import com.vaadin.client.flow.binding.BindingStrategy;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.model.BeanModelType;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.template.angular.ModelValueBindingProvider;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;

import elemental.dom.Node;
import elemental.json.JsonObject;

/**
 * Abstract binding strategy to handle template nodes.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            a DOM node type which strategy is applicable for
 */
public abstract class AbstractTemplateStrategy<T extends Node>
        implements BindingStrategy<T> {

    private static class TemplateBinderContextImpl
            implements TemplateBinderContext {

        private BinderContext original;

        private StateNode templateRoot;

        private TemplateBinderContextImpl(BinderContext original,
                StateNode templateRoot) {
            assert templateRoot.hasFeature(NodeFeatures.TEMPLATE);

            this.original = original;
            this.templateRoot = templateRoot;
        }

        @Override
        public Node createAndBind(StateNode node) {
            return original.createAndBind(node);
        }

        @Override
        public void bind(StateNode stateNode, Node node) {
            original.bind(stateNode, node);
        }

        @Override
        public <T extends BindingStrategy<?>> JsArray<T> getStrategies(
                Predicate<BindingStrategy<?>> predicate) {
            return original.getStrategies(predicate);
        }

        @Override
        public StateNode getTemplateRoot() {
            return templateRoot;
        }
    }

    @Override
    public boolean isApplicable(StateNode templateRoot) {
        assert templateRoot != null;

        boolean isTemplate = templateRoot.hasFeature(NodeFeatures.TEMPLATE);
        if (isTemplate) {
            return isApplicable(templateRoot.getTree(),
                    getTemplateId(templateRoot));
        }
        return false;
    }

    @Override
    public T create(StateNode templateRoot) {
        return create(templateRoot.getTree(), getTemplateId(templateRoot));
    }

    @Override
    public void bind(StateNode templateRoot, T htmlNode,
            BinderContext context) {
        bind(templateRoot, htmlNode, getTemplateId(templateRoot),
                new TemplateBinderContextImpl(context, templateRoot));
    }

    /**
     * Returns whether this strategy applies to the template node with the given
     * {@code templateId} inside the state {@code tree}.
     *
     * @param tree
     *            the state tree, not {@code null}
     * @param templateId
     *            the template id to check against
     * @return {@code true} if strategy is applicable
     */
    protected boolean isApplicable(StateTree tree, int templateId) {
        assert tree != null;
        TemplateNode templateNode = getTemplateNode(tree, templateId);
        return getTemplateType().equals(templateNode.getType());
    }

    /**
     * Gets template type which strategy handles.
     * <p>
     * Only one strategy may handle the given type.
     *
     * @return template type of the strategy
     */
    protected abstract String getTemplateType();

    /**
     * Creates a DOM node for the {@code templateId} using the {@code tree} as a
     * context.
     *
     * @param tree
     *            the state tree, not {@code null}
     * @param templateId
     *            the template id
     * @return the DOM node, not <code>null</code>
     */
    protected abstract T create(StateTree tree, int templateId);

    /**
     * Binds a DOM node to the {@code modelNode} using the {@code templateId}
     * and {@code context} to delegate handling of nodes with the types that the
     * strategy is not aware of.
     *
     * @param modelNode
     *            the state node containing model data to bind, not {@code null}
     * @param node
     *            the DOM node, not <code>null</code>
     * @param context
     *            delegation context
     * @param templateId
     *            the template id
     */
    protected abstract void bind(StateNode modelNode, T node, int templateId,
            TemplateBinderContext context);

    /**
     * Binds the {@code modelNode} using the given {@code binding} and
     * {@code executor} to set the {@code binding} data to the node.
     *
     * @param modelNode
     *            the state node containing model data, not {@code null}
     * @param binding
     *            binding data to set, not {@code null}
     * @param executor
     *            the operation to set the binding data to the node
     */
    protected void bind(StateNode modelNode, Binding binding,
            Consumer<Optional<Object>> executor) {
        if (ModelValueBindingProvider.TYPE.equals(binding.getType())) {
            Computation computation = Reactive
                    .runWhenDependenciesChange(() -> executor
                            .accept(getModelBindingValue(modelNode, binding)));
            modelNode.addUnregisterListener(event -> computation.stop());
        } else {
            // Only static bindings is known as a final call
            assert binding.getType().equals(StaticBindingValueProvider.TYPE);
            executor.accept(Optional.of(getStaticBindingValue(binding)));
        }
    }

    /**
     * Hooks up all {@code bindings} to run {@code executor} when the
     * {@code modelNode} changes in a way that affects the binding.
     *
     * @param modelNode
     *            the state node containing model data, not {@code null}
     * @param bindings
     *            binding data to set, not {@code null}
     * @param executor
     *            the operation to use the binding value for a named binding,
     *            not {@code null}
     */
    protected void bind(StateNode modelNode, JsonObject bindings,
            BiConsumer<String, Optional<Object>> executor) {
        if (bindings == null) {
            return;
        }

        for (String name : bindings.keys()) {
            Binding binding = WidgetUtil.crazyJsCast(bindings.get(name));
            bind(modelNode, binding, value -> executor.accept(name, value));
        }
    }

    /**
     * Gets the value from the {@code node} for the {@code binding}.
     *
     * @param node
     *            the state node, not {@code null}
     * @param binding
     *            binding data, not {@code null}
     * @return map binding value, or an empty optional if no value for the
     *         binding
     */
    private static Optional<Object> getModelBindingValue(StateNode node,
            Binding binding) {
        NodeMap model = node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        String key = binding.getValue();
        assert key != null;

        if (!node.hasFeature(NodeFeatures.TEMPLATE)) {
            /*
             * This is temporary legacy logic to support *ngFor bindings. JS
             * evaluation should be used in any case. But at the moment JS
             * evaluation doesn't work with *ngFor bindings so they are handled
             * here.
             *
             * TODO: remove this and update JS evaluation to support *ngFor.
             */
            String[] modelPathParts = key.split("\\.");
            // The last part is the propertyName
            for (int i = 0; i < modelPathParts.length - 1; i++) {
                StateNode n = (StateNode) model.getProperty(modelPathParts[i])
                        .getValue();
                model = n.getMap(NodeFeatures.TEMPLATE_MODELMAP);
            }
            key = modelPathParts[modelPathParts.length - 1];
            return Optional.ofNullable(model.getProperty(key).getValue());
        } else {
            String expression = key;

            String modelDescriptorId = (String) node
                    .getMap(NodeFeatures.TEMPLATE)
                    .getProperty(NodeProperties.MODEL_DESCRIPTOR).getValue();

            assert modelDescriptorId != null;

            JsonObject modelDescriptor = node.getTree().getRegistry()
                    .getConstantPool().get(modelDescriptorId);

            NativeFunction function = new NativeFunction("model",
                    "with(model) { return " + expression + "}");

            BeanModelType type = new BeanModelType(modelDescriptor);

            Object proxy = type.createProxy(model);

            return Optional.ofNullable(function.call(null, proxy));
        }
    }

    /**
     * Gets static biding value for the {@code binding}.
     *
     * @param binding
     *            binding data, not {@code null}
     * @return static binding value
     */
    protected String getStaticBindingValue(Binding binding) {
        assert binding != null;
        return Optional.ofNullable(binding.getValue()).orElse("");
    }

    /**
     * Gets template node from the template registry of the {@code tree} by the
     * {@code templateId}.
     *
     * @param tree
     *            state tree which owns the template registry
     * @param templateId
     *            template id
     * @return template node by the id
     */
    protected static TemplateNode getTemplateNode(StateTree tree,
            int templateId) {
        return tree.getRegistry().getTemplateRegistry().get(templateId);
    }

    /**
     * Creates and binds a DOM node for the given state node and
     * {@code templateId} using binder {@code context}.
     *
     * @param modelNode
     *            the state node with model data for which to create a DOM node,
     *            not <code>null</code>
     * @param templateId
     *            template id
     * @param context
     *            binder context
     *
     * @return the DOM node, not <code>null</code>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static Node createAndBind(StateNode modelNode, int templateId,
            TemplateBinderContext context) {
        JsArray<AbstractTemplateStrategy> strategies = context.getStrategies(
                strategy -> strategy instanceof AbstractTemplateStrategy<?>);
        StateTree tree = modelNode.getTree();
        for (int i = 0; i < strategies.length(); i++) {
            AbstractTemplateStrategy strategy = strategies.get(i);
            if (strategy.isApplicable(tree, templateId)) {
                Node domNode = strategy.create(tree, templateId);
                strategy.bind(modelNode, domNode, templateId, context);
                return domNode;
            }
        }
        throw new IllegalArgumentException("Unsupported template type: "
                + getTemplateNode(tree, templateId).getType());
    }

    private int getTemplateId(StateNode node) {
        return node.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeProperties.ROOT_TEMPLATE_ID)
                .getValueOrDefault(-1);
    }

}
