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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.vaadin.event.EventRouter;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.ui.ui.PageClientRpc;
import com.vaadin.shared.ui.ui.PageState;
import com.vaadin.shared.ui.ui.UIState;
import com.vaadin.util.ReflectTools;

public class Page implements Serializable {

    /**
     * Listener that gets notified when the size of the browser window
     * containing the uI has changed.
     * 
     * @see UI#addListener(BrowserWindowResizeListener)
     */
    public interface BrowserWindowResizeListener extends Serializable {
        /**
         * Invoked when the browser window containing a UI has been resized.
         * 
         * @param event
         *            a browser window resize event
         */
        public void browserWindowResized(BrowserWindowResizeEvent event);
    }

    /**
     * Event that is fired when a browser window containing a uI is resized.
     */
    public static class BrowserWindowResizeEvent extends EventObject {

        private final int width;
        private final int height;

        /**
         * Creates a new event
         * 
         * @param source
         *            the uI for which the browser window has been resized
         * @param width
         *            the new width of the browser window
         * @param height
         *            the new height of the browser window
         */
        public BrowserWindowResizeEvent(Page source, int width, int height) {
            super(source);
            this.width = width;
            this.height = height;
        }

        @Override
        public Page getSource() {
            return (Page) super.getSource();
        }

        /**
         * Gets the new browser window height
         * 
         * @return an integer with the new pixel height of the browser window
         */
        public int getHeight() {
            return height;
        }

        /**
         * Gets the new browser window width
         * 
         * @return an integer with the new pixel width of the browser window
         */
        public int getWidth() {
            return width;
        }
    }

    private static final Method BROWSER_RESIZE_METHOD = ReflectTools.findMethod(BrowserWindowResizeListener.class, "browserWindowResized", BrowserWindowResizeEvent.class);

    /**
     * Listener that that gets notified when the URI fragment of the page
     * changes.
     * 
     * @see Page#addUriFragmentChangedListener(UriFragmentChangedListener)
     */
    public interface UriFragmentChangedListener extends Serializable {
        /**
         * Event handler method invoked when the URI fragment of the page
         * changes. Please note that the initial URI fragment has already been
         * set when a new UI is initialized, so there will not be any initial
         * event for listeners added during {@link UI#init(VaadinRequest)}.
         * 
         * @see Page#addUriFragmentChangedListener(UriFragmentChangedListener)
         * 
         * @param event
         *            the URI fragment changed event
         */
        public void uriFragmentChanged(UriFragmentChangedEvent event);
    }

    private static final Method URI_FRAGMENT_CHANGED_METHOD = ReflectTools.findMethod(Page.UriFragmentChangedListener.class, "uriFragmentChanged", UriFragmentChangedEvent.class);

    /**
     * A list of notifications that are waiting to be sent to the client.
     * Cleared (set to null) when the notifications have been sent.
     */
    private List<Notification> notifications;

    /**
     * Event fired when the URI fragment of a <code>Page</code> changes.
     * 
     * @see Page#addUriFragmentChangedListener(UriFragmentChangedListener)
     */
    public static class UriFragmentChangedEvent extends EventObject {

        /**
         * The new URI fragment
         */
        private final String uriFragment;

        /**
         * Creates a new instance of UriFragmentReader change event.
         * 
         * @param source
         *            the Source of the event.
         * @param uriFragment
         *            the new uriFragment
         */
        public UriFragmentChangedEvent(Page source, String uriFragment) {
            super(source);
            this.uriFragment = uriFragment;
        }

        /**
         * Gets the page in which the fragment has changed.
         * 
         * @return the page in which the fragment has changed
         */
        public Page getPage() {
            return (Page) getSource();
        }

        /**
         * Get the new URI fragment
         * 
         * @return the new fragment
         */
        public String getUriFragment() {
            return uriFragment;
        }
    }

    private EventRouter eventRouter;

    private final UI uI;

    private int browserWindowWidth = -1;
    private int browserWindowHeight = -1;

    private JavaScript javaScript;

    /**
     * The current browser location.
     */
    private URI location;

    private final PageState state;

    private String windowName;

    public Page(UI uI, PageState state) {
        this.uI = uI;
        this.state = state;
    }

    private void addListener(Class<?> eventType, Object target, Method method) {
        if (!hasEventRouter()) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, method);
    }

    private void removeListener(Class<?> eventType, Object target, Method method) {
        if (hasEventRouter()) {
            eventRouter.removeListener(eventType, target, method);
        }
    }

    /**
     * Adds a listener that gets notified every time the URI fragment of this
     * page is changed. Please note that the initial URI fragment has already
     * been set when a new UI is initialized, so there will not be any initial
     * event for listeners added during {@link UI#init(VaadinRequest)}.
     * 
     * @see #getUriFragment()
     * @see #setUriFragment(String)
     * @see #removeUriFragmentChangedListener(UriFragmentChangedListener)
     * 
     * @param listener
     *            the URI fragment listener to add
     */
    public void addUriFragmentChangedListener(Page.UriFragmentChangedListener listener) {
        addListener(UriFragmentChangedEvent.class, listener, URI_FRAGMENT_CHANGED_METHOD);
    }

    /**
     * Removes a URI fragment listener that was previously added to this page.
     * 
     * @param listener
     *            the URI fragment listener to remove
     * 
     * @see Page#addUriFragmentChangedListener(UriFragmentChangedListener)
     */
    public void removeUriFragmentChangedListener(Page.UriFragmentChangedListener listener) {
        removeListener(UriFragmentChangedEvent.class, listener, URI_FRAGMENT_CHANGED_METHOD);
    }

    /**
     * Sets the fragment part in the current location URI. Optionally fires a
     * {@link UriFragmentChangedEvent}.
     * <p>
     * The fragment is the optional last component of a URI, prefixed with a
     * hash sign ("#").
     * <p>
     * Passing an empty string as <code>newFragment</code> sets an empty
     * fragment (a trailing "#" in the URI.) Passing <code>null</code> if there
     * is already a non-null fragment will leave a trailing # in the URI since
     * removing it would cause the browser to reload the page. This is not fully
     * consistent with the semantics of {@link java.net.URI}.
     * 
     * @param newUriFragment
     *            The new fragment.
     * @param fireEvents
     *            true to fire event
     * 
     * @see #getUriFragment()
     * @see #setLocation(URI)
     * @see UriFragmentChangedEvent
     * @see Page.UriFragmentChangedListener
     * 
     */
    public void setUriFragment(String newUriFragment, boolean fireEvents) {
        String oldUriFragment = location.getFragment();
        if (newUriFragment == null && getUriFragment() != null) {
            // Can't completely remove the fragment once it has been set, will
            // instead set it to the empty string
            newUriFragment = "";
        }
        if (newUriFragment == oldUriFragment || (newUriFragment != null && newUriFragment.equals(oldUriFragment))) {
            return;
        }
        try {
            location = new URI(location.getScheme(), location.getSchemeSpecificPart(), newUriFragment);
        } catch (URISyntaxException e) {
            // This should not actually happen as the fragment syntax is not
            // constrained
            throw new RuntimeException(e);
        }
        if (fireEvents) {
            fireEvent(new UriFragmentChangedEvent(this, newUriFragment));
        }
        uI.markAsDirty();
    }

    private void fireEvent(EventObject event) {
        if (hasEventRouter()) {
            eventRouter.fireEvent(event);
        }
    }

    /**
     * Sets URI fragment. This method fires a {@link UriFragmentChangedEvent}
     * 
     * @param newUriFragment
     *            id of the new fragment
     * @see UriFragmentChangedEvent
     * @see Page.UriFragmentChangedListener
     */
    public void setUriFragment(String newUriFragment) {
        setUriFragment(newUriFragment, true);
    }

    /**
     * Gets the currently set URI fragment.
     * <p>
     * Returns <code>null</code> if there is no fragment and an empty string if
     * there is an empty fragment.
     * <p>
     * To listen to changes in fragment, hook a
     * {@link Page.UriFragmentChangedListener}.
     * 
     * @return the current fragment in browser location URI.
     * 
     * @see #getLocation()
     * @see #setUriFragment(String)
     * @see #addUriFragmentChangedListener(UriFragmentChangedListener)
     */
    public String getUriFragment() {
        return location.getFragment();
    }

    public void init(VaadinRequest request) {
        // NOTE: UI.refresh makes assumptions about the semantics of this
        // method.
        // It should be kept in sync if this method is changed.

        // Extract special parameter sent by vaadinBootstrap.js
        String location = request.getParameter("v-loc");
        String clientWidth = request.getParameter("v-cw");
        String clientHeight = request.getParameter("v-ch");
        windowName = request.getParameter("v-wn");

        if (location != null) {
            try {
                this.location = new URI(location);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid location URI received from client", e);
            }
        }
        if (clientWidth != null && clientHeight != null) {
            try {
                browserWindowWidth = Integer.parseInt(clientWidth);
                browserWindowHeight = Integer.parseInt(clientHeight);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid window size received from client", e);
            }
        }
    }

    public WebBrowser getWebBrowser() {
        return uI.getSession().getBrowser();
    }

    /**
     * Gets the window.name value of the browser window of this page.
     * 
     * @since 7.2
     * 
     * @return the window name, <code>null</code> if the name is not known
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * For internal use only. Updates the internal state with the given values.
     * Does not resize the Page or browser window.
     * 
     * @since 7.2
     * 
     * @param width
     *            the new browser window width
     * @param height
     *            the new browser window height
     * @param fireEvents
     *            whether to fire {@link BrowserWindowResizeEvent} if the size
     *            changes
     */
    public void updateBrowserWindowSize(int width, int height, boolean fireEvents) {
        boolean sizeChanged = false;

        if (width != browserWindowWidth) {
            browserWindowWidth = width;
            sizeChanged = true;
        }

        if (height != browserWindowHeight) {
            browserWindowHeight = height;
            sizeChanged = true;
        }

        if (fireEvents && sizeChanged) {
            fireEvent(new BrowserWindowResizeEvent(this, browserWindowWidth, browserWindowHeight));
        }

    }

    /**
     * Adds a new {@link BrowserWindowResizeListener} to this UI. The listener
     * will be notified whenever the browser window within which this UI resides
     * is resized.
     * <p>
     * In most cases, the UI should be in lazy resize mode when using browser
     * window resize listeners. Otherwise, a large number of events can be
     * received while a resize is being performed. Use
     * {@link UI#setResizeLazy(boolean)}.
     * </p>
     * 
     * @param resizeListener
     *            the listener to add
     * 
     * @see BrowserWindowResizeListener#browserWindowResized(BrowserWindowResizeEvent)
     * @see UI#setResizeLazy(boolean)
     */
    public void addBrowserWindowResizeListener(BrowserWindowResizeListener resizeListener) {
        addListener(BrowserWindowResizeEvent.class, resizeListener, BROWSER_RESIZE_METHOD);
        getState(true).hasResizeListeners = true;
    }

    /**
     * Removes a {@link BrowserWindowResizeListener} from this UI. The listener
     * will no longer be notified when the browser window is resized.
     * 
     * @param resizeListener
     *            the listener to remove
     */
    public void removeBrowserWindowResizeListener(BrowserWindowResizeListener resizeListener) {
        removeListener(BrowserWindowResizeEvent.class, resizeListener, BROWSER_RESIZE_METHOD);
        getState(true).hasResizeListeners = hasEventRouter() && eventRouter.hasListeners(BrowserWindowResizeEvent.class);
    }

    /**
     * Gets the last known height of the browser window in which this UI
     * resides.
     * 
     * @return the browser window height in pixels
     */
    public int getBrowserWindowHeight() {
        return browserWindowHeight;
    }

    /**
     * Gets the last known width of the browser window in which this uI resides.
     * 
     * @return the browser window width in pixels
     */
    public int getBrowserWindowWidth() {
        return browserWindowWidth;
    }

    public JavaScript getJavaScript() {
        if (javaScript == null) {
            // Create and attach on first use
            javaScript = new JavaScript(uI);
        }
        return javaScript;
    }

    /**
     * Navigates this page to the given URI. The contents of this page in the
     * browser is replaced with whatever is returned for the given URI.
     * <p>
     * This method should not be used to start downloads, as the client side
     * will assume the browser will navigate away when opening the URI. Use one
     * of the {@code Page.open} methods or {@code FileDownloader} instead.
     * 
     * @see #open(String, String)
     * @see FileDownloader
     * 
     * @param uri
     *            the URI to show
     */
    public void setLocation(String uri) {
        // FIXME
        uI.markAsDirty();
    }

    /**
     * Navigates this page to the given URI. The contents of this page in the
     * browser is replaced with whatever is returned for the given URI.
     * <p>
     * This method should not be used to start downloads, as the client side
     * will assume the browser will navigate away when opening the URI. Use one
     * of the {@code Page.open} methods or {@code FileDownloader} instead.
     * 
     * @see #open(String, String)
     * @see FileDownloader
     * 
     * @param uri
     *            the URI to show
     */
    public void setLocation(URI uri) {
        setLocation(uri.toString());
    }

    /**
     * Returns the location URI of this page, as reported by the browser. Note
     * that this may not be consistent with the server URI the application is
     * deployed in due to potential proxies, redirections and similar.
     * 
     * @return The browser location URI.
     */
    public URI getLocation() {
        if (location == null && !uI.getSession().getConfiguration().isSendUrlsAsParameters()) {
            throw new IllegalStateException("Location is not available as the " + Constants.SERVLET_PARAMETER_SENDURLSASPARAMETERS + " parameter is configured as false");
        }
        return location;
    }

    /**
     * For internal use only. Used to update the server-side location when the
     * client-side location changes.
     * 
     * @deprecated As of 7.2, use {@link #updateLocation(String, boolean)}
     *             instead.
     * 
     * @param location
     *            the new location URI
     */
    @Deprecated
    public void updateLocation(String location) {
        updateLocation(location, true);
    }

    /**
     * For internal use only. Used to update the server-side location when the
     * client-side location changes.
     * 
     * @since 7.2
     * 
     * @param location
     *            the new location URI
     * @param fireEvents
     *            whether to fire {@link UriFragmentChangedEvent} if the URI
     *            fragment changes
     */
    public void updateLocation(String location, boolean fireEvents) {
        try {
            String oldUriFragment = this.location.getFragment();
            this.location = new URI(location);
            String newUriFragment = this.location.getFragment();
            if (fireEvents && !Objects.equals(oldUriFragment, newUriFragment)) {
                fireEvent(new UriFragmentChangedEvent(this, newUriFragment));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal helper method to actually add a notification.
     * 
     * @param notification
     *            the notification to add
     */
    private void addNotification(Notification notification) {
        if (notifications == null) {
            notifications = new LinkedList<Notification>();
        }
        notifications.add(notification);
        uI.markAsDirty();
    }

    /**
     * Shows a notification message.
     * 
     * @see Notification
     * 
     * @param notification
     *            The notification message to show
     * 
     * @deprecated As of 7.0, use Notification.show(Page) instead.
     */
    @Deprecated
    public void showNotification(Notification notification) {
        addNotification(notification);
    }

    /**
     * Gets the Page to which the current uI belongs. This is automatically
     * defined when processing requests to the server. In other cases, (e.g.
     * from background threads), the current uI is not automatically defined.
     * 
     * @see UI#getCurrent()
     * 
     * @return the current page instance if available, otherwise
     *         <code>null</code>
     */
    public static Page getCurrent() {
        UI currentUI = UI.getCurrent();
        if (currentUI == null) {
            return null;
        }
        return currentUI.getPage();
    }

    /**
     * Sets the page title. The page title is displayed by the browser e.g. as
     * the title of the browser window or as the title of the tab.
     * <p>
     * If the title is set to null, it will not left as-is. Set to empty string
     * to clear the title.
     * 
     * @param title
     *            the page title to set
     */
    public void setTitle(String title) {
        getState(true).title = title;
    }

    /**
     * Reloads the page in the browser.
     */
    public void reload() {
        uI.getRpcProxy(PageClientRpc.class).reload();
    }

    /**
     * Returns the page state.
     * <p>
     * The page state is transmitted to UIConnector together with
     * {@link UIState} rather than as an individual entity.
     * </p>
     * <p>
     * The state should be considered an internal detail of Page. Classes
     * outside of Page should not access it directly but only through public
     * APIs provided by Page.
     * </p>
     * 
     * @since 7.1
     * @param markAsDirty
     *            true to mark the state as dirty
     * @return PageState object that can be read in any case and modified if
     *         markAsDirty is true
     */
    protected PageState getState(boolean markAsDirty) {
        if (markAsDirty) {
            uI.markAsDirty();
        }
        return state;
    }

    private boolean hasEventRouter() {
        return eventRouter != null;
    }
}
