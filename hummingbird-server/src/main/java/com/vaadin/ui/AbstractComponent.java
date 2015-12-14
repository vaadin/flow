/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.vaadin.annotations.Tag;
import com.vaadin.event.EventSource;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Resource;
import com.vaadin.server.SizeWithUnit;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.ComponentConstants;

/**
 * An abstract class that defines default implementation for the
 * {@link Component} interface. Basic UI components that are not derived from an
 * external component can inherit this class to easily qualify as Vaadin
 * components. Most components in Vaadin do just that.
 *
 * @author Vaadin Ltd.
 * @since 3.0
 */
@SuppressWarnings("serial")
@Tag("div")
public abstract class AbstractComponent extends AbstractClientConnector
        implements Component, EventSource {

    /* Private members */

    /**
     * Application specific data object. The component does not use or modify
     * this.
     */
    private Object applicationData;

    /**
     * The internal error message of the component.
     */
    private ErrorMessage componentError = null;

    /**
     * Locale of this component.
     */
    private Locale locale;

    /* Sizeable fields */

    private float width = -1;
    private float height = -1;
    private Unit widthUnit = Unit.PIXELS;
    private Unit heightUnit = Unit.PIXELS;

    private boolean visible = true;

    private ErrorHandler errorHandler = null;
    private List<Runnable> runOnAttach;

    private boolean initCalled = false;

    protected static final String DESIGN_ATTR_PLAIN_TEXT = "plain-text";

    /* Constructor */

    /**
     * Constructs a new Component.
     */
    public AbstractComponent() {
        super();
    }

    protected AbstractComponent(String tagName) {
        super(tagName);
    }

    /* Get/Set component properties */

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        getElement().setAttribute("id", id);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#getId()
     */
    @Override
    public String getId() {
        return getElement().getAttribute("id");
    }

    /*
     * Gets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    @Override
    public String getStyleName() {
        return getElement().getClassNames();
    }

    /*
     * Sets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    @Override
    public void setStyleName(String style) {
        getElement().removeAllClasses();
        if (style == null || "".equals(style)) {
            return;
        }
        addStyleName(style);
    }

    @Override
    public void setPrimaryStyleName(String style) {
        getState().primaryStyleName = style;
    }

    @Override
    public String getPrimaryStyleName() {
        return getState(false).primaryStyleName;
    }

    @Override
    public void addStyleName(String style) {
        if (style == null || "".equals(style)) {
            return;
        }
        getElement().addClass(style);
    }

    @Override
    public void removeStyleName(String style) {
        StringTokenizer tokenizer = new StringTokenizer(style, " ");
        while (tokenizer.hasMoreTokens()) {
            getElement().removeClass(tokenizer.nextToken());
        }
    }

    /**
     * Adds or removes a style name. Multiple styles can be specified as a
     * space-separated list of style names.
     *
     * If the {@code add} parameter is true, the style name is added to the
     * component. If the {@code add} parameter is false, the style name is
     * removed from the component.
     * <p>
     * Functionally this is equivalent to using {@link #addStyleName(String)} or
     * {@link #removeStyleName(String)}
     *
     * @since 7.5
     * @param style
     *            the style name to be added or removed
     * @param add
     *            <code>true</code> to add the given style, <code>false</code>
     *            to remove it
     * @see #addStyleName(String)
     * @see #removeStyleName(String)
     */
    public void setStyleName(String style, boolean add) {
        if (add) {
            addStyleName(style);
        } else {
            removeStyleName(style);
        }
    }

    /*
     * Get's the component's caption. Don't add a JavaDoc comment here, we use
     * the default documentation from implemented interface.
     */
    @Override
    public String getCaption() {
        return getState(false).caption;
    }

    /**
     * Sets the component's caption <code>String</code>. Caption is the visible
     * name of the component. This method will trigger a
     * {@link RepaintRequestEvent}.
     *
     * @param caption
     *            the new caption <code>String</code> for the component.
     */
    @Override
    public void setCaption(String caption) {
        getState().caption = caption;
    }

    /**
     * Sets whether the caption is rendered as HTML.
     * <p>
     * If set to true, the captions are rendered in the browser as HTML and the
     * developer is responsible for ensuring no harmful HTML is used. If set to
     * false, the caption is rendered in the browser as plain text.
     * <p>
     * The default is false, i.e. to render that caption as plain text.
     *
     * @param captionAsHtml
     *            true if the captions are rendered as HTML, false if rendered
     *            as plain text
     */
    public void setCaptionAsHtml(boolean captionAsHtml) {
        getState().captionAsHtml = captionAsHtml;
    }

    /**
     * Checks whether captions are rendered as HTML
     * <p>
     * The default is false, i.e. to render that caption as plain text.
     *
     * @return true if the captions are rendered as HTML, false if rendered as
     *         plain text
     */
    public boolean isCaptionAsHtml() {
        return getState(false).captionAsHtml;
    }

    /*
     * Don't add a JavaDoc comment here, we use the default documentation from
     * implemented interface.
     */
    @Override
    public Locale getLocale() {
        if (locale != null) {
            return locale;
        }
        Component parent = getParent();
        if (parent != null) {
            return parent.getLocale();
        }
        final VaadinSession session = getSession();
        if (session != null) {
            return session.getLocale();
        }
        return null;
    }

    /**
     * Sets the locale of this component.
     *
     * <pre>
     * // Component for which the locale is meaningful
     * InlineDateField date = new InlineDateField(&quot;Datum&quot;);
     *
     * // German language specified with ISO 639-1 language
     * // code and ISO 3166-1 alpha-2 country code.
     * date.setLocale(new Locale(&quot;de&quot;, &quot;DE&quot;));
     *
     * date.setResolution(DateField.RESOLUTION_DAY);
     * layout.addComponent(date);
     * </pre>
     *
     *
     * @param locale
     *            the locale to become this component's locale.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;

        if (locale != null && isAttached()) {
            getUI().getLocaleService().addLocale(locale);
        }

        markAsDirty();
    }

    /*
     * Gets the component's icon resource. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    @Override
    public Resource getIcon() {
        return getResource(ComponentConstants.ICON_RESOURCE);
    }

    /**
     * Sets the component's icon. This method will trigger a
     * {@link RepaintRequestEvent}.
     *
     * @param icon
     *            the icon to be shown with the component's caption.
     */
    @Override
    public void setIcon(Resource icon) {
        setResource(ComponentConstants.ICON_RESOURCE, icon);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return getState(false).enabled;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        getState().enabled = enabled;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.client.Connector#isConnectorEnabled()
     */
    @Override
    public boolean isConnectorEnabled() {
        if (!isVisible()) {
            return false;
        } else if (!isEnabled()) {
            return false;
        } else if (!super.isConnectorEnabled()) {
            return false;
        } else if ((getParent() instanceof SelectiveRenderer)
                && !((SelectiveRenderer) getParent()).isRendered(this)) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#isVisible()
     */
    @Override
    public boolean isVisible() {
        return visible;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        if (isVisible() == visible) {
            return;
        }

        this.visible = visible;
        setStyleName("v-hidden", !visible);
        if (visible) {
            /*
             * If the visibility state is toggled from invisible to visible it
             * affects all children (the whole hierarchy) in addition to this
             * component.
             */
            markAsDirtyRecursive();
        }
        if (getParent() != null) {
            // Must always repaint the parent (at least the hierarchy) when
            // visibility of a child component changes.
            getParent().markAsDirty();
        }
    }

    /*
     * Gets the component's parent component. Don't add a JavaDoc comment here,
     * we use the default documentation from implemented interface.
     */
    @Override
    public Component getParent() {
        Element parentElement = getElement().getParent();
        while (parentElement != null
                && parentElement.getComponents().isEmpty()) {
            parentElement = parentElement.getParent();
        }

        if (parentElement == null) {
            return null;
        }

        return findParentComponent(this, parentElement);
    }

    @Override
    public void setParent(Component parent) {
        getLogger().severe("setParent() called - this should not be needed");
    }

    /**
     * Returns the closest ancestor with the given type.
     * <p>
     * To find the Window that contains the component, use {@code Window w =
     * getParent(Window.class);}
     * </p>
     *
     * @param <T>
     *            The type of the ancestor
     * @param parentType
     *            The ancestor class we are looking for
     * @return The first ancestor that can be assigned to the given class. Null
     *         if no ancestor with the correct type could be found.
     */
    public <T extends Component> T findAncestor(Class<T> parentType) {
        Component p = getParent();
        while (p != null) {
            if (parentType.isAssignableFrom(p.getClass())) {
                return parentType.cast(p);
            }
            p = p.getParent();
        }
        return null;
    }

    /**
     * Gets the error message for this component.
     *
     * @return ErrorMessage containing the description of the error state of the
     *         component or null, if the component contains no errors. Extending
     *         classes should override this method if they support other error
     *         message types such as validation errors or buffering errors. The
     *         returned error message contains information about all the errors.
     */
    public ErrorMessage getErrorMessage() {
        return componentError;
    }

    /**
     * Gets the component's error message.
     *
     * @link Terminal.ErrorMessage#ErrorMessage(String, int)
     *
     * @return the component's error message.
     */
    public ErrorMessage getComponentError() {
        return componentError;
    }

    /**
     * Sets the component's error message. The message may contain certain XML
     * tags, for more information see
     *
     * @link Component.ErrorMessage#ErrorMessage(String, int)
     *
     * @param componentError
     *            the new <code>ErrorMessage</code> of the component.
     */
    public void setComponentError(ErrorMessage componentError) {
        this.componentError = componentError;
        markAsDirty();
    }

    /*
     * Tests if the component is in read-only mode. Don't add a JavaDoc comment
     * here, we use the default documentation from implemented interface.
     */
    @Override
    public boolean isReadOnly() {
        return getState(false).readOnly;
    }

    /*
     * Sets the component's read-only mode. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    @Override
    public void setReadOnly(boolean readOnly) {
        getState().readOnly = readOnly;
    }

    @Override
    public void attach() {
        markAsDirty();

        UI ui = getUI();
        ui.getConnectorTracker().registerConnector(this);

        fireEvent(new AttachEvent(this));

        for (Component component : getChildComponents()) {
            component.attach();
        }

        if (locale != null) {
            ui.getLocaleService().addLocale(locale);
        }

        if (runOnAttach != null) {
            runOnAttach.forEach(Runnable::run);
            runOnAttach = null;
        }

        if (!initCalled) {
            initCalled = true;
            init();
        }
    }

    @Override
    public void markAsDirtyRecursive() {
        markAsDirty();

        for (ClientConnector connector : getChildComponents()) {
            connector.markAsDirtyRecursive();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The {@link #getSession()} and {@link #getUI()} methods might return
     * <code>null</code> after this method is called.
     * </p>
     */
    @Override
    public void detach() {
        assert isAttached() : "detach() should never be called for a detached component";
        for (Component component : getChildComponents()) {
            component.detach();
        }

        fireEvent(new DetachEvent(this));

        getUI().getConnectorTracker().unregisterConnector(this);
    }

    /**
     * Sets the focus for this component if the component is {@link Focusable}.
     */
    protected void focus() {
        getElement().focus();
    }

    /**
     * Build CSS compatible string representation of height.
     *
     * @return CSS height
     */
    private String getCSSHeight() {
        return getHeight() + getHeightUnits().getSymbol();
    }

    /**
     * Build CSS compatible string representation of width.
     *
     * @return CSS width
     */
    private String getCSSWidth() {
        return getWidth() + getWidthUnits().getSymbol();
    }

    /**
     * Returns the shared state bean with information to be sent from the server
     * to the client.
     *
     * Subclasses should override this method and set any relevant fields of the
     * state returned by super.getState().
     *
     * @since 7.0
     *
     * @return updated component shared state
     */
    @Override
    protected AbstractComponentState getState() {
        return (AbstractComponentState) super.getState();
    }

    @Override
    protected AbstractComponentState getState(boolean markAsDirty) {
        return (AbstractComponentState) super.getState(markAsDirty);
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);

        ErrorMessage error = getErrorMessage();
        if (null != error) {
            getState().errorMessage = error.getFormattedHtmlMessage();
        } else {
            getState().errorMessage = null;
        }
    }

    /* Component event framework */

    /**
     * Sets the data object, that can be used for any application specific data.
     * The component does not use or modify this data.
     *
     * @param data
     *            the Application specific data.
     * @since 3.1
     */
    public void setData(Object data) {
        applicationData = data;
    }

    /**
     * Gets the application specific data. See {@link #setData(Object)}.
     *
     * @return the Application specific data set with setData function.
     * @since 3.1
     */
    public Object getData() {
        return applicationData;
    }

    /* Sizeable and other size related methods */

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.Sizeable#getHeight()
     */
    @Override
    public float getHeight() {
        return height;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#getHeightUnits()
     */
    @Override
    public Unit getHeightUnits() {
        return heightUnit;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#getWidth()
     */
    @Override
    public float getWidth() {
        return width;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#getWidthUnits()
     */
    @Override
    public Unit getWidthUnits() {
        return widthUnit;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setHeight(float, Unit)
     */
    @Override
    public void setHeight(float height, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        this.height = height;
        heightUnit = unit;
        if (height < 0) {
            getElement().removeStyle("height");
        } else {
            getElement().setStyle("height", height + unit.toString());
        }
        // ComponentSizeValidator.setHeightLocation(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setSizeFull()
     */
    @Override
    public void setSizeFull() {
        setWidth(100, Unit.PERCENTAGE);
        setHeight(100, Unit.PERCENTAGE);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setSizeUndefined()
     */
    @Override
    public void setSizeUndefined() {
        setWidthUndefined();
        setHeightUndefined();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setWidthUndefined()
     */
    @Override
    public void setWidthUndefined() {
        setWidth(-1, Unit.PIXELS);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setHeightUndefined()
     */
    @Override
    public void setHeightUndefined() {
        setHeight(-1, Unit.PIXELS);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setWidth(float, Unit)
     */
    @Override
    public void setWidth(float width, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        this.width = width;
        widthUnit = unit;
        if (width < 0) {
            getElement().removeStyle("width");
        } else {
            getElement().setStyle("width", width + unit.toString());
        }
        // ComponentSizeValidator.setWidthLocation(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setWidth(java.lang.String)
     */
    @Override
    public void setWidth(String width) {
        SizeWithUnit size = SizeWithUnit.parseStringSize(width);
        if (size != null) {
            setWidth(size.getSize(), size.getUnit());
        } else {
            setWidth(-1, Unit.PIXELS);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.Sizeable#setHeight(java.lang.String)
     */
    @Override
    public void setHeight(String height) {
        SizeWithUnit size = SizeWithUnit.parseStringSize(height);
        if (size != null) {
            setHeight(size.getSize(), size.getUnit());
        } else {
            setHeight(-1, Unit.PIXELS);
        }
    }

    /**
     * Determine whether a <code>content</code> component is equal to, or the
     * ancestor of this component.
     *
     * @param content
     *            the potential ancestor element
     * @return <code>true</code> if the relationship holds
     */
    protected boolean isOrHasAncestor(Component content) {
        if (content instanceof HasComponents) {
            for (Component parent = this; parent != null; parent = parent
                    .getParent()) {
                if (parent.equals(content)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addAttachListener(AttachListener listener) {
        addListener(AttachEvent.class, listener);
    }

    @Override
    public void removeAttachListener(AttachListener listener) {
        removeListener(AttachEvent.class, listener);
    }

    @Override
    public void addDetachListener(DetachListener listener) {
        addListener(DetachEvent.class, listener);
    }

    @Override
    public void removeDetachListener(DetachListener listener) {
        removeListener(DetachEvent.class, listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.ClientConnector#getErrorHandler()
     */
    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.ClientConnector#setErrorHandler(com.vaadin.server.
     * ErrorHandler)
     */
    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /* Listener code starts */

    @Override
    public boolean hasListeners(Class<? extends EventObject> eventType) {
        return getEventRouter().hasListeners(eventType);
    }

    @Override
    public void addListener(Class<? extends EventObject> eventType,
            EventListener listener) {
        getEventRouter().addListener(eventType, listener);
    }

    @Override
    public void removeListener(Class<? extends EventObject> eventType,
            EventListener listener) {
        getEventRouter().removeListener(eventType, listener);
    }

    @Override
    public Collection<EventListener> getListeners(
            Class<? extends EventObject> eventType) {
        return getEventRouter().getListeners(eventType);
    }

    @Override
    public void fireEvent(EventObject event) {
        getEventRouter().fireEvent(event);
    }

    private static final Logger getLogger() {
        return Logger.getLogger(AbstractComponent.class.getName());
    }

    /* Element related */

    /**
     * Sets the root element for this component.
     * <p>
     * The root element is the base of the component and is the element which is
     * added to the DOM. A component must always have a single root element.
     * <p>
     * As far as possible, the component should aim to store its state
     * information in the element or in sub elements, i.e. the Component class
     * should be as stateless as possible.
     * <p>
     * The root element is typically set when the component is created but it
     * can also be set again later, when binding a Component instance to an
     * existing element. If the component stores state outside the element, this
     * method should be overridden to handle updating this state information.
     *
     * @param element
     *            the element to use for the component
     */
    @Override
    protected void setElement(Element element) {
        if (initCalled) {
            throw new IllegalStateException(
                    "setElement must not be called after the component has been initialized through init()");
        }
        super.setElement(element);
        // Map the element to this component
        element.getTemplate().getComponents(element.getNode(), true).add(this);
        // TODO remove old component reference if changed?
        // TODO What about Composite?
    }

    @Override
    public void elementAttached() {
        if (isAttached()) {
            attach();
        }
    }

    @Override
    public void elementDetached() {
        if (isAttached()) {
            detach();
        }
    }

    @Override
    public Iterator<Component> iterator() {
        return getChildComponents().iterator();
    }

    @Override
    public List<Component> getChildComponents() {
        List<Component> childComponents = new ArrayList<>();
        for (int i = 0; i < getElement().getChildCount(); i++) {
            Element child = getElement().getChild(i);
            findComponents(child, childComponents);
        }
        return childComponents;
    }

    private void findComponents(Element element, List<Component> components) {
        if (!element.getComponents().isEmpty()) {
            components.add(findChildComponent(this, element));
            return;
        }

        for (int i = 0; i < element.getChildCount(); i++) {
            Element child = element.getChild(i);
            findComponents(child, components);
        }
    }

    private static Component findChildComponent(Component parent,
            Element childElementWithComponent) {
        assert !childElementWithComponent.getComponents().isEmpty();

        List<Component> components = childElementWithComponent.getComponents();
        if (components.size() == 1) {
            return components.get(0);
        } else {
            if (parent instanceof Composite) {
                // Inside a composite chain
                int i = components.indexOf(parent);
                if (i == -1) {
                    throw new IllegalStateException(
                            "Composite hierarchy is incorrect: "
                                    + parent.getClass().getName()
                                    + " not found in Composite chain");
                } else if (i == components.size() - 1) {
                    // Parent is the last composite
                    return components.get(0);
                } else {
                    // Component is index 0, parents 1..N.
                    // Next child is thus i-1
                    return components.get(i - 1);
                }
            } else {
                // The element is the parent of a composite chain
                return components.get(components.size() - 1);
            }
        }
    }

    private static Component findParentComponent(Component child,
            Element parentElementWithComponent) {
        assert !parentElementWithComponent.getComponents().isEmpty();

        Element childElementWithComponent = child.getElement();
        assert !childElementWithComponent.getComponents().isEmpty();

        // Composite is a special, special case...
        List<Component> childComponents = childElementWithComponent
                .getComponents();
        if (childComponents.size() == 1) {
            // No composite chain - the standard case
            return parentElementWithComponent.getComponents().get(0);
        } else {
            int childIndexInCompositeChain;
            if (child instanceof Composite) {
                // Inside a composite chain
                childIndexInCompositeChain = childComponents.indexOf(child);
            } else {
                // This is the composition root of the last composite
                // -> return first composite
                childIndexInCompositeChain = 0;
            }

            if (childIndexInCompositeChain == -1) {
                throw new IllegalStateException(
                        "Composite hierarchy is incorrect: "
                                + child.getClass().getName()
                                + " not found in Composite chain");
            } else if (childIndexInCompositeChain == childComponents.size()
                    - 1) {
                // This is the last Composite parent
                // -> the parent component is the parentElement's component
                return parentElementWithComponent.getComponents().get(0);
            } else {
                // Component is index 0, parents 1..N.
                // Next parent is thus i+1
                return childComponents.get(childIndexInCompositeChain + 1);
            }
        }

    }

    @Override
    public void runAttached(Runnable runnable) {
        if (isAttached()) {
            runnable.run();
        } else {
            if (runOnAttach == null) {
                runOnAttach = new ArrayList<>();
            }
            runOnAttach.add(runnable);
        }
    }

    /**
     * Helper for {@link JS#get(Class, Component)} which uses this component as
     * the scope
     *
     * @param javascriptInterface
     *            the Javascript interface to use
     * @return a proxy object used to execute Javascript on the client
     */
    protected <T> T getJS(Class<T> javascriptInterface) {
        return JS.get(javascriptInterface, this);
    }

    @Override
    public Element preRender() {
        if (getElement().getTag().contains("-")) {
            // Web component, just render an empty div for now
            Element div = new Element("div");
            if (getElement().hasAttribute("style")) {
                div.setAttribute("style", getElement().getAttribute("style"));
            }
            if (getElement().hasAttribute("class")) {
                div.setAttribute("class", getElement().getAttribute("class"));
            }

            return div;
        } else {
            return PreRenderer.preRenderElementTree(getElement());
        }
    }

    /**
     * Maps the component to the existing element.
     *
     * @deprecated Because the API is not thought through
     * @param component
     * @param element
     */
    @Deprecated
    public static void mapComponent(AbstractComponent component,
            Element element) {
        if (component.isAttached()) {
            // Detach from old position
            component.detach();
        }

        component.setElement(element);
        component.elementAttached();
    }

    /**
     * Method for performing initialization of the component.
     * <p>
     * This method is called once for each component after the component has
     * been attached to a UI for the first time. T
     * <p>
     * The element for the component has been set to the final instance when
     * this method is called and must never change in or after init().
     */
    protected void init() {
    }

}
