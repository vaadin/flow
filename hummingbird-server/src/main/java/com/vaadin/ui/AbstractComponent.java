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

package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.SizeWithUnit;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.ui.ComponentStateUtil;

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
public abstract class AbstractComponent extends AbstractClientConnector
        implements Component {

    /* Private members */

    /**
     * Application specific data object. The component does not use or modify
     * this.
     */
    private Object applicationData;

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

    private Boolean explicitImmediateValue;

    private String id;

    protected static final String DESIGN_ATTR_PLAIN_TEXT = "plain-text";

    /* Constructor */

    /**
     * Constructs a new Component.
     */
    public AbstractComponent() {
        // ComponentSizeValidator.setCreationLocation(this);
    }

    /* Get/Set component properties */

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.ui.Component#getId()
     */
    @Override
    public String getId() {
        return id;
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
        markAsDirty();
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

    /*
     * Notify the component that it's attached to a window. Don't add a JavaDoc
     * comment here, we use the default documentation from implemented
     * interface.
     */
    @Override
    public void attach() {
        super.attach();
        if (delayedFocus) {
            focus();
        }
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
        markAsDirty();
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
        markAsDirty();
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

    private static final Logger getLogger() {
        return Logger.getLogger(AbstractComponent.class.getName());
    }
}
