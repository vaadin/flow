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
package com.vaadin.flow.component;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component.MapToExistingElement;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.internal.ComponentMetaData;
import com.vaadin.flow.component.internal.ComponentMetaData.DependencyInfo;
import com.vaadin.flow.component.internal.ComponentMetaData.SynchronizedPropertyInfo;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableTriConsumer;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.Attributes;
import com.vaadin.flow.server.VaadinService;

/**
 * Utility methods for {@link Component}.
 *
 * @author Vaadin Ltd
 */
public class ComponentUtil {

    static ReflectionCache<Component, ComponentMetaData> componentMetaDataCache = new ReflectionCache<>(
            ComponentMetaData::new);

    private ComponentUtil() {
        // Util methods only
    }

    /**
     * Finds the first component instance in each {@link Element} subtree by
     * traversing the {@link Element} tree starting from the given element.
     *
     * @param element
     *            the element to start scanning from
     * @param componentConsumer
     *            a consumer which is called for each found component
     */
    public static void findComponents(Element element,
            Consumer<Component> componentConsumer) {
        assert element != null;
        assert componentConsumer != null;

        Optional<Component> maybeComponent = element.getComponent();

        if (maybeComponent.isPresent()) {
            Component component = maybeComponent.get();
            componentConsumer.accept(component);
            return;
        }

        element.getChildren().forEach(childElement -> ComponentUtil
                .findComponents(childElement, componentConsumer));
    }

    /**
     * Gets the parent of the given component, which is inside the given
     * composite.
     * <p>
     * This method is meant for internal use only.
     *
     * @param composite
     *            the composite containing the component
     * @param component
     *            the component to get the parent for, must be inside the
     *            composite or a nested composite
     * @return the parent of the component, never <code>null</code>
     */
    public static Component getParentUsingComposite(Composite<?> composite,
            Component component) {
        // If this is the component inside a composite or a nested
        // composite, we need to traverse the composite hierarchy to find
        // the parent
        Composite<?> compositeAncestor = composite;
        while (true) {
            Component compositeChild = compositeAncestor.getContent();
            if (compositeChild == component) {
                return compositeAncestor;
            }
            compositeAncestor = (Composite<?>) compositeAncestor.getContent();
        }

    }

    /**
     * Returns the innermost component from a {@link Composite} chain, i.e. the
     * first content which is not a {@link Composite}.
     *
     * @param composite
     *            a composite in the chain
     * @return the innermost component
     */
    public static Component getInnermostComponent(Composite<?> composite) {
        Component content = composite.getContent();
        while (content instanceof Composite) {
            content = ((Composite<?>) content).getContent();
        }
        return content;
    }

    /**
     * Checks if the given component is inside a {@link Composite} chain, i.e.
     * it is a composite in the chain or the content of the innermost
     * {@link Composite}.
     *
     * @param composite
     *            the first composite
     * @param component
     *            the component to look for
     * @return <code>true</code> if the component is inside the composite chain,
     *         <code>false</code> otherwise
     */
    public static boolean isCompositeContent(Composite<?> composite,
            Component component) {
        Component compositeContent = composite.getContent();
        if (compositeContent == component) {
            return true;
        } else if (compositeContent instanceof Composite) {
            // Nested composites
            return isCompositeContent((Composite<?>) compositeContent,
                    component);
        } else {
            return false;
        }
    }

    /**
     * Finds the first component by traversing upwards in the element hierarchy,
     * starting from the given element.
     *
     * @param element
     *            the element from which to begin the search
     * @return optional of the component, empty if no component is found
     */
    public static Optional<Component> findParentComponent(Element element) {
        Element mappedElement = element;
        while (mappedElement != null
                && !mappedElement.getComponent().isPresent()) {
            mappedElement = mappedElement.getParent();
        }
        if (mappedElement == null) {
            return Optional.empty();
        }

        return Optional.of(getInnermostComponent(mappedElement));
    }

    /**
     * Gets the innermost mapped component for the element.
     * <p>
     * This returns {@link Element#getComponent()} if something else than a
     * {@link Composite} is mapped to the element. If a {@link Composite} is
     * mapped to the element, finds the innermost content of the
     * {@link Composite} chain.
     *
     * @param element
     *            the element which is mapped to a component
     * @return the innermost component mapped to the element
     */
    public static Component getInnermostComponent(Element element) {
        assert element.getComponent().isPresent();

        Component component = element.getComponent().get();
        if (component instanceof Composite) {
            return ComponentUtil
                    .getInnermostComponent((Composite<?>) component);
        }

        return component;
    }

    /**
     * Handles triggering the {@link Component#onAttach(AttachEvent) onAttach}
     * method and firing the {@link AttachEvent} for the given component when it
     * has been attached to a UI.
     *
     * @param component
     *            the component attached to a UI
     * @param initialAttach
     *            indicates if this is the first time the component (element)
     *            has been attached
     */
    public static void onComponentAttach(Component component,
            boolean initialAttach) {
        if (component instanceof Composite) {
            onComponentAttach(((Composite<?>) component).getContent(),
                    initialAttach);
        }

        Optional<UI> ui = component.getUI();
        if (ui.isPresent() && component instanceof LocaleChangeObserver) {
            LocaleChangeEvent localeChangeEvent = new LocaleChangeEvent(
                    ui.get(), ui.get().getLocale());
            ((LocaleChangeObserver) component).localeChange(localeChangeEvent);
        }

        AttachEvent attachEvent = new AttachEvent(component, initialAttach);
        component.onAttach(attachEvent);
        if (component.hasListener(AttachEvent.class)) {
            component.getEventBus().fireEvent(attachEvent);
        }
        // inform component about onEnabledState if new state differs from
        // internal state
        if (component instanceof HasEnabled
                && component.getElement().isEnabled() != component.getElement()
                        .getNode().isEnabledSelf()) {

            Optional<Component> parent = component.getParent();
            if (parent.isPresent()
                    && isAttachedToParent(component, parent.get())) {
                component.onEnabledStateChanged(
                        component.getElement().isEnabled());
            }
        }
    }

    /**
     * Handles triggering the {@link Component#onDetach(DetachEvent) onDetach}
     * method and firing the {@link DetachEvent} for the given component right
     * before it is detached from a UI.
     *
     * @param component
     *            the component detached from a UI
     */
    public static void onComponentDetach(Component component) {
        if (component instanceof Composite) {
            onComponentDetach(((Composite<?>) component).getContent());
        }

        DetachEvent detachEvent = new DetachEvent(component);
        component.onDetach(detachEvent);
        if (component.hasListener(DetachEvent.class)) {
            component.getEventBus().fireEvent(detachEvent);
        }
        // inform component about onEnabledState if parent and child states
        // differ.
        if (component instanceof HasEnabled
                && component.getElement().isEnabled() != component.getElement()
                        .getNode().isEnabledSelf()) {
            Optional<Component> parent = component.getParent();
            if (parent.isPresent()) {
                Component parentComponent = parent.get();
                boolean state = isAttachedToParent(component, parentComponent)
                        ? checkParentChainState(parentComponent)
                        : component.getElement().getNode().isEnabledSelf();
                component.onEnabledStateChanged(state);
            } else {
                component.onEnabledStateChanged(
                        component.getElement().isEnabled());
            }
        }
    }

    private static boolean isAttachedToParent(Component component,
            Component parentComponent) {
        return parentComponent.getChildren()
                .anyMatch(child -> child.equals(component))
                || isVirtualChild(component, parentComponent);
    }

    private static boolean isVirtualChild(Component component,
            Component parentComponent) {
        Iterator<StateNode> iterator = parentComponent.getElement().getNode()
                .getFeature(VirtualChildrenList.class).iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(component.getElement().getNode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkParentChainState(Component component) {
        if (!component.getElement().getNode().isEnabledSelf()) {
            return false;
        }

        Optional<Component> parent = component.getParent();
        if (parent.isPresent()) {
            Component parentComponent = parent.get();
            if (isAttachedToParent(component, parentComponent)) {
                return checkParentChainState(parentComponent);
            }
        }

        return true;
    }

    /**
     * Creates a new component instance using the given element, maps the
     * component to the element and optionally maps the element to the component
     * (if <code>mapComponent</code> is <code>true</code>).
     * <p>
     * This is a helper method for Element#as and Component#from.
     *
     * @see Component#from(Element, Class)
     * @see Element#as(Class)
     *
     * @param <T>
     *            the component type
     * @param element
     *            the element
     * @param componentType
     *            the component type
     * @param mapComponent
     *            <code>true</code> to also map the element to the component,
     *            <code>false</code> to only map the component to the element
     * @return a new component instance of the given type
     */
    public static <T extends Component> T componentFromElement(Element element,
            Class<T> componentType, boolean mapComponent) {
        if (element == null) {
            throw new IllegalArgumentException("Element to use cannot be null");
        }
        if (componentType == null) {
            throw new IllegalArgumentException("Component type cannot be null");
        }
        MapToExistingElement wrapData = new MapToExistingElement(element,
                mapComponent);

        try {
            Component.elementToMapTo.set(wrapData);
            UI ui = UI.getCurrent();
            if (ui == null) {
                throw new IllegalStateException("UI instance is not available. "
                        + "It looks like you are trying to execute UI code outside the UI/Servlet dispatching thread");
            }
            Instantiator instantiator = Instantiator.get(ui);
            return instantiator.createComponent(componentType);
        } finally {
            // This should always be cleared, normally it's cleared in Component
            // but in case of exception it might be not cleared
            Component.elementToMapTo.remove();
        }

    }

    /**
     * Gets the synchronized property infos of the properties that are defined
     * declaratively for the given class with their RPC update mode.
     *
     * @param componentClass
     *            the component class to check
     * @return the synchronized property infos of the properties defined
     *         declaratively for the class
     */
    public static Collection<SynchronizedPropertyInfo> getSynchronizedProperties(
            Class<? extends Component> componentClass) {
        return componentMetaDataCache.get(componentClass)
                .getSynchronizedProperties();
    }

    /**
     * Gets the name of the synchronized property event defined declaratively
     * for the given class.
     *
     * @param componentClass
     *            the component class to check
     * @return the synchronized property events defined declaratively for the
     *         class
     */
    public static Stream<String> getSynchronizedPropertyEvents(
            Class<? extends Component> componentClass) {
        Collection<SynchronizedPropertyInfo> infos = componentMetaDataCache
                .get(componentClass).getSynchronizedProperties();
        return infos.stream().flatMap(SynchronizedPropertyInfo::getEventNames)
                .distinct();
    }

    /**
     * Gets the dependencies for the given class, defined using annotations (
     * {@link HtmlImport}, {@link JavaScript}, {@link StyleSheet} and
     * {@link Uses}).
     *
     * @param service
     *            the service to use for resolving dependencies
     * @param componentClass
     *            the component class to check
     * @return the dependencies for the given class
     */
    public static DependencyInfo getDependencies(VaadinService service,
            Class<? extends Component> componentClass) {
        return componentMetaDataCache.get(componentClass)
                .getDependencyInfo(service);
    }

    private static <T, U> void writeAttribute(Component component,
            SerializableTriConsumer<Attributes, T, U> setter, T key, U value) {
        Attributes attributes = component.attributes;
        if (attributes == null) {
            if (value == null) {
                return;
            }
            attributes = new Attributes();
            component.attributes = attributes;
        }

        setter.accept(attributes, key, value);

        if (attributes.isEmpty()) {
            component.attributes = null;
        }
    }

    /**
     * Stores an attribute value for the given component.
     *
     * @see #setAttribute(Component, Class, Object)
     * @see #getAttribute(Component, String)
     *
     * @param component
     *            the component for which to set an attribute
     * @param name
     *            the name of the attribute to set
     * @param value
     *            the attribute value to set, or <code>null</code> to remove the
     *            attribute
     */
    public static void setAttribute(Component component, String name,
            Object value) {
        writeAttribute(component, Attributes::setAttribute, name, value);
    }

    /**
     * Stores a typed instance as an attribute for the given component.
     *
     * @see #setAttribute(Component, String, Object)
     * @see #getAttribute(Component, Class)
     *
     * @param component
     *            the component for which to set an attribute
     * @param type
     *            the type of the attribute to set
     * @param value
     *            the attribute value to set, or <code>null</code> to remove the
     *            attribute
     */
    public static <T> void setAttribute(Component component, Class<T> type,
            T value) {
        writeAttribute(component, Attributes::setAttribute, type, value);
    }

    private static <T, U> U getAttribute(Component component,
            BiFunction<Attributes, T, U> getter, T key) {
        Attributes attributes = component.attributes;
        if (attributes == null) {
            return null;
        }
        return getter.apply(attributes, key);
    }

    /**
     * Gets an attribute with the given name, or <code>null</code> if there is
     * no such attribute.
     *
     * @see #setAttribute(Component, String, Object)
     *
     * @param component
     *            the component from which to get the attribute
     * @param name
     *            the attribute name
     * @return the attribute value, or <code>null</code> if there is no
     *         attribute
     */
    public static Object getAttribute(Component component, String name) {
        return getAttribute(component, Attributes::getAttribute, name);
    }

    /**
     * Gets an attribute with the given type, or <code>null</code> if there is
     * no such attribute.
     *
     * @see #setAttribute(Component, Class, Object)
     *
     * @param component
     *            the component from which to get the attribute
     * @param type
     *            the attribute type
     * @return the attribute value, or <code>null</code> if there is no
     *         attribute
     */
    public static <T> T getAttribute(Component component, Class<T> type) {
        return getAttribute(component,
                (attributes, ignore) -> attributes.getAttribute(type), type);
    }

}
