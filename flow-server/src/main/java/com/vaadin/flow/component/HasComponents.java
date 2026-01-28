/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.nodefeature.ChildrenBindingFeature;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.impl.Effect;
import com.vaadin.signals.shared.SharedListSignal;
import com.vaadin.signals.shared.SharedValueSignal;

/**
 * A component to which the user can add and remove child components.
 * {@link Component} in itself provides basic support for child components that
 * are manually added as children of an element belonging to the component. This
 * interface provides an explicit API for components that explicitly supports
 * adding and removing arbitrary child components.
 * <p>
 * {@link HasComponents} is generally implemented by layouts or components whose
 * primary function is to host child components. It isn't for example
 * implemented by non-layout components such as fields.
 * <p>
 * The default implementations assume that children are attached to
 * {@link #getElement()}. Override all methods in this interface if the
 * components should be added to some other element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasComponents extends HasElement, HasEnabled {
    /**
     * Adds the given components as children of this component.
     * <p>
     * In case any of the specified components has already been added to another
     * parent, it will be removed from there and added to this one.
     *
     * @param components
     *            the components to add
     */
    default void add(Component... components) {
        Objects.requireNonNull(components, "Components should not be null");
        add(Arrays.asList(components));
    }

    /**
     * Adds the given components as children of this component.
     * <p>
     * In case any of the specified components has already been added to another
     * parent, it will be removed from there and added to this one.
     *
     * @param components
     *            the components to add
     */
    default void add(Collection<Component> components) {
        Objects.requireNonNull(components, "Components should not be null");
        getElement().getNode()
                .getFeatureIfInitialized(ChildrenBindingFeature.class)
                .ifPresent(feature -> {
                    if (feature.hasBinding()) {
                        throw new BindingActiveException(
                                "add is not allowed while a binding for children exists.");
                    }
                });
        components.stream()
                .map(component -> Objects.requireNonNull(component,
                        "Component to add cannot be null"))
                .map(Component::getElement).forEach(getElement()::appendChild);
    }

    /**
     * Add the given text as a child of this component.
     *
     * @param text
     *            the text to add, not <code>null</code>
     */
    default void add(String text) {
        add(new Text(text));
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            the components to remove
     * @throws IllegalArgumentException
     *             if there is a component whose non {@code null} parent is not
     *             this component
     */
    default void remove(Component... components) {
        Objects.requireNonNull(components, "Components should not be null");
        remove(Arrays.asList(components));
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            the components to remove
     * @throws IllegalArgumentException
     *             if there is a component whose non {@code null} parent is not
     *             this component
     */
    default void remove(Collection<Component> components) {
        Objects.requireNonNull(components, "Components should not be null");
        getElement().getNode()
                .getFeatureIfInitialized(ChildrenBindingFeature.class)
                .ifPresent(feature -> {
                    if (feature.hasBinding()) {
                        throw new BindingActiveException(
                                "remove is not allowed while a binding for children exists.");
                    }
                });
        List<Component> toRemove = new ArrayList<>(components.size());
        for (Component component : components) {
            Objects.requireNonNull(component,
                    "Component to remove cannot be null");
            Element parent = component.getElement().getParent();
            if (parent == null) {
                LoggerFactory.getLogger(HasComponents.class).debug(
                        "Remove of a component with no parent does nothing.");
                continue;
            }
            if (getElement().equals(parent)) {
                toRemove.add(component);
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
        toRemove.stream().map(Component::getElement)
                .forEach(getElement()::removeChild);
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text content as well as child elements that have been added directly to
     * this component using the {@link Element} API. it also removes the
     * children that were added only at the client-side.
     */
    default void removeAll() {
        getElement().removeAllChildren();
    }

    /**
     * Adds the given component as child of this component at the specific
     * index.
     * <p>
     * In case the specified component has already been added to another parent,
     * it will be removed from there and added to this one.
     *
     * @param index
     *            the index, where the component will be added. The index must
     *            be non-negative and may not exceed the children count
     * @param component
     *            the component to add, value should not be null
     */
    default void addComponentAtIndex(int index, Component component) {
        Objects.requireNonNull(component, "Component should not be null");
        if (index < 0) {
            throw new IllegalArgumentException(
                    "Cannot add a component with a negative index");
        }
        // The case when the index is bigger than the children count is handled
        // inside the method below
        getElement().insertChild(index, component.getElement());
    }

    /**
     * Adds the given component as the first child of this component.
     * <p>
     * In case the specified component has already been added to another parent,
     * it will be removed from there and added to this one.
     *
     * @param component
     *            the component to add, value should not be null
     */
    default void addComponentAsFirst(Component component) {
        addComponentAtIndex(0, component);
    }

    /**
     * Binds a {@link SharedListSignal} to this component using a child
     * component factory. Each {@link SharedValueSignal} in the list corresponds
     * to a child component within this component.
     * <p>
     * This component is automatically updated to reflect the structure of the
     * {@link SharedListSignal}. Changes to the list, such as additions,
     * removals, or reordering, will update this component's children
     * accordingly.
     * <p>
     * This component must not contain any children that are not part of the
     * {@link SharedListSignal}. If this component has existing children when
     * this method is called, or if it contains unrelated children after the
     * list changes, an {@link IllegalStateException} will be thrown.
     * <p>
     * New child components are created using the provided
     * <code>childFactory</code> function. This function takes a
     * {@link SharedValueSignal} from the {@link SharedListSignal} and returns a
     * corresponding {@link Component}. It shouldn't return <code>null</code>.
     * The {@link SharedValueSignal} can be further bound to the returned
     * component as needed. Note that <code>childFactory</code> is run inside a
     * {@link Effect}, and therefore {@link SharedValueSignal#value()} calls
     * makes effect re-run automatically on signal value change.
     * <p>
     * Example of usage:
     *
     * <pre>
     * SharedListSignal&lt;String&gt; taskList = new SharedListSignal&lt;&gt;(String.class);
     *
     * UnorderedList component = new UnorderedList();
     *
     * component.bindChildren(taskList, ListItem::new);
     * </pre>
     *
     * @param list
     *            list signal to bind to the parent, must not be
     *            <code>null</code>
     * @param childFactory
     *            factory to create new component, must not be <code>null</code>
     * @param <T>
     *            the value type of the {@link SharedValueSignal}s in the
     *            {@link SharedListSignal}
     * @return a registration that can be used to remove the binding
     * @throws IllegalStateException
     *             thrown if this component isn't empty
     * @throws BindingActiveException
     *             thrown if a binding for children already exists
     */
    default <T> Registration bindChildren(SharedListSignal<T> list,
            SerializableFunction<SharedValueSignal<T>, Component> childFactory) {
        Objects.requireNonNull(list, "ListSignal cannot be null");
        Objects.requireNonNull(childFactory,
                "Child element factory cannot be null");
        var self = (Component & HasComponents) this;
        var node = self.getElement().getNode();
        var feature = node.getFeature(ChildrenBindingFeature.class);
        if (feature.hasBinding() && node.isAttached()) {
            throw new BindingActiveException();
        }
        var binding = ComponentEffect.bindChildren(self, list, childFactory);
        feature.setBinding(binding, list);
        return feature::removeBinding;
    }
}
