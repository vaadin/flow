/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.Collections;
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
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableTriConsumer;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.Attributes;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

/**
 * Utility methods for {@link Component}.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        fireEvent(component, attachEvent);

        // inform component about onEnabledState if new state differs from
        // internal state
        if (component instanceof HasEnabled
                && component.getElement().isEnabled() != component.getElement()
                        .getNode().isEnabledSelf()) {

            Element parent = component.getElement().getParent();
            if (parent != null
                    && isAttachedToParent(component.getElement(), parent)) {
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
        fireEvent(component, detachEvent);

        // inform component about onEnabledState if parent and child states
        // differ.
        if (component instanceof HasEnabled
                && component.getElement().isEnabled() != component.getElement()
                        .getNode().isEnabledSelf()) {
            Element parent = component.getElement().getParent();
            if (parent != null) {
                boolean state = isAttachedToParent(component.getElement(),
                        parent) ? checkParentChainState(parent)
                                : component.getElement().getNode()
                                        .isEnabledSelf();
                component.onEnabledStateChanged(state);
            } else {
                component.onEnabledStateChanged(
                        component.getElement().isEnabled());
            }
        }
    }

    private static boolean isAttachedToParent(Element element, Element parent) {
        return parent.getChildren().anyMatch(child -> child.equals(element))
                || isVirtualChild(element, parent);
    }

    private static boolean isVirtualChild(Element element, Element parent) {
        Iterator<StateNode> iterator = parent.getNode()
                .getFeatureIfInitialized(VirtualChildrenList.class)
                .map(VirtualChildrenList::iterator)
                .orElse(Collections.emptyIterator());
        while (iterator.hasNext()) {
            if (iterator.next().equals(element.getNode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkParentChainState(Element element) {
        if (!element.getNode().isEnabledSelf()) {
            return false;
        }

        Element parent = element.getParent();
        if (parent != null) {
            if (isAttachedToParent(element, parent)) {
                return checkParentChainState(parent);
            }
        }

        return true;
    }

    /**
     * Adds a listener for an event of the given type to the {@code component}.
     *
     * @param <T>
     *            the component event type
     * @param component
     *            the component to add the {@code listener}
     * @param eventType
     *            the component event type, not <code>null</code>
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    public static <T extends ComponentEvent<?>> Registration addListener(
            Component component, Class<T> eventType,
            ComponentEventListener<T> listener) {
        return component.addListener(eventType, listener);
    }

    /**
     * Adds a listener for an event of the given type to the {@code component},
     * and customizes the corresponding DOM event listener with the given
     * consumer. This allows overriding eg. the debounce settings defined in the
     * {@link DomEvent} annotation.
     * <p>
     * Note that customizing the DOM event listener works only for event types
     * which are annotated with {@link DomEvent}. Use
     * {@link #addListener(Component, Class, ComponentEventListener)} for other
     * listeners, or if you don't need to customize the DOM listener.
     *
     * @param <T>
     *            the event type
     * @param component
     *            the component to add the {@code listener}
     * @param eventType
     *            the event type for which to call the listener, must be
     *            annotated with {@link DomEvent}
     * @param listener
     *            the listener to call when the event occurs, not {@code null}
     * @param domListenerConsumer
     *            a consumer to customize the behavior of the DOM event
     *            listener, not {@code null}
     * @return a handle that can be used for removing the listener
     * @throws IllegalArgumentException
     *             if the event type is not annotated with {@link DomEvent}
     */
    public static <T extends ComponentEvent<?>> Registration addListener(
            Component component, Class<T> eventType,
            ComponentEventListener<T> listener,
            Consumer<DomListenerRegistration> domListenerConsumer) {
        return component.getEventBus().addListener(eventType, listener,
                domListenerConsumer);
    }

    /**
     * Dispatches the event to all listeners registered for the event type.
     *
     * @see Component#fireEvent(ComponentEvent)
     *
     * @param <T>
     *            the type of the component
     * @param component
     *            the component for which to fire events
     * @param componentEvent
     *            the event to fire
     */
    public static <T extends Component> void fireEvent(T component,
            ComponentEvent<? extends T> componentEvent) {
        component.fireEvent(componentEvent);
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

    private static <T, U> void setData(Component component,
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
     * Stores a arbitrary value for the given component.
     *
     * @see #setData(Component, Class, Object)
     * @see #getData(Component, String)
     *
     * @param component
     *            the component for which to set the data
     * @param key
     *            the key with which the instance can be retrieved, not
     *            <code>null</code>
     * @param value
     *            the data to set, or <code>null</code> to remove data
     *            previously set with the same key
     */
    public static void setData(Component component, String key, Object value) {
        setData(component, Attributes::setAttribute, key, value);
    }

    /**
     * Stores a an instance of a specific type for the given component.
     *
     * @see #setData(Component, String, Object)
     * @see #getData(Component, Class)
     *
     * @param <T>
     *            the data instance type
     * @param component
     *            the component for which to set the data
     * @param type
     *            the type of the data to set, not <code>null</code>
     * @param value
     *            the data instance to set, or <code>null</code> to remove data
     *            previously set with the same type
     */
    public static <T> void setData(Component component, Class<T> type,
            T value) {
        setData(component, Attributes::setAttribute, type, value);
    }

    private static <T, U> U getData(Component component,
            BiFunction<Attributes, T, U> getter, T key) {
        Attributes attributes = component.attributes;
        if (attributes == null) {
            return null;
        }
        return getter.apply(attributes, key);
    }

    /**
     * Gets a data instance with the given key, or <code>null</code> if no data
     * has been set for that key.
     *
     * @see #setData(Component, String, Object)
     *
     * @param component
     *            the component from which to get the data
     * @param key
     *            the data key
     * @return the data instance, or <code>null</code> if no instance has been
     *         set using the given key
     */
    public static Object getData(Component component, String key) {
        return getData(component, Attributes::getAttribute, key);
    }

    /**
     * Gets a data instance with the given type, or <code>null</code> if there
     * is no such instance.
     *
     * @see #setData(Component, Class, Object)
     *
     * @param <T>
     *            the data instance type
     * @param component
     *            the component from which to get the data
     * @param type
     *            the data type
     * @return the data instance, or <code>null</code> if no instance has been
     *         set using the given type
     */
    public static <T> T getData(Component component, Class<T> type) {
        return getData(component,
                (attributes, ignore) -> attributes.getAttribute(type), type);
    }

}
