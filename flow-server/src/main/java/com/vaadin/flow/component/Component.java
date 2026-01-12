/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.vaadin.flow.component.internal.ComponentMetaData;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.server.Attributes;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

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
        implements HasStyle, AttachNotifier, DetachNotifier {

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

    private static final PropertyChangeListener NOOP_PROPERTY_LISTENER = event -> {
        // NOOP
    };

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
        ComponentTracker.trackCreate(this);
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
        ComponentTracker.trackCreate(this);
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
        ComponentUtil.getSynchronizedProperties(getClass())
                .forEach(this::addSynchronizedProperty);
    }

    private void addSynchronizedProperty(
            ComponentMetaData.SynchronizedPropertyInfo info) {
        if (info.getUpdateMode() == null) {
            throw new IllegalArgumentException(getClass().getName()
                    + ": property update control mode for disabled element in @Synchronize annotation must not be null.");
        }
        info.getEventNames().forEach(eventType -> {
            if (eventType == null) {
                throw new IllegalArgumentException(getClass().getName()
                        + ": event type must not be null for @Synchronize annotation");
            }
            DomListenerRegistration propertyListener = element
                    .addPropertyChangeListener(info.getProperty(), eventType,
                            NOOP_PROPERTY_LISTENER)
                    .setDisabledUpdateMode(info.getUpdateMode());
            if (info.getAllowInert()) {
                propertyListener.allowInert();
            }
        });
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
     * Gets the low level root element of this component.
     * <p>
     * <b>Note!</b> Element API is designed for building components at a lower
     * abstraction level than normal Vaadin UI development. If you see a direct
     * call to this method in your applications UI code, you should consider
     * that as a sign that you are probably doing something wrong and you should
     * instead use other methods from your component, e.g. when getting
     * children, parent or ancestor component or adding listeners. This method
     * is breaking the Component's abstraction layer and its implementations
     * provided. You should only call this method and use the Element API when
     * creating or extending components (e.g. setting the attributes and
     * properties, adding DOM listeners, execute JavaScript code), or when you
     * otherwise need to break through the abstraction layer. If it is a hack or
     * a workaround, it is also better to hide that into an extension, helper
     * class, separate add-on module or at least into a private method
     * documenting the usage.
     * <p>
     * Each component must have exactly one root element. When the component is
     * attached to a parent component, this element is attached to the parent
     * component's element hierarchy.
     *
     * @return the root element of this component
     */
    @Override
    public Element getElement() {
        assert element != null
                : "getElement() must not be called before the element has been set";
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
     * Returns all listeners that match or extend the given event type.
     *
     * @param eventType
     *            the component event type
     * @return A collection with all registered listeners for a given event
     *         type. Empty if no listeners are found.
     */
    protected Collection<?> getListeners(
            Class<? extends ComponentEvent> eventType) {
        return eventBus != null ? eventBus.getListeners(eventType)
                : Collections.emptyList();
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
     * This method is invoked before the {@link AttachEvent} is fired for the
     * component. Make sure to call <code>super.onAttach</code> when overriding
     * this method.
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
     * This method is invoked before the {@link DetachEvent} is fired for the
     * component.
     * <p>
     * Make sure to call <code>super.onDetach</code> when overriding this
     * method.
     *
     * @param detachEvent
     *            the detach event
     */
    protected void onDetach(DetachEvent detachEvent) {
        // NOOP by default
    }

    /**
     * Checks whether this component is currently attached to a UI.
     * <p>
     * When {@link UI#close()} is called, the UI and the components are not
     * detached immediately; the UI cleanup is performed at the end of the
     * current request which also detaches the UI and its components.
     *
     * @return true if the component is attached to an active UI.
     */
    public boolean isAttached() {
        return getElement().getNode().isAttached();
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
     * Binds a {@link Signal}'s value to the <code>visible</code> property of
     * this component and keeps property synchronized with the signal value
     * while the component is in attached state. When the element is in detached
     * state, signal value changes have no effect. <code>null</code> signal
     * unbinds the existing binding.
     * <p>
     * While a Signal is bound to a property, any attempt to set the visibility
     * manually with {@link #setVisible(boolean)} throws
     * {@link com.vaadin.signals.BindingActiveException}. Same happens when
     * trying to bind a new Signal while one is already bound.
     * <p>
     * Example of usage:
     *
     * <pre>
     * ValueSignal&lt;Boolean&gt; signal = new ValueSignal&lt;&gt;(true);
     * Span component = new Span();
     * add(component);
     * component.bindVisible(signal);
     * signal.value(false); // The component is set hidden
     * </pre>
     *
     * @param visibleSignal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setVisible(boolean)
     */
    public void bindVisible(Signal<Boolean> visibleSignal) {
        getElement().bindVisible(visibleSignal);
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
     * Gets whether this component was attached as part of a template (by being
     * mapped by an {@link Id} annotation), or if it was created directly.
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
        final Optional<I18NProvider> i18NProvider = LocaleUtil
                .getI18NProvider();
        return i18NProvider
                .map(i18n -> i18n.getTranslation(key,
                        LocaleUtil.getLocale(() -> i18NProvider), params))
                .orElseGet(() -> "!{" + key + "}!");
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
    public String getTranslation(Object key, Object... params) {
        final Optional<I18NProvider> i18NProvider = LocaleUtil
                .getI18NProvider();
        return i18NProvider
                .map(i18n -> i18n.getTranslation(key,
                        LocaleUtil.getLocale(() -> i18NProvider), params))
                .orElseGet(() -> "!{" + key + "}!");
    }

    /**
     * Get the translation for key with given locale.
     * <p>
     * The method never returns a null. If there is no {@link I18NProvider}
     * available or no translation for the {@code key} it returns an exception
     * string e.g. '!{key}!'.
     * <p>
     * For the maintainers: this method will remain deprecated but should not be
     * removed. Removing it would cause silent runtime issues where
     * {@link #getTranslation(String, Object...)} might be invoked instead of
     * the correct method. By keeping this deprecated method, developers will
     * receive deprecation warnings rather than encountering subtle runtime
     * problems.
     *
     * @param key
     *            translation key
     * @param locale
     *            locale to use
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     * @deprecated Use {@link #getTranslation(Locale, String, Object...)}
     *             instead
     */
    @Deprecated
    public String getTranslation(String key, Locale locale, Object... params) {
        return getTranslation(locale, key, params);
    }

    /**
     * Get the translation for key with given locale.
     * <p>
     * The method never returns a null. If there is no {@link I18NProvider}
     * available or no translation for the {@code key} it returns an exception
     * string e.g. '!{key}!'.
     * <p>
     * For the maintainers: this method will remain deprecated but should not be
     * removed. Removing it would cause silent runtime issues where
     * {@link #getTranslation(String, Object...)} might be invoked instead of
     * the correct method. By keeping this deprecated method, developers will
     * receive deprecation warnings rather than encountering subtle runtime
     * problems.
     *
     * @param key
     *            translation key
     * @param locale
     *            locale to use
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     * @deprecated Use {@link #getTranslation(Locale, String, Object...)}
     *             instead
     */
    @Deprecated
    public String getTranslation(Object key, Locale locale, Object... params) {
        return getTranslation(locale, key, params);
    }

    /**
     * Get the translation for key with given locale.
     * <p>
     * The method never returns a null. If there is no {@link I18NProvider}
     * available or no translation for the {@code key} it returns an exception
     * string e.g. '!{key}!'.
     *
     * @param locale
     *            locale to use
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     */
    public String getTranslation(Locale locale, String key, Object... params) {
        return LocaleUtil.getI18NProvider()
                .map(i18n -> i18n.getTranslation(key, locale, params))
                .orElseGet(() -> "!{" + key + "}!");
    }

    /**
     * Get the translation for key with given locale.
     * <p>
     * The method never returns a null. If there is no {@link I18NProvider}
     * available or no translation for the {@code key} it returns an exception
     * string e.g. '!{key}!'.
     *
     * @param locale
     *            locale to use
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     */
    public String getTranslation(Locale locale, Object key, Object... params) {
        return LocaleUtil.getI18NProvider()
                .map(i18n -> i18n.getTranslation(key, locale, params))
                .orElseGet(() -> "!{" + key + "}!");
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
        return LocaleUtil.getLocale(LocaleUtil::getI18NProvider);
    }

    /**
     * Scrolls the current component into the visible area of the browser
     * window.
     * <p>
     * This method can be called with no arguments for default browser behavior,
     * or with one or more {@link ScrollIntoViewOption} values to control
     * scrolling behavior:
     * <ul>
     * <li>{@link ScrollIntoViewOption.Behavior} - controls whether scrolling is
     * instant or smooth</li>
     * <li>{@link ScrollIntoViewOption.Block} - controls vertical alignment of
     * the element</li>
     * <li>{@link ScrollIntoViewOption.Inline} - controls horizontal alignment
     * of the element</li>
     * </ul>
     * <p>
     * Examples:
     *
     * <pre>
     * component.scrollIntoView(ScrollIntoViewOption.Behavior.SMOOTH);
     * component.scrollIntoView(ScrollIntoViewOption.Block.END);
     * component.scrollIntoView(ScrollIntoViewOption.Behavior.SMOOTH,
     *         ScrollIntoViewOption.Block.END,
     *         ScrollIntoViewOption.Inline.CENTER);
     * </pre>
     *
     * @param options
     *            zero or more scroll options
     */
    public void scrollIntoView(ScrollIntoViewOption... options) {
        getElement().scrollIntoView(options);
    }

    /**
     * Scrolls the current component into the visible area of the browser
     * window.
     *
     * @deprecated Use {@link #scrollIntoView(ScrollIntoViewOption...)} instead
     * @param scrollOptions
     *            options to define the scrolling behavior
     */
    @Deprecated(since = "25.0", forRemoval = true)
    public void scrollIntoView(ScrollOptions scrollOptions) {
        getElement().scrollIntoView(scrollOptions);
    }

    /**
     * Traverses the component tree up and returns the first ancestor component
     * that matches the given type.
     *
     * @param componentType
     *            the class of the ancestor component to search for
     * @return The first ancestor that can be assigned to the given class. Null
     *         if no ancestor with the correct type could be found.
     * @param <T>
     *            the type of the ancestor component to return
     */
    public <T> T findAncestor(Class<T> componentType) {
        Optional<Component> optionalParent = getParent();
        while (optionalParent.isPresent()) {
            Component parent = optionalParent.get();
            if (componentType.isAssignableFrom(parent.getClass())) {
                return componentType.cast(parent);
            } else {
                optionalParent = parent.getParent();
            }
        }
        return null;
    }

    /**
     * Removes the component from its parent.
     */
    public void removeFromParent() {
        getElement().removeFromParent();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        if (this instanceof UI ui) {
            Map<Class<?>, CurrentInstance> instances = CurrentInstance
                    .setCurrent(ui);
            try {
                out.defaultWriteObject();
            } finally {
                CurrentInstance.clearAll();
                CurrentInstance.restoreInstances(instances);
            }
        } else {
            out.defaultWriteObject();
        }
    }

    @Serial
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        if (this instanceof UI ui) {
            Map<Class<?>, CurrentInstance> instances = CurrentInstance
                    .getInstances();
            // Cannot use CurrentInstance.setCurrent(this) because it will try
            // to get VaadinSession from UI.internals that is not yet available
            CurrentInstance.set(UI.class, ui);
            try {
                in.defaultReadObject();
            } finally {
                CurrentInstance.clearAll();
                CurrentInstance.restoreInstances(instances);
            }
        } else {
            in.defaultReadObject();
        }
    }

}
