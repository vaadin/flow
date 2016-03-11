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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Optional;

import com.vaadin.event.EventRouter;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.namespace.DependencyListNamespace;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.Dependency.Type;
import com.vaadin.ui.FrameworkData.JavaScriptInvocation;
import com.vaadin.util.ReflectTools;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Represents the web page open in the browser, containing the UI it is
 * connected to.
 *
 * @author Vaadin
 * @since
 */
public class Page implements Serializable {

    /**
     * Event fired when the location has changed.
     * <p>
     * Situations when this might happen include:
     * <ul>
     * <li>{@link Page#setLocation(String)} is called.
     * <li>A <code>PopStateEvent</code> is fired in the browser e.g. because the
     * user used the browsers back or forward button.
     * <li>Navigation has been triggered by the user clicking a link marked with
     * attribute {@value ApplicationConstants#ROUTER_LINK_ATTRIBUTE}.
     * </ul>
     */
    public static class LocationChangeEvent extends EventObject {
        private final String location;
        private final transient JsonValue state;

        /**
         * Creates a new event.
         *
         * @param page
         *            the page instance that fired the event, not
         *            <code>null</code>
         * @param state
         *            the history state from the browser, <code>null</code> if
         *            no state was provided
         * @param location
         *            the new browser location, not <code>null</code>
         */
        public LocationChangeEvent(Page page, JsonValue state,
                String location) {
            super(page);

            assert location != null;

            this.location = location;
            this.state = state;
        }

        @Override
        public Page getSource() {
            return (Page) super.getSource();
        }

        /**
         * Gets the location that was opened. This is relative to the base url.
         *
         * @return the location, not null
         */
        public String getLocation() {
            return location;
        }

        /**
         * Gets the history state value as JSON.
         *
         * @return an optional JSON state value
         */
        public Optional<JsonValue> getState() {
            return Optional.ofNullable(state);
        }

    }

    /**
     * Handles location change events.
     *
     * @see LocationChangeEvent
     */
    @FunctionalInterface
    public interface LocationChangeListener extends Serializable {
        /**
         * Invoked when a <code>popstate</code> event is fired.
         *
         * @param event
         *            the event
         */
        void onLocationChange(LocationChangeEvent event);
    }

    private static final Method locationChangeMethod = ReflectTools.findMethod(
            LocationChangeListener.class, "onLocationChange",
            LocationChangeEvent.class);

    private final UI ui;
    private final History history;

    private String location = "";

    private EventRouter eventRouter = new EventRouter();

    /**
     * Creates a page instance for the given UI.
     *
     * @param ui
     *            the UI that this page instance is connected to
     */
    public Page(UI ui) {
        this.ui = ui;
        history = new History(ui);
    }

    /**
     * Adds the given style sheet to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the context path of the
     * application.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols such as "vaadin://" can be used.
     *
     * @param url
     *            the URL to load the style sheet from, not <code>null</code>
     */
    public void addStyleSheet(String url) {
        addDependency(new Dependency(Type.STYLESHEET, url));
    }

    /**
     * Adds the given JavaScript to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the context path of the
     * application.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols such as "vaadin://" can be used.
     *
     * @param url
     *            the URL to load the JavaScript from, not <code>null</code>
     */
    public void addJavaScript(String url) {
        addDependency(new Dependency(Type.JAVASCRIPT, url));
    }

    /**
     * Adds the given dependency to the page and ensures that it is loaded
     * successfully.
     *
     * @param dependency
     *            the dependency to load
     */
    private void addDependency(Dependency dependency) {
        assert dependency != null;

        DependencyListNamespace namespace = ui.getFrameworkData().getStateTree()
                .getRootNode().getNamespace(DependencyListNamespace.class);

        namespace.add(dependency);
    }

    /**
     * Asynchronously runs the given JavaScript expression in the browser. The
     * given parameters will be available to the expression as variables named
     * <code>$0</code>, <code>$1</code>, and so on. Supported parameter types
     * are:
     * <ul>
     * <li>{@link String}
     * <li>{@link Integer}
     * <li>{@link Double}
     * <li>{@link Boolean}
     * <li>{@link Element} (will be sent as <code>null</code> if the server-side
     * element instance is not attached when the invocation is sent to the
     * client)
     * </ul>
     *
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     */
    public void executeJavaScript(String expression, Object... parameters) {
        /*
         * To ensure attached elements are actually attached, the parameters
         * won't be serialized until the phase the UIDL message is created. To
         * give the user immediate feedback if using a parameter type that can't
         * be serialized, we do a dry run at this point.
         */
        for (Object argument : parameters) {
            // Throws IAE for unsupported types
            JsonCodec.encodeWithTypeInfo(argument);
        }

        ui.getFrameworkData().addJavaScriptInvocation(new JavaScriptInvocation(
                expression, Arrays.asList(parameters)));
    }

    /**
     * Gets a representation of <code>window.history</code> for this page.
     *
     * @return the history representation
     */
    public History getHistory() {
        return history;
    }

    /**
     * Gets the current location, relative to the base URI of the current UI.
     *
     * @return the current location, not <code>null</code>.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Navigates the user's browser to a new location, relative to the base URI
     * of the current UI. New locations inside the application (i.e. a relative
     * URL without <code>..</code> segments) will immediately trigger a
     * {@link LocationChangeEvent} to be fired. New locations outside the
     * application (e.g. an absolute URL) causes the user to leave the
     * application without firing any {@link LocationChangeEvent}.
     *
     * @param location
     *            the location to navigate to, not <code>null</code>
     */
    public void setLocation(String location) {
        setLocation(location, null);
    }

    /**
     * Navigates the user's browser to a new location, optionally setting a
     * history state value that will be available if the user returns to the
     * same location. The location is relative to the base URI of the current
     * UI. New locations inside the application (i.e. a relative URL without
     * <code>..</code> segments) will immediately trigger a
     * {@link LocationChangeEvent} to be fired. New locations outside the
     * application (e.g. an absolute URL) causes the user to leave the
     * application without firing any {@link LocationChangeEvent}.
     *
     * @param location
     *            the location to navigate to, not <code>null</code>
     * @param state
     *            the history state to set, must be <code>null</code> if
     *            navigating to an external location
     */
    public void setLocation(String location, JsonObject state) {
        assert location != null;

        try {
            URI uri = new URI(location);
            String resolvedLocation = uri.getPath();

            boolean isExternal = uri.getHost() != null
                    || uri.getPath().startsWith("..")
                    || uri.getPath().startsWith("/");

            if (isExternal) {
                if (state != null) {
                    throw new IllegalArgumentException(
                            "state must be null for external locations");
                }
                executeJavaScript("window.location=$0", location);
            } else {
                getHistory().pushState(state, resolvedLocation);
                fireLocationChange(new Page.LocationChangeEvent(this, state,
                        resolvedLocation));
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Location cannot be parsed", e);
        }
    }

    /**
     * Internally updates the current location and fires the event.
     *
     * @param event
     *            the event with the new location
     */
    public void fireLocationChange(Page.LocationChangeEvent event) {
        assert event != null;
        assert event.getSource() == this;

        location = event.getLocation();

        eventRouter.fireEvent(event);
    }

    /**
     * Adds a listener that will be notified when the location changes.
     *
     * @see LocationChangeEvent
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     */
    public void addLocationChangeListener(LocationChangeListener listener) {
        assert listener != null;
        eventRouter.addListener(LocationChangeEvent.class, listener,
                locationChangeMethod);
    }

    /**
     * Removes a previously added location change listener.
     *
     * @param listener
     *            the listener to remove, not <code>null</code>
     */
    public void removeLocationChangeListener(LocationChangeListener listener) {
        assert listener != null;
        eventRouter.removeListener(LocationChangeEvent.class, listener,
                locationChangeMethod);
    }
}
