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
package com.vaadin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.Profiler;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.Util;
import com.vaadin.client.VConsole;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.ui.UIConnector;
import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.ComponentConstants;
import com.vaadin.shared.ui.ComponentStateUtil;
import com.vaadin.shared.ui.TabIndexState;

public abstract class AbstractComponentConnector extends AbstractConnector
        implements ComponentConnector {

    private Widget widget;

    /**
     * The style names from getState().getStyles() which are currently applied
     * to the widget.
     */
    private JsArrayString styleNames = JsArrayString.createArray().cast();

    /**
     * Default constructor
     */
    public AbstractComponentConnector() {
    }

    /**
     * Creates and returns the widget for this VPaintableWidget. This method
     * should only be called once when initializing the paintable.
     * <p>
     * You should typically not override this method since the framework by
     * default generates an implementation that uses {@link GWT#create(Class)}
     * to create a widget of the same type as returned by the most specific
     * override of {@link #getWidget()}. If you do override the method, you
     * can't call <code>super.createWidget()</code> since the metadata needed
     * for that implementation is not generated if there's an override of the
     * method.
     *
     * @return a new widget instance to use for this component connector
     */
    protected Widget createWidget() {
        HTML w = new HTML();
        setConnectorId(w.getElement(), getConnectorId());
        return w;
    }

    private static native void setConnectorId(Element el, String id)
    /*-{
        el.tkPid = id;
    }-*/;

    /**
     * Returns the widget associated with this paintable. The widget returned by
     * this method must not changed during the life time of the paintable.
     *
     * @return The widget associated with this paintable
     */
    @Override
    public Widget getWidget() {
        if (widget == null) {
            if (Profiler.isEnabled()) {
                Profiler.enter("AbstractComponentConnector.createWidget for "
                        + getClass().getSimpleName());
            }
            widget = createWidget();
            if (Profiler.isEnabled()) {
                Profiler.leave("AbstractComponentConnector.createWidget for "
                        + getClass().getSimpleName());
            }
        }

        return widget;
    }

    @Override
    public AbstractComponentState getState() {
        return (AbstractComponentState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        Profiler.enter("AbstractComponentConnector.onStateChanged");
        Profiler.enter("AbstractComponentConnector.onStateChanged update id");
        if (stateChangeEvent.hasPropertyChanged("id")) {
            if (getState().id != null) {
                getWidget().getElement().setId(getState().id);
            } else if (!stateChangeEvent.isInitialStateChange()) {
                getWidget().getElement().removeAttribute("id");
            }
        }
        Profiler.leave("AbstractComponentConnector.onStateChanged update id");

        /*
         * Disabled state may affect (override) tabindex so the order must be
         * first setting tabindex, then enabled state (through super
         * implementation).
         */
        Profiler.enter(
                "AbstractComponentConnector.onStateChanged update tab index");
        if (getState() instanceof TabIndexState) {
            if (getWidget() instanceof Focusable) {
                ((Focusable) getWidget())
                        .setTabIndex(((TabIndexState) getState()).tabIndex);
            } else {
                /*
                 * TODO Enable this error when all widgets have been fixed to
                 * properly support tabIndex, i.e. implement Focusable
                 */
                // VConsole.error("Tab index received for "
                // + Util.getSimpleName(getWidget())
                // + " which does not implement Focusable");
            }
        }
        Profiler.leave(
                "AbstractComponentConnector.onStateChanged update tab index");

        Profiler.enter(
                "AbstractComponentConnector.onStateChanged AbstractConnector.onStateChanged()");
        super.onStateChanged(stateChangeEvent);
        Profiler.leave(
                "AbstractComponentConnector.onStateChanged AbstractConnector.onStateChanged()");

        // Style names
        Profiler.enter(
                "AbstractComponentConnector.onStateChanged updateWidgetStyleNames");
        updateWidgetStyleNames();
        Profiler.leave(
                "AbstractComponentConnector.onStateChanged updateWidgetStyleNames");

        Profiler.leave("AbstractComponentConnector.onStateChanged");
    }

    @Override
    public void setWidgetEnabled(boolean widgetEnabled) {
        // add or remove v-disabled style name from the widget
        setWidgetStyleName(StyleConstants.DISABLED, !widgetEnabled);

        if (getWidget() instanceof HasEnabled) {
            // set widget specific enabled state
            ((HasEnabled) getWidget()).setEnabled(widgetEnabled);
        }

        // make sure the caption has or has not v-disabled style
        if (delegateCaptionHandling()) {
            ServerConnector parent = getParent();
            if (parent == null && !(this instanceof UIConnector)) {
                VConsole.error("Parent of connector "
                        + Util.getConnectorString(this)
                        + " is null. This is typically an indication of a broken component hierarchy");
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.client.ComponentConnector#delegateCaptionHandling ()
     */
    @Override
    public boolean delegateCaptionHandling() {
        return true;
    }

    /**
     * Updates the user defined, read-only and error style names for the widget
     * based the shared state. User defined style names are prefixed with the
     * primary style name of the widget returned by {@link #getWidget()}
     * <p>
     * This method can be overridden to provide additional style names for the
     * component, for example see
     * {@link AbstractFieldConnector#updateWidgetStyleNames()}
     * </p>
     */
    protected void updateWidgetStyleNames() {
        Profiler.enter("AbstractComponentConnector.updateWidgetStyleNames");
        AbstractComponentState state = getState();

        String primaryStyleName = getWidget().getStylePrimaryName();

        // Set the core 'v' style name for the widget
        setWidgetStyleName(StyleConstants.UI_WIDGET, true);

        // should be in AbstractFieldConnector ?
        // add / remove read-only style name
        setWidgetStyleName("v-readonly", isReadOnly());

        // add / remove error style name
        setWidgetStyleNameWithPrefix(primaryStyleName, StyleConstants.ERROR_EXT,
                null != state.errorMessage);

        // add additional user defined style names as class names, prefixed with
        // component default class name. remove nonexistent style names.

        // Remove all old stylenames
        for (int i = 0; i < styleNames.length(); i++) {
            String oldStyle = styleNames.get(i);
            setWidgetStyleName(oldStyle, false);
            setWidgetStyleNameWithPrefix(primaryStyleName + "-", oldStyle,
                    false);
        }
        styleNames.setLength(0);

        if (ComponentStateUtil.hasStyles(state)) {
            // add new style names
            for (String newStyle : state.styles) {
                setWidgetStyleName(newStyle, true);
                setWidgetStyleNameWithPrefix(primaryStyleName + "-", newStyle,
                        true);
                styleNames.push(newStyle);
            }

        }

        if (state.primaryStyleName != null
                && !state.primaryStyleName.equals(primaryStyleName)) {
            /*
             * We overwrite the widgets primary stylename if state defines a
             * primary stylename. This has to be done after updating other
             * styles to be sure the dependent styles are updated correctly.
             */
            getWidget().setStylePrimaryName(state.primaryStyleName);
        }
        Profiler.leave("AbstractComponentConnector.updateWidgetStyleNames");
    }

    /**
     * This is used to add / remove state related style names from the widget.
     * <p>
     * Override this method for example if the style name given here should be
     * updated in another widget in addition to the one returned by the
     * {@link #getWidget()}.
     * </p>
     *
     * @param styleName
     *            the style name to be added or removed
     * @param add
     *            <code>true</code> to add the given style, <code>false</code>
     *            to remove it
     */
    protected void setWidgetStyleName(String styleName, boolean add) {
        getWidget().setStyleName(styleName, add);
    }

    /**
     * This is used to add / remove state related prefixed style names from the
     * widget.
     * <p>
     * Override this method if the prefixed style name given here should be
     * updated in another widget in addition to the one returned by the
     * <code>Connector</code>'s {@link #getWidget()}, or if the prefix should be
     * different. For example see
     * {@link com.vaadin.client.ui.datefield.DateFieldConnector#setWidgetStyleNameWithPrefix(String, String, boolean)}
     * </p>
     *
     * @param styleName
     *            the style name to be added or removed
     * @param add
     *            <code>true</code> to add the given style, <code>false</code>
     *            to remove it
     * @deprecated This will be removed once styles are no longer added with
     *             prefixes.
     */
    @Deprecated
    protected void setWidgetStyleNameWithPrefix(String prefix, String styleName,
            boolean add) {
        if (!styleName.startsWith("-")) {
            if (!prefix.endsWith("-")) {
                prefix += "-";
            }
        } else {
            if (prefix.endsWith("-")) {
                styleName.replaceFirst("-", "");
            }
        }
        getWidget().setStyleName(prefix + styleName, add);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.client.ComponentConnector#isReadOnly()
     */
    @Override
    @Deprecated
    public boolean isReadOnly() {
        return getState().readOnly;
    }

    @Override
    public void updateEnabledState(boolean enabledState) {
        super.updateEnabledState(enabledState);

        setWidgetEnabled(isEnabled());
    }

    @Override
    public void onUnregister() {
        super.onUnregister();

        // Show an error if widget is still attached to DOM. It should never be
        // at this point.
        if (getWidget() != null && getWidget().isAttached()) {
            getWidget().removeFromParent();
            VConsole.error(
                    "Widget is still attached to the DOM after the connector ("
                            + Util.getConnectorString(this)
                            + ") has been unregistered. Widget was removed.");
        }
    }

    /**
     * Gets the URI of the icon set for this component.
     *
     * @return the URI of the icon, or <code>null</code> if no icon has been
     *         defined.
     */
    protected String getIconUri() {
        return getResourceUrl(ComponentConstants.ICON_RESOURCE);
    }

    /**
     * Gets the icon set for this component.
     *
     * @return the icon, or <code>null</code> if no icon has been defined.
     */
    protected Icon getIcon() {
        return getConnection().getIcon(getIconUri());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.client.ComponentConnector#flush()
     */
    @Override
    public void flush() {
        // No generic implementation. Override if needed
    }
}
