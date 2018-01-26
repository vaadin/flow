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
import java.util.Optional;
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
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.internal.ReflectionCache;

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
     * Gets the name of the synchronized properties defined declaratively for
     * the given class.
     *
     * @param componentClass
     *            the component class to check
     * @return the synchronized properties defined declaratively for the class
     */
    public static Stream<String> getSynchronizedProperties(
            Class<? extends Component> componentClass) {
        Collection<SynchronizedPropertyInfo> infos = componentMetaDataCache
                .get(componentClass).getSynchronizedProperties();
        return infos.stream().map(info -> info.getProperty());
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
     * @param componentClass
     *            the component class to check
     * @return the dependencies for the given class
     */
    public static DependencyInfo getDependencies(
            Class<? extends Component> componentClass) {
        return componentMetaDataCache.get(componentClass).getDependencyInfo();
    }

}
