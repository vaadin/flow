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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

import com.vaadin.annotations.Tag;
import com.vaadin.event.EventRouter;
import com.vaadin.event.MethodEventSource;
import com.vaadin.server.AbstractErrorMessage.ContentMode;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.ErrorMessage.ErrorLevel;
import com.vaadin.server.Resource;
import com.vaadin.server.SizeWithUnit;
import com.vaadin.server.Sizeable;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.ComponentConstants;
import com.vaadin.shared.ui.ComponentStateUtil;
import com.vaadin.ui.declarative.DesignAttributeHandler;
import com.vaadin.ui.declarative.DesignContext;
import com.vaadin.util.ReflectTools;

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
        implements Component, MethodEventSource {

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

    /**
     * The component should receive focus (if {@link Focusable}) when attached.
     */
    private boolean delayedFocus;

    /* Sizeable fields */

    private float width = -1;
    private float height = -1;
    private Unit widthUnit = Unit.PIXELS;
    private Unit heightUnit = Unit.PIXELS;

    private boolean visible = true;

    private HasComponents parent;
    private ErrorHandler errorHandler = null;
    /**
     * The EventRouter used for the event model.
     */
    private EventRouter eventRouter = null;

    private com.vaadin.hummingbird.kernel.Element element;

    protected static final String DESIGN_ATTR_PLAIN_TEXT = "plain-text";

    /* Constructor */

    /**
     * Constructs a new Component.
     */
    public AbstractComponent() {
        // ComponentSizeValidator.setCreationLocation(this);
        setElement(new com.vaadin.hummingbird.kernel.Element(
                getClass().getAnnotation(Tag.class).value()));
    }

    /* Get/Set component properties */

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        getState().id = id;
        getElement().setAttribute("id", id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#getId()
     */
    @Override
    public String getId() {
        return getState(false).id;
    }

    /*
     * Gets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    @Override
    public String getStyleName() {
        String s = "";
        if (ComponentStateUtil.hasStyles(getState(false))) {
            for (final Iterator<String> it = getState(false).styles
                    .iterator(); it.hasNext();) {
                s += it.next();
                if (it.hasNext()) {
                    s += " ";
                }
            }
        }
        return s;
    }

    /*
     * Sets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    @Override
    public void setStyleName(String style) {
        if (style == null || "".equals(style)) {
            getState().styles = null;
            return;
        }
        if (getState().styles == null) {
            getState().styles = new ArrayList<String>();
        }
        List<String> styles = getState().styles;
        styles.clear();
        StringTokenizer tokenizer = new StringTokenizer(style, " ");
        while (tokenizer.hasMoreTokens()) {
            styles.add(tokenizer.nextToken());
        }
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
        if (style.contains(" ")) {
            // Split space separated style names and add them one by one.
            StringTokenizer tokenizer = new StringTokenizer(style, " ");
            while (tokenizer.hasMoreTokens()) {
                addStyleName(tokenizer.nextToken());
            }
            return;
        }

        if (getState().styles == null) {
            getState().styles = new ArrayList<String>();
        }
        List<String> styles = getState().styles;
        if (!styles.contains(style)) {
            styles.add(style);
        }
    }

    @Override
    public void removeStyleName(String style) {
        if (ComponentStateUtil.hasStyles(getState())) {
            StringTokenizer tokenizer = new StringTokenizer(style, " ");
            while (tokenizer.hasMoreTokens()) {
                getState().styles.remove(tokenizer.nextToken());
            }
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
        HasComponents parent = getParent();
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
    public HasComponents getParent() {
        return parent;
    }

    @Override
    public void setParent(HasComponents parent) {
        // If the parent is not changed, don't do anything
        if (parent == null ? this.parent == null : parent.equals(this.parent)) {
            return;
        }

        if (parent != null && this.parent != null) {
            throw new IllegalStateException(
                    getClass().getName() + " already has a parent.");
        }

        // Send a detach event if the component is currently attached
        if (isAttached()) {
            detach();
        }

        // Connect to new parent
        this.parent = parent;

        // Send attach event if the component is now attached
        if (isAttached()) {
            attach();
        }
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
    public <T extends HasComponents> T findAncestor(Class<T> parentType) {
        HasComponents p = getParent();
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
        fireComponentErrorEvent();
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

        getUI().getConnectorTracker().registerConnector(this);

        fireEvent(new AttachEvent(this));

        for (Component component : getAllChildrenIterable(this)) {
            component.attach();
        }

        if (delayedFocus) {
            focus();
        }
        if (locale != null) {
            getUI().getLocaleService().addLocale(locale);
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
        for (Component component : getAllChildrenIterable(this)) {
            component.detach();
        }

        fireEvent(new DetachEvent(this));

        getUI().getConnectorTracker().unregisterConnector(this);
    }

    /**
     * Sets the focus for this component if the component is {@link Focusable}.
     */
    protected void focus() {
        if (this instanceof Focusable) {
            final VaadinSession session = getSession();
            if (session != null) {
                getUI().setFocusedComponent((Focusable) this);
                delayedFocus = false;
            } else {
                delayedFocus = true;
            }
        }
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

    /* General event framework */

    private static final Method COMPONENT_EVENT_METHOD = ReflectTools
            .findMethod(Component.Listener.class, "componentEvent",
                    Component.Event.class);

    /* Component event framework */

    /*
     * Registers a new listener to listen events generated by this component.
     * Don't add a JavaDoc comment here, we use the default documentation from
     * implemented interface.
     */
    @Override
    public void addListener(Component.Listener listener) {
        addListener(Component.Event.class, listener, COMPONENT_EVENT_METHOD);
    }

    /*
     * Removes a previously registered listener from this component. Don't add a
     * JavaDoc comment here, we use the default documentation from implemented
     * interface.
     */
    @Override
    public void removeListener(Component.Listener listener) {
        removeListener(Component.Event.class, listener, COMPONENT_EVENT_METHOD);
    }

    /**
     * Emits the component event. It is transmitted to all registered listeners
     * interested in such events.
     */
    protected void fireComponentEvent() {
        fireEvent(new Component.Event(this));
    }

    /**
     * Emits the component error event. It is transmitted to all registered
     * listeners interested in such events.
     */
    protected void fireComponentErrorEvent() {
        fireEvent(new Component.ErrorEvent(getComponentError(), this));
    }

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
            getState().height = null;
            Style.remove(getElement(), "height");
        } else {
            getState().height = height + unit.toString();
            Style.add(getElement(), "height", getState().height);
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
            getState().width = null;
            Style.remove(getElement(), "width");
        } else {
            getState().width = width + unit.toString();
            Style.add(getElement(), "width", getState().width);
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

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#readDesign(org.jsoup.nodes.Element,
     * com.vaadin.ui.declarative.DesignContext)
     */
    @Override
    public void readDesign(Element design, DesignContext designContext) {
        Attributes attr = design.attributes();
        // handle default attributes
        for (String attribute : getDefaultAttributes()) {
            if (design.hasAttr(attribute)) {
                DesignAttributeHandler.assignValue(this, attribute,
                        design.attr(attribute));
            }

        }

        // handle locale
        if (attr.hasKey("locale")) {
            setLocale(getLocaleFromString(attr.get("locale")));
        }
        // handle width and height
        readSize(attr);
        // handle component error
        if (attr.hasKey("error")) {
            UserError error = new UserError(attr.get("error"), ContentMode.HTML,
                    ErrorLevel.ERROR);
            setComponentError(error);
        }
        // Tab index when applicable
        if (design.hasAttr("tabindex") && this instanceof Focusable) {
            ((Focusable) this).setTabIndex(DesignAttributeHandler.readAttribute(
                    "tabindex", design.attributes(), Integer.class));
        }

        // check for unsupported attributes
        Set<String> supported = new HashSet<String>();
        supported.addAll(getDefaultAttributes());
        supported.addAll(getCustomAttributes());
        for (Attribute a : attr) {
            if (!a.getKey().startsWith(":")
                    && !supported.contains(a.getKey())) {
                getLogger()
                        .info("Unsupported attribute found when reading from design : "
                                + a.getKey());
            }
        }
    }

    /**
     * Constructs a Locale corresponding to the given string. The string should
     * consist of one, two or three parts with '_' between the different parts
     * if there is more than one part. The first part specifies the language,
     * the second part the country and the third part the variant of the locale.
     * 
     * @param localeString
     *            the locale specified as a string
     * @return the Locale object corresponding to localeString
     */
    private Locale getLocaleFromString(String localeString) {
        if (localeString == null) {
            return null;
        }
        String[] parts = localeString.split("_");
        if (parts.length > 3) {
            throw new RuntimeException(
                    "Cannot parse the locale string: " + localeString);
        }
        switch (parts.length) {
        case 1:
            return new Locale(parts[0]);
        case 2:
            return new Locale(parts[0], parts[1]);
        default:
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }

    /**
     * Reads the size of this component from the given design attributes. If the
     * attributes do not contain relevant size information, defaults is
     * consulted.
     * 
     * @param attributes
     *            the design attributes
     * @param defaultInstance
     *            instance of the class that has default sizing.
     */
    private void readSize(Attributes attributes) {
        // read width
        if (attributes.hasKey("width-auto") || attributes.hasKey("size-auto")) {
            this.setWidth(null);
        } else if (attributes.hasKey("width-full")
                || attributes.hasKey("size-full")) {
            this.setWidth("100%");
        } else if (attributes.hasKey("width")) {
            this.setWidth(attributes.get("width"));
        }

        // read height
        if (attributes.hasKey("height-auto")
                || attributes.hasKey("size-auto")) {
            this.setHeight(null);
        } else if (attributes.hasKey("height-full")
                || attributes.hasKey("size-full")) {
            this.setHeight("100%");
        } else if (attributes.hasKey("height")) {
            this.setHeight(attributes.get("height"));
        }
    }

    /**
     * Writes the size related attributes for the component if they differ from
     * the defaults
     * 
     * @param component
     *            the component
     * @param attributes
     *            the attribute map where the attribute are written
     * @param defaultInstance
     *            the default instance of the class for fetching the default
     *            values
     */
    private void writeSize(Attributes attributes, Component defaultInstance) {
        if (hasEqualSize(defaultInstance)) {
            // we have default values -> ignore
            return;
        }
        boolean widthFull = getWidth() == 100f
                && getWidthUnits().equals(Sizeable.Unit.PERCENTAGE);
        boolean heightFull = getHeight() == 100f
                && getHeightUnits().equals(Sizeable.Unit.PERCENTAGE);
        boolean widthAuto = getWidth() == -1;
        boolean heightAuto = getHeight() == -1;

        // first try the full shorthands
        if (widthFull && heightFull) {
            attributes.put("size-full", "");
        } else if (widthAuto && heightAuto) {
            attributes.put("size-auto", "");
        } else {
            // handle width
            if (!hasEqualWidth(defaultInstance)) {
                if (widthFull) {
                    attributes.put("width-full", "");
                } else if (widthAuto) {
                    attributes.put("width-auto", "");
                } else {
                    String widthString = DesignAttributeHandler.getFormatter()
                            .format(getWidth()) + getWidthUnits().getSymbol();
                    attributes.put("width", widthString);

                }
            }
            if (!hasEqualHeight(defaultInstance)) {
                // handle height
                if (heightFull) {
                    attributes.put("height-full", "");
                } else if (heightAuto) {
                    attributes.put("height-auto", "");
                } else {
                    String heightString = DesignAttributeHandler.getFormatter()
                            .format(getHeight()) + getHeightUnits().getSymbol();
                    attributes.put("height", heightString);
                }
            }
        }
    }

    /**
     * Test if the given component has equal width with this instance
     * 
     * @param component
     *            the component for the width comparison
     * @return true if the widths are equal
     */
    private boolean hasEqualWidth(Component component) {
        return getWidth() == component.getWidth()
                && getWidthUnits().equals(component.getWidthUnits());
    }

    /**
     * Test if the given component has equal height with this instance
     * 
     * @param component
     *            the component for the height comparison
     * @return true if the heights are equal
     */
    private boolean hasEqualHeight(Component component) {
        return getHeight() == component.getHeight()
                && getHeightUnits().equals(component.getHeightUnits());
    }

    /**
     * Test if the given components has equal size with this instance
     * 
     * @param component
     *            the component for the size comparison
     * @return true if the sizes are equal
     */
    private boolean hasEqualSize(Component component) {
        return hasEqualWidth(component) && hasEqualHeight(component);
    }

    /**
     * Returns a collection of attributes that do not require custom handling
     * when reading or writing design. These are typically attributes of some
     * primitive type. The default implementation searches setters with
     * primitive values
     * 
     * @return a collection of attributes that can be read and written using the
     *         default approach.
     */
    private Collection<String> getDefaultAttributes() {
        Collection<String> attributes = DesignAttributeHandler
                .getSupportedAttributes(this.getClass());
        attributes.removeAll(getCustomAttributes());
        return attributes;
    }

    /**
     * Returns a collection of attributes that should not be handled by the
     * basic implementation of the {@link readDesign} and {@link writeDesign}
     * methods. Typically these are handled in a custom way in the overridden
     * versions of the above methods
     * 
     * @since 7.4
     * 
     * @return the collection of attributes that are not handled by the basic
     *         implementation
     */
    protected Collection<String> getCustomAttributes() {
        ArrayList<String> l = new ArrayList<String>(
                Arrays.asList(customAttributes));
        if (this instanceof Focusable) {
            l.add("tab-index");
            l.add("tabindex");
        }
        return l;
    }

    private static final String[] customAttributes = new String[] { "width",
            "height", "debug-id", "error", "width-auto", "height-auto",
            "width-full", "height-full", "size-auto", "size-full", "locale",
            "read-only", "_id" };

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#writeDesign(org.jsoup.nodes.Element,
     * com.vaadin.ui.declarative.DesignContext)
     */
    @Override
    public void writeDesign(Element design, DesignContext designContext) {
        AbstractComponent def = designContext.getDefaultInstance(this);
        Attributes attr = design.attributes();
        // handle default attributes
        for (String attribute : getDefaultAttributes()) {
            DesignAttributeHandler.writeAttribute(this, attribute, attr, def);
        }
        // handle locale
        if (getLocale() != null && (getParent() == null
                || !getLocale().equals(getParent().getLocale()))) {
            design.attr("locale", getLocale().toString());
        }
        // handle size
        writeSize(attr, def);
        // handle component error
        String errorMsg = getComponentError() != null
                ? getComponentError().getFormattedHtmlMessage() : null;
        String defErrorMsg = def.getComponentError() != null
                ? def.getComponentError().getFormattedHtmlMessage() : null;
        if (!Objects.equals(errorMsg, defErrorMsg)) {
            attr.put("error", errorMsg);
        }
        // handle tab index
        if (this instanceof Focusable) {
            DesignAttributeHandler.writeAttribute("tabindex", attr,
                    ((Focusable) this).getTabIndex(),
                    ((Focusable) def).getTabIndex(), Integer.class);
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
        addListener(AttachEvent.ATTACH_EVENT_IDENTIFIER, AttachEvent.class,
                listener, AttachListener.attachMethod);
    }

    @Override
    public void removeAttachListener(AttachListener listener) {
        removeListener(AttachEvent.ATTACH_EVENT_IDENTIFIER, AttachEvent.class,
                listener);
    }

    @Override
    public void addDetachListener(DetachListener listener) {
        addListener(DetachEvent.DETACH_EVENT_IDENTIFIER, DetachEvent.class,
                listener, DetachListener.detachMethod);
    }

    @Override
    public void removeDetachListener(DetachListener listener) {
        removeListener(DetachEvent.DETACH_EVENT_IDENTIFIER, DetachEvent.class,
                listener);
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

    /* Listener code starts. Should be refactored. */

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     * 
     * <p>
     * This method additionally informs the event-api to route events with the
     * given eventIdentifier to the components handleEvent function call.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventIdentifier
     *            the identifier of the event to listen for
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     * 
     * @since 6.2
     */
    protected void addListener(String eventIdentifier, Class<?> eventType,
            Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        boolean needRepaint = !eventRouter.hasListeners(eventType);
        eventRouter.addListener(eventType, target, method);

        if (needRepaint) {
            ComponentStateUtil.addRegisteredEventListener(getState(),
                    eventIdentifier);
        }
    }

    /**
     * Checks if the given event type is listened for this component.
     * 
     * @param eventType
     *            the event type to be checked
     * @return true if a listener is registered for the given event type
     */
    protected boolean hasListeners(Class<?> eventType) {
        return eventRouter != null && eventRouter.hasListeners(eventType);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     * 
     * <p>
     * This method additionally informs the event-api to stop routing events
     * with the given eventIdentifier to the components handleEvent function
     * call.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventIdentifier
     *            the identifier of the event to stop listening for
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     * 
     * @since 6.2
     */
    protected void removeListener(String eventIdentifier, Class<?> eventType,
            Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
            if (!eventRouter.hasListeners(eventType)) {
                ComponentStateUtil.removeRegisteredEventListener(getState(),
                        eventIdentifier);
            }
        }
    }

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     * 
     */
    @Override
    public void addListener(Class<?> eventType, Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, method);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     */
    @Override
    public void removeListener(Class<?> eventType, Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
        }
    }

    /**
     * Removes one registered listener method. The given method owned by the
     * given object will no longer be called when the specified events are
     * generated by this component.
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            target object that has registered to listen to events of type
     *            <code>eventType</code> with one or more methods.
     * @param method
     *            the method owned by <code>target</code> that's registered to
     *            listen to events of type <code>eventType</code>.
     */
    @Override
    public void removeListener(Class<?> eventType, Object target,
            Method method) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, method);
        }
    }

    /**
     * Returns all listeners that are registered for the given event type or one
     * of its subclasses.
     * 
     * @param eventType
     *            The type of event to return listeners for.
     * @return A collection with all registered listeners. Empty if no listeners
     *         are found.
     */
    public Collection<?> getListeners(Class<?> eventType) {
        if (eventRouter == null) {
            return Collections.EMPTY_LIST;
        }

        return eventRouter.getListeners(eventType);
    }

    /**
     * Sends the event to all listeners.
     * 
     * @param event
     *            the Event to be sent to all listeners.
     */
    protected void fireEvent(EventObject event) {
        if (eventRouter != null) {
            eventRouter.fireEvent(event);
        }
    }

    @Override
    public com.vaadin.hummingbird.kernel.Element getElement() {
        return element;
    }

    protected void setElement(com.vaadin.hummingbird.kernel.Element element) {
        this.element = element;
    }

    private static final Logger getLogger() {
        return Logger.getLogger(AbstractComponent.class.getName());
    }
}
