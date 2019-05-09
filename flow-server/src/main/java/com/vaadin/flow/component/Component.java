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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.server.Attributes;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;

/**
 * A Component is a higher level abstraction of an {@link Element} or a
 * hierarchy of {@link Element}s.
 * <p>
 * A component must have exactly one root element which is created based on the
 * {@link Tag} annotation of the sub class (or in special cases set using the
 * constructor {@link #Component(Element)} or using
 * {@link #setElement(Component, Element)} before the element is attached to a
 * parent). The root element cannot be changed once it has been set.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class Component
        implements HasElement, AttachNotifier, DetachNotifier {

    /**
     * Encapsulates data required for mapping a new component instance to an
     * existing element.
     */
    static class MapToExistingElement implements Serializable {
        Element element = null;
        private boolean mapElementToComponent = false;

        public MapToExistingElement(Element element,
                boolean mapElementToComponent) {
            this.element = element;
            this.mapElementToComponent = mapElementToComponent;
        }

    }

    private static final PropertyDescriptor<String, Optional<String>> idDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("id", "");

    /**
     * Contains information about the element which should be used the next time
     * a component class is instantiated.
     */
    static ThreadLocal<MapToExistingElement> elementToMapTo = new ThreadLocal<>();

    private Element element;

    // Manipulated through ComponentUtil to avoid polluting the regular
    // Component API
    Attributes attributes;

    private ComponentEventBus eventBus = null;

    private final boolean templateMapped;

    /**
     * Creates a component instance with an element created based on the
     * {@link Tag} annotation of the sub class.
     * <p>
     * If this is invoked through {@link #from(Element, Class)} or
     * {@link Element#as(Class)}, uses the element defined in those methods
     * instead of creating a new element.
     */
    protected Component() {
        Optional<String> tagNameAnnotation = AnnotationReader
                .getAnnotationFor(getClass(), Tag.class).map(Tag::value);
        if (!tagNameAnnotation.isPresent()) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + " (or a super class) must be annotated with @"
                    + Tag.class.getName()
                    + " if the default constructor is used.");
        }

        String tagName = tagNameAnnotation.get();
        if (tagName.isEmpty()) {
            throw new IllegalStateException("@" + Tag.class.getSimpleName()
                    + " value cannot be empty.");
        }

        if (elementToMapTo.get() != null) {
            mapToElement(tagName);
            templateMapped = element != null && element.isVirtualChild();
        } else {
            Element e = new Element(tagName);
            setElement(this, e);
            templateMapped = false;
        }
    }

    /**
     * Creates a component instance based on the given element.
     * <p>
     * For nearly all cases you want to pass an element reference but it is
     * possible to pass {@code null} to this method. If you pass {@code null}
     * you must ensure that the element is initialized using
     * {@link #setElement(Component, Element)} before {@link #getElement()} is
     * used.
     *
     * @param element
     *            the root element for the component
     */
    protected Component(Element element) {
        if (elementToMapTo.get() != null) {
            mapToElement(element == null ? null : element.getTag());
            templateMapped = this.element != null
                    && this.element.isVirtualChild();
        } else {
            if (element != null) {
                setElement(this, element, true);
            }
            templateMapped = false;
        }
    }

    /**
     * Configures synchronized properties based on given annotations.
     */
    private void configureSynchronizedProperties() {
        ComponentUtil.getSynchronizedProperties(getClass()).forEach(
                info -> getElement().addSynchronizedProperty(info.getProperty(),
                        info.getUpdateMode()));
        ComponentUtil.getSynchronizedPropertyEvents(getClass())
                .forEach(getElement()::addSynchronizedPropertyEvent);
    }

    private void mapToElement(String tagName) {
        MapToExistingElement wrapData = elementToMapTo.get();
        assert wrapData != null;

        // Clear to be sure that the element is only used for one component
        elementToMapTo.remove();

        // Sanity check: validate that tag name matches
        String elementTag = wrapData.element.getTag();
        if (tagName != null && !tagName.equalsIgnoreCase(elementTag)) {
            throw new IllegalArgumentException("A component specified to use a "
                    + tagName + " element cannot use an element with tag name "
                    + elementTag);
        }
        setElement(this, wrapData.element, wrapData.mapElementToComponent);
    }

    /**
     * Gets the root element of this component.
     * <p>
     * Each component must have exactly one root element. When the component is
     * attached to a parent component, this element is attached to the parent
     * component's element hierarchy.
     *
     * @return the root element of this component
     */
    @Override
    public Element getElement() {
        assert element != null : "getElement() must not be called before the element has been set";
        return element;
    }

    /**
     * Initializes the root element of a component.
     * <p>
     * Each component must have a root element and it must be set before the
     * component is attached to a parent. The root element of a component cannot
     * be changed once it has been set.
     * <p>
     * Typically you do not want to call this method but define the element
     * through {@link #Component(Element)} instead.
     *
     * @param element
     *            the root element of the component
     * @param mapElementToComponent
     *            <code>true</code> to map the element to the component in
     *            addition to mapping the component to the element,
     *            <code>false</code> to only map the component to the element
     */
    private static void setElement(Component component, Element element,
            boolean mapElementToComponent) {
        if (component.element != null) {
            throw new IllegalStateException("Element has already been set");
        }
        if (element == null) {
            throw new IllegalArgumentException("Element must not be null");
        }
        component.element = element;
        if (mapElementToComponent) {
            ElementUtil.setComponent(element, component);
            component.configureSynchronizedProperties();
        }
    }

    /**
     * Initializes the root element of a component.
     * <p>
     * Each component must have a root element and it must be set before the
     * component is attached to a parent. The root element of a component cannot
     * be changed once it has been set.
     * <p>
     * Typically you do not want to call this method but define the element
     * through {@link #Component(Element)} instead.
     *
     * @param component
     *            the component to set the root element to
     * @param element
     *            the root element of the component
     */
    protected static void setElement(Component component, Element element) {
        setElement(component, element, true);
    }

    /**
     * Gets the parent component of this component.
     * <p>
     * A component can only have one parent.
     *
     * @return an optional parent component, or an empty optional if the
     *         component is not attached to a parent
     */
    public Optional<Component> getParent() {

        // If "this" is a component inside a Composite, iterate from the
        // Composite downwards
        Optional<Component> mappedComponent = getElement().getComponent();
        if (!mappedComponent.isPresent()) {
            throw new IllegalStateException(
                    "You cannot use getParent() on a wrapped component. Use Component.wrapAndMap to include the component in the hierarchy");
        }
        if (isInsideComposite(mappedComponent.get())) {
            Component parent = ComponentUtil.getParentUsingComposite(
                    (Composite<?>) mappedComponent.get(), this);
            return Optional.of(parent);
        }

        // Find the parent component based on the first parent element which is
        // mapped to a component
        return ComponentUtil.findParentComponent(getElement().getParent());
    }

    private boolean isInsideComposite(Component mappedComponent) {
        return mappedComponent instanceof Composite && mappedComponent != this;
    }

    /**
     * Gets the child components of this component.
     * <p>
     * The default implementation finds child components by traversing each
     * child {@link Element} tree.
     * <p>
     * If the component is injected to a PolymerTemplate using the
     * <code>@Id</code> annotation the getChildren method will only return
     * children added from the server side and will not return any children
     * declared in the template file.
     *
     * @see Id
     *
     * @return the child components of this component
     */
    public Stream<Component> getChildren() {
        // This should not ever be called for a Composite as it will return
        // wrong results
        assert !(this instanceof Composite);

        if (!getElement().getComponent().isPresent()) {
            throw new IllegalStateException(
                    "You cannot use getChildren() on a wrapped component. Use Component.wrapAndMap to include the component in the hierarchy");
        }

        Builder<Component> childComponents = Stream.builder();
        getElement().getChildren().forEach(childElement -> ComponentUtil
                .findComponents(childElement, childComponents::add));
        return childComponents.build();
    }

    /**
     * Gets the event bus for this component.
     * <p>
     * This method will create the event bus if it has not yet been created.
     *
     * @return the event bus for this component
     */
    protected ComponentEventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new ComponentEventBus(this);
        }
        return eventBus;
    }

    /**
     * Adds a listener for an event of the given type.
     *
     * @param <T>
     *            the component event type
     * @param eventType
     *            the component event type, not <code>null</code>
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for removing the listener
     */
    protected <T extends ComponentEvent<?>> Registration addListener(
            Class<T> eventType, ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    /**
     * Checks if there is at least one listener registered for the given event
     * type for this component.
     *
     * @param eventType
     *            the component event type
     * @return <code>true</code> if at least one listener is registered,
     *         <code>false</code> otherwise
     */
    @SuppressWarnings("rawtypes")
    protected boolean hasListener(Class<? extends ComponentEvent> eventType) {
        return eventBus != null && eventBus.hasListener(eventType);
    }

    /**
     * Dispatches the event to all listeners registered for the event type.
     *
     * @see ComponentUtil#fireEvent(Component, ComponentEvent)
     *
     * @param componentEvent
     *            the event to fire
     */
    protected void fireEvent(ComponentEvent<?> componentEvent) {
        if (hasListener(componentEvent.getClass())) {
            getEventBus().fireEvent(componentEvent);
        }
    }

    /**
     * Gets the UI this component is attached to.
     *
     * @return an optional UI component, or an empty optional if this component
     *         is not attached to a UI
     */
    public Optional<UI> getUI() {
        Optional<Component> parent = getParent();
        if (parent.isPresent()) {
            return parent.flatMap(Component::getUI);
        } else if (getElement().getParentNode() instanceof ShadowRoot) {
            parent = ComponentUtil.findParentComponent(
                    ((ShadowRoot) getElement().getParentNode()).getHost());
            return parent.flatMap(Component::getUI);
        }
        return Optional.empty();
    }

    /**
     * Sets the id of the root element of this component. The id is used with
     * various APIs to identify the element, and it should be unique on the
     * page.
     *
     * @param id
     *            the id to set, or <code>""</code> to remove any previously set
     *            id
     */
    public void setId(String id) {
        set(idDescriptor, id);
    }

    /**
     * Gets the id of the root element of this component.
     *
     * @see #setId(String)
     *
     * @return the id, or and empty optional if no id has been set
     */
    public Optional<String> getId() {
        return get(idDescriptor);
    }

    /**
     * Called when the component is attached to a UI.
     * <p>
     * The default implementation does nothing.
     * <p>
     * This method is invoked before the {@link AttachEvent} is fired for the
     * component.
     *
     * @param attachEvent
     *            the attach event
     */
    protected void onAttach(AttachEvent attachEvent) {
        // NOOP by default
    }

    /**
     * Called when the component is detached from a UI.
     * <p>
     * The default implementation does nothing.
     * <p>
     * This method is invoked before the {@link DetachEvent} is fired for the
     * component.
     *
     * @param detachEvent
     *            the detach event
     */
    protected void onDetach(DetachEvent detachEvent) {
        // NOOP by default
    }

    /**
     * Sets the value of the given component property.
     *
     * @see PropertyDescriptor
     *
     * @param <T>
     *            type of the value to set
     * @param descriptor
     *            the descriptor for the property to set, not <code>null</code>
     * @param value
     *            the new property value to set
     */
    protected <T> void set(PropertyDescriptor<T, ?> descriptor, T value) {
        assert descriptor != null;

        descriptor.set(this, value);
    }

    /**
     * Gets the value of the given component property.
     *
     * @see PropertyDescriptor
     *
     * @param <T>
     *            type of the value to get
     * @param descriptor
     *            the descriptor for the property to set, not <code>null</code>
     * @return the property value
     */
    protected <T> T get(PropertyDescriptor<?, T> descriptor) {
        assert descriptor != null;

        return descriptor.get(this);
    }

    /**
     * Creates a new component instance using the given element.
     * <p>
     * You can use this method when you have an element instance and want to use
     * it through the API of a {@link Component} class.
     * <p>
     * This method attaches the component instance to the element so that
     * {@link Element#getComponent()} returns the component instance. This means
     * that {@link #getParent()}, {@link #getChildren()} and other methods which
     * rely on {@link Element} -&gt; {@link Component} mappings will work
     * correctly.
     * <p>
     * Note that only one {@link Component} can be mapped to any given
     * {@link Element}.
     *
     * @see Element#as(Class)
     *
     * @param <T>
     *            the component type to create
     * @param element
     *            the element to wrap
     * @param componentType
     *            the component type
     * @return the component instance connected to the given element
     */
    public static <T extends Component> T from(Element element,
            Class<T> componentType) {
        return ComponentUtil.componentFromElement(element, componentType, true);
    }

    /**
     * Sets the component visibility value.
     * <p>
     * When a component is set as invisible, all the updates of the component
     * from the server to the client are blocked until the component is set as
     * visible again.
     * <p>
     * Invisible components don't receive any updates from the client-side.
     * Unlike the server-side updates, client-side updates, if any, are
     * discarded while the component is invisible, and are not transmitted to
     * the server when the component is made visible.
     *
     * @param visible
     *            the component visibility value
     */
    public void setVisible(boolean visible) {
        getElement().setVisible(visible);
    }

    /**
     * Gets the component visibility value.
     *
     * @return {@code true} if the component is visible, {@code false} otherwise
     */
    public boolean isVisible() {
        return getElement().isVisible();
    }

    /**
     * Handle component enable state when the enabled state changes.
     * <p>
     * By default this sets or removes the 'disabled' attribute from the
     * element. This can be overridden to have custom handling.
     *
     * @param enabled
     *            the new enabled state of the component
     */
    public void onEnabledStateChanged(boolean enabled) {
        // If the node has feature ElementData, then we know that the state
        // provider accepts attributes
        if (getElement().getNode().hasFeature(ElementData.class)) {
            getElement().setAttribute("disabled", !enabled);
        }
    }

    /**
     * Gets whether this component was attached as part of a
     * {@link PolymerTemplate} (by being mapped by an {@link Id} annotation), or
     * if it was created directly.
     *
     * @return <code>true</code> when it was mapped inside a template,
     *         <code>false</code> otherwise
     */
    protected boolean isTemplateMapped() {
        return templateMapped;
    }

    /**
     * Get the translation for the component locale.
     * <p>
     * The method never returns a null. If there is no {@link I18NProvider}
     * available or no translation for the {@code key} it returns an exception
     * string e.g. '!{key}!'.
     *
     * @see #getLocale()
     *
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found (implementation should not return
     *         null)
     */
    public String getTranslation(String key, Object... params) {
        return getTranslation(key, getLocale(), params);
    }

    /**
     * Get the translation for key with given locale.
     * <p>
     * The method never returns a null. If there is no {@link I18NProvider}
     * available or no translation for the {@code key} it returns an exception
     * string e.g. '!{key}!'.
     *
     * @param key
     *            translation key
     * @param locale
     *            locale to use
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     */
    public String getTranslation(String key, Locale locale, Object... params) {
        if (getI18NProvider() == null) {
            return "!{" + key + "}!";
        }
        return getI18NProvider().getTranslation(key, locale, params);
    }

    private I18NProvider getI18NProvider() {
        return VaadinService.getCurrent().getInstantiator().getI18NProvider();
    }

    /**
     * Gets the locale for this component.
     * <p>
     * It returns the {@link UI} locale if it has been set. If there is no
     * {@link UI} locale available then it tries to use the first locale
     * provided by the {@link I18NProvider}. If there is no any provided locale
     * then the default locale is used.
     *
     * @return the component locale
     */
    protected Locale getLocale() {
        UI currentUi = UI.getCurrent();
        Locale locale = currentUi == null ? null : currentUi.getLocale();
        if (locale == null) {
            List<Locale> locales = getI18NProvider().getProvidedLocales();
            if (locales != null && !locales.isEmpty()) {
                locale = locales.get(0);
            } else {
                locale = Locale.getDefault();
            }
        }
        return locale;
    }
}
