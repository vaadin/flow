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

package com.vaadin.client;

import java.util.HashMap;
import java.util.logging.Logger;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.vaadin.client.ApplicationConfiguration.ErrorMessage;
import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.communication.CommunicationProblemHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.ReconnectingCommunicationProblemHandler;
import com.vaadin.client.communication.ServerCommunicationHandler;
import com.vaadin.client.communication.ServerMessageHandler;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.componentlocator.ComponentLocator;
import com.vaadin.client.ui.FontIcon;
import com.vaadin.client.ui.Icon;
import com.vaadin.client.ui.ImageIcon;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.ui.UIConnector;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.Version;

/**
 * This is the client side communication "engine", managing client-server
 * communication with its server side counterpart
 * com.vaadin.server.VaadinService.
 *
 * Client-side connectors receive updates from the corresponding server-side
 * connector (typically component) as state updates or RPC calls. The connector
 * has the possibility to communicate back with its server side counter part
 * through RPC calls.
 *
 * TODO document better
 *
 * Entry point classes (widgetsets) define <code>onModuleLoad()</code>.
 */
public class ApplicationConnection implements HasHandlers {

    /**
     * A string that, if found in a non-JSON response to a UIDL request, will
     * cause the browser to refresh the page. If followed by a colon, optional
     * whitespace, and a URI, causes the browser to synchronously load the URI.
     *
     * <p>
     * This allows, for instance, a servlet filter to redirect the application
     * to a custom login page when the session expires. For example:
     * </p>
     *
     * <pre>
     * if (sessionExpired) {
     *     response.setHeader(&quot;Content-Type&quot;, &quot;text/html&quot;);
     *     response.getWriter().write(myLoginPageHtml + &quot;&lt;!-- Vaadin-Refresh: &quot;
     *             + request.getContextPath() + &quot; --&gt;&quot;);
     * }
     * </pre>
     */
    public static final String UIDL_REFRESH_TOKEN = "Vaadin-Refresh";

    private final HashMap<String, String> resourcesMap = new HashMap<String, String>();

    private final UIConnector uIConnector;

    protected boolean cssLoaded = false;

    /** Parameters for this application connection loaded from the web-page */
    private ApplicationConfiguration configuration;

    /** Event bus for communication events */
    private EventBus eventBus = GWT.create(SimpleEventBus.class);

    public enum State {
        INITIALIZING, RUNNING, TERMINATED;
    }

    private State state = State.INITIALIZING;

    /**
     * The communication handler methods are called at certain points during
     * communication with the server. This allows for making add-ons that keep
     * track of different aspects of the communication.
     */
    public interface CommunicationHandler extends EventHandler {
        void onRequestStarting(RequestStartingEvent e);

        void onResponseHandlingStarted(ResponseHandlingStartedEvent e);

        void onResponseHandlingEnded(ResponseHandlingEndedEvent e);
    }

    public static class RequestStartingEvent
            extends ApplicationConnectionEvent {

        public static Type<CommunicationHandler> TYPE = new Type<CommunicationHandler>();

        public RequestStartingEvent(ApplicationConnection connection) {
            super(connection);
        }

        @Override
        public Type<CommunicationHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(CommunicationHandler handler) {
            handler.onRequestStarting(this);
        }
    }

    public static class ResponseHandlingEndedEvent
            extends ApplicationConnectionEvent {

        public static Type<CommunicationHandler> TYPE = new Type<CommunicationHandler>();

        public ResponseHandlingEndedEvent(ApplicationConnection connection) {
            super(connection);
        }

        @Override
        public Type<CommunicationHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(CommunicationHandler handler) {
            handler.onResponseHandlingEnded(this);
        }
    }

    public static abstract class ApplicationConnectionEvent
            extends GwtEvent<CommunicationHandler> {

        private ApplicationConnection connection;

        protected ApplicationConnectionEvent(ApplicationConnection connection) {
            this.connection = connection;
        }

        public ApplicationConnection getConnection() {
            return connection;
        }

    }

    public static class ResponseHandlingStartedEvent
            extends ApplicationConnectionEvent {

        public ResponseHandlingStartedEvent(ApplicationConnection connection) {
            super(connection);
        }

        public static Type<CommunicationHandler> TYPE = new Type<CommunicationHandler>();

        @Override
        public Type<CommunicationHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(CommunicationHandler handler) {
            handler.onResponseHandlingStarted(this);
        }
    }

    /**
     * Event triggered when a application is stopped by calling
     * {@link ApplicationConnection#setApplicationRunning(false)}.
     *
     * To listen for the event add a {@link ApplicationStoppedHandler} by
     * invoking
     * {@link ApplicationConnection#addHandler(ApplicationConnection.ApplicationStoppedEvent.Type, ApplicationStoppedHandler)}
     * to the {@link ApplicationConnection}
     *
     * @since 7.1.8
     * @author Vaadin Ltd
     */
    public static class ApplicationStoppedEvent
            extends GwtEvent<ApplicationStoppedHandler> {

        public static Type<ApplicationStoppedHandler> TYPE = new Type<ApplicationStoppedHandler>();

        @Override
        public Type<ApplicationStoppedHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(ApplicationStoppedHandler listener) {
            listener.onApplicationStopped(this);
        }
    }

    /**
     * Allows custom handling of communication errors.
     */
    public interface CommunicationErrorHandler {
        /**
         * Called when a communication error has occurred. Returning
         * <code>true</code> from this method suppresses error handling.
         *
         * @param details
         *            A string describing the error.
         * @param statusCode
         *            The HTTP status code (e.g. 404, etc).
         * @return true if the error reporting should be suppressed, false to
         *         perform normal error reporting.
         */
        public boolean onError(String details, int statusCode);
    }

    /**
     * A listener for listening to application stopped events. The listener can
     * be added to a {@link ApplicationConnection} by invoking
     * {@link ApplicationConnection#addHandler(ApplicationStoppedEvent.Type, ApplicationStoppedHandler)}
     *
     * @since 7.1.8
     * @author Vaadin Ltd
     */
    public interface ApplicationStoppedHandler extends EventHandler {

        /**
         * Triggered when the {@link ApplicationConnection} marks a previously
         * running application as stopped by invoking
         * {@link ApplicationConnection#setApplicationRunning(false)}
         *
         * @param event
         *            the event triggered by the {@link ApplicationConnection}
         */
        void onApplicationStopped(ApplicationStoppedEvent event);
    }

    private CommunicationErrorHandler communicationErrorDelegate = null;

    private VLoadingIndicator loadingIndicator;

    private Heartbeat heartbeat = GWT.create(Heartbeat.class);

    private final VaadinUriResolver uriResolver = new VaadinUriResolver() {
        @Override
        protected String getVaadinDirUrl() {
            return getConfiguration().getVaadinDirUrl();
        }

        @Override
        protected String getServiceUrlParameterName() {
            return getConfiguration().getServiceUrlParameterName();
        }

        @Override
        protected String getServiceUrl() {
            return getConfiguration().getServiceUrl();
        }

        @Override
        protected String getThemeUri() {
            return ApplicationConnection.this.getThemeUri();
        }

        @Override
        protected String encodeQueryStringParameterValue(String queryString) {
            return URL.encodeQueryString(queryString);
        }
    };

    private Client currentClient;

    public static class MultiStepDuration extends Duration {
        private int previousStep = elapsedMillis();

        public void logDuration(String message) {
            logDuration(message, 0);
        }

        public void logDuration(String message, int minDuration) {
            int currentTime = elapsedMillis();
            int stepDuration = currentTime - previousStep;
            if (stepDuration >= minDuration) {
                getLogger().info(message + ": " + stepDuration + " ms");
            }
            previousStep = currentTime;
        }
    }

    public ApplicationConnection() {
        // Assuming UI data is eagerly loaded
        uIConnector = GWT.create(UIConnector.class);
        loadingIndicator = GWT.create(VLoadingIndicator.class);
        loadingIndicator.setConnection(this);
        serverRpcQueue = GWT.create(ServerRpcQueue.class);
        serverRpcQueue.setConnection(this);
        communicationProblemHandler = GWT
                .create(ReconnectingCommunicationProblemHandler.class);
        communicationProblemHandler.setConnection(this);
        serverMessageHandler = GWT.create(ServerMessageHandler.class);
        serverCommunicationHandler = GWT
                .create(ServerCommunicationHandler.class);
        serverCommunicationHandler.setConnection(this);
    }

    public void init(ApplicationConfiguration cnf) {
        getLogger().info("Starting application");
        getLogger().info("Using theme: " + cnf.getThemeName());

        getLogger().info("Vaadin application servlet version: "
                + cnf.getServletVersion());

        if (!cnf.getServletVersion().equals(Version.getFullVersion())) {
            getLogger()
                    .severe("Warning: your widget set seems to be built with a different "
                            + "version than the one used on server. Unexpected "
                            + "behavior may occur.");
        }

        configuration = cnf;

        ComponentLocator componentLocator = new ComponentLocator(this);

        String appId = cnf.getRootPanelId().replaceFirst("-\\d+$", "");
        // remove the end (window name) of autogenerated rootpanel id

        currentClient = initializeTestbenchHooks(componentLocator, appId);

        initializeClientHooks();

        // Remove the v-app-loading or any splash screen added inside the div by
        // the user
        // getContainerElement().setInnerHTML("");

        // Activate the initial theme by only adding the class name. Not calling
        // activateTheme here as it will also cause a full layout and updates to
        // the overlay container which has not yet been created at this point
        getContainerElement().addClassName(cnf.getThemeName());

        getLoadingIndicator().show();

        serverMessageHandler.setConnection(this);

        heartbeat.init(this);
    }

    public Element getContainerElement() {
        // TODO Embedded case
        return Document.get().getBody();
    }

    /**
     * Starts this application. Don't call this method directly - it's called by
     * {@link ApplicationConfiguration#startNextApplication()}, which should be
     * called once this application has started (first response received) or
     * failed to start. This ensures that the applications are started in order,
     * to avoid session-id problems.
     *
     */
    public void start() {
        String jsonText = configuration.getUIDL();
        if (jsonText == null) {
            // initial UIDL not in DOM, request from server
            getServerCommunicationHandler().resynchronize();
        } else {
            // initial UIDL provided in DOM, continue as if returned by request

            // Hack to avoid logging an error in endRequest()
            getServerCommunicationHandler().startRequest();
            getServerMessageHandler().handleMessage(jsonText);
        }
    }

    /**
     * Checks if there is some work to be done on the client side
     *
     * @return true if the client has some work to be done, false otherwise
     */
    private boolean isActive() {
        return !getServerMessageHandler().isInitialUidlHandled()
                || isWorkPending()
                || getServerCommunicationHandler().hasActiveRequest()
                || isExecutingDeferredCommands();
    }

    private native Client initializeTestbenchHooks(
            ComponentLocator componentLocator, String TTAppId)
            /*-{
            var ap = this;
            var client = {};
            client.isActive = $entry(function() {
            return ap.@com.vaadin.client.ApplicationConnection::isActive()();
            });
            var vi = ap.@com.vaadin.client.ApplicationConnection::getVersionInfo()();
            if (vi) {
            client.getVersionInfo = function() {
            return vi;
            }
            }
            
            client.modules = {};
            client.modules.publish = function(namespace,moduleCode) {
                var module = {};
                new Function("module", moduleCode).call(null, module);
                if (client.modules[namespace]) {
                    throw client.modules[namespace]+" is alredy defined";
                }
                client.modules[namespace] = module;
            }
            
            client.getProfilingData = $entry(function() {
            var smh = ap.@com.vaadin.client.ApplicationConnection::getServerMessageHandler();
            var pd = [
            smh.@com.vaadin.client.communication.ServerMessageHandler::lastProcessingTime,
            smh.@com.vaadin.client.communication.ServerMessageHandler::totalProcessingTime
            ];
            pd = pd.concat(smh.@com.vaadin.client.communication.ServerMessageHandler::serverTimingInfo);
            pd[pd.length] = smh.@com.vaadin.client.communication.ServerMessageHandler::bootstrapTime;
            return pd;
            });
            
            client.getElementByPath = $entry(function(id) {
            return componentLocator.@com.vaadin.client.componentlocator.ComponentLocator::getElementByPath(Ljava/lang/String;)(id);
            });
            client.getElementByPathStartingAt = $entry(function(id, element) {
            return componentLocator.@com.vaadin.client.componentlocator.ComponentLocator::getElementByPathStartingAt(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;)(id, element);
            });
            client.getElementsByPath = $entry(function(id) {
            return componentLocator.@com.vaadin.client.componentlocator.ComponentLocator::getElementsByPath(Ljava/lang/String;)(id);
            });
            client.getElementsByPathStartingAt = $entry(function(id, element) {
            return componentLocator.@com.vaadin.client.componentlocator.ComponentLocator::getElementsByPathStartingAt(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;)(id, element);
            });
            client.getPathForElement = $entry(function(element) {
            return componentLocator.@com.vaadin.client.componentlocator.ComponentLocator::getPathForElement(Lcom/google/gwt/dom/client/Element;)(element);
            });
            client.initializing = false;
            
            $wnd.vaadin.clients[TTAppId] = client;
            
            return client;
            }-*/;

    @JsType
    public interface Client {
        @JsProperty
        public JavaScriptObject getModules();
    }

    public Client getCurrentClient() {
        return currentClient;
    }

    /**
     * Helper for tt initialization
     */
    private JavaScriptObject getVersionInfo() {
        return configuration.getVersionInfoJSObject();
    }

    /**
     * Publishes a JavaScript API for mash-up applications.
     * <ul>
     * <li><code>vaadin.forceSync()</code> sends pending variable changes, in
     * effect synchronizing the server and client state. This is done for all
     * applications on host page.</li>
     * <li><code>vaadin.postRequestHooks</code> is a map of functions which gets
     * called after each XHR made by vaadin application. Note, that it is
     * attaching js functions responsibility to create the variable like this:
     *
     * <code><pre>
     * if(!vaadin.postRequestHooks) {vaadin.postRequestHooks = new Object();}
     * postRequestHooks.myHook = function(appId) {
     *          if(appId == "MyAppOfInterest") {
     *                  // do the staff you need on xhr activity
     *          }
     * }
     * </pre></code> First parameter passed to these functions is the identifier
     * of Vaadin application that made the request.
     * </ul>
     *
     * TODO make this multi-app aware
     */
    private native void initializeClientHooks()
    /*-{
    	var app = this;
    	var oldSync;
    	if ($wnd.vaadin.forceSync) {
    		oldSync = $wnd.vaadin.forceSync;
    	}
    	$wnd.vaadin.forceSync = $entry(function() {
    		if (oldSync) {
    			oldSync();
    		}
    		app.@com.vaadin.client.ApplicationConnection::getServerRpcQueue()().@com.vaadin.client.communication.ServerRpcQueue::flush()();
    	});
    }-*/;

    /**
     * Runs possibly registered client side post request hooks. This is expected
     * to be run after each uidl request made by Vaadin application.
     *
     * @param appId
     */
    public static native void runPostRequestHooks(String appId)
    /*-{
    	if ($wnd.vaadin.postRequestHooks) {
    		for ( var hook in $wnd.vaadin.postRequestHooks) {
    			if (typeof ($wnd.vaadin.postRequestHooks[hook]) == "function") {
    				try {
    					$wnd.vaadin.postRequestHooks[hook](appId);
    				} catch (e) {
    				}
    			}
    		}
    	}
    }-*/;

    /**
     * If on Liferay and logged in, ask the client side session management
     * JavaScript to extend the session duration.
     *
     * Otherwise, Liferay client side JavaScript will explicitly expire the
     * session even though the server side considers the session to be active.
     * See ticket #8305 for more information.
     */
    public static native void extendLiferaySession()
    /*-{
    if ($wnd.Liferay && $wnd.Liferay.Session) {
        $wnd.Liferay.Session.extend();
        // if the extend banner is visible, hide it
        if ($wnd.Liferay.Session.banner) {
            $wnd.Liferay.Session.banner.remove();
        }
    }
    }-*/;

    int cssWaits = 0;

    protected ServerRpcQueue serverRpcQueue;
    protected CommunicationProblemHandler communicationProblemHandler;
    protected ServerMessageHandler serverMessageHandler;
    protected ServerCommunicationHandler serverCommunicationHandler;

    static final int MAX_CSS_WAITS = 100;

    public void executeWhenCSSLoaded(final Command c) {
        if (!isCSSLoaded() && cssWaits < MAX_CSS_WAITS) {
            (new Timer() {
                @Override
                public void run() {
                    executeWhenCSSLoaded(c);
                }
            }).schedule(50);

            // Show this message just once
            if (cssWaits++ == 0) {
                getLogger().warning("Assuming CSS loading is not complete, "
                        + "postponing render phase. "
                        + "(.v-loading-indicator height == 0)");
            }
        } else {
            cssLoaded = true;
            if (cssWaits >= MAX_CSS_WAITS) {
                getLogger().severe("CSS files may have not loaded properly.");
            }

            c.execute();
        }
    }

    /**
     * Checks whether or not the CSS is loaded. By default checks the size of
     * the loading indicator element.
     *
     * @return
     */
    protected boolean isCSSLoaded() {
        return cssLoaded
                || getLoadingIndicator().getElement().getOffsetHeight() != 0;
    }

    /**
     * Shows the communication error notification.
     *
     * @param details
     *            Optional details.
     * @param statusCode
     *            The status code returned for the request
     *
     */
    public void showCommunicationError(String details, int statusCode) {
        getLogger().severe("Communication error: " + details);
        showError(details, configuration.getCommunicationError());
    }

    /**
     * Shows the authentication error notification.
     *
     * @param details
     *            Optional details.
     */
    public void showAuthenticationError(String details) {
        getLogger().severe("Authentication error: " + details);
        showError(details, configuration.getAuthorizationError());
    }

    /**
     * Shows the session expiration notification.
     *
     * @param details
     *            Optional details.
     */
    public void showSessionExpiredError(String details) {
        getLogger().severe("Session expired: " + details);
        showError(details, configuration.getSessionExpiredError());
    }

    /**
     * Shows an error notification.
     *
     * @param details
     *            Optional details.
     * @param message
     *            An ErrorMessage describing the error.
     */
    protected void showError(String details, ErrorMessage message) {
        VNotification.showError(this, message.getCaption(),
                message.getMessage(), details, message.getUrl());
    }

    /**
     * Checks if the client has running or scheduled commands
     */
    private boolean isWorkPending() {
        ConnectorMap connectorMap = getConnectorMap();
        JsArrayObject<ServerConnector> connectors = connectorMap
                .getConnectorsAsJsArray();
        int size = connectors.size();
        for (int i = 0; i < size; i++) {
            ServerConnector conn = connectors.get(i);
            if (isWorkPending(conn)) {
                return true;
            }

            if (conn instanceof ComponentConnector) {
                ComponentConnector compConn = (ComponentConnector) conn;
                if (isWorkPending(compConn.getWidget())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWorkPending(Object object) {
        return object instanceof DeferredWorker
                && ((DeferredWorker) object).isWorkPending();
    }

    /**
     * Checks if deferred commands are (potentially) still being executed as a
     * result of an update from the server. Returns true if a deferred command
     * might still be executing, false otherwise. This will not work correctly
     * if a deferred command is added in another deferred command.
     * <p>
     * Used by the native "client.isActive" function.
     * </p>
     *
     * @return true if deferred commands are (potentially) being executed, false
     *         otherwise
     */
    private boolean isExecutingDeferredCommands() {
        Scheduler s = Scheduler.get();
        if (s instanceof VSchedulerImpl) {
            return ((VSchedulerImpl) s).hasWorkQueued();
        } else {
            return false;
        }
    }

    /**
     * Returns the loading indicator used by this ApplicationConnection
     *
     * @return The loading indicator for this ApplicationConnection
     */
    public VLoadingIndicator getLoadingIndicator() {
        return loadingIndicator;
    }

    public void loadStyleDependencies(JsArrayString dependencies) {
        // Assuming no reason to interpret in a defined order
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                ApplicationConfiguration.endDependencyLoading();
            }

            @Override
            public void onError(ResourceLoadEvent event) {
                getLogger().severe(event.getResourceUrl()
                        + " could not be loaded, or the load detection failed because the stylesheet is empty.");
                // The show must go on
                onLoad(event);
            }
        };
        ResourceLoader loader = ResourceLoader.get();
        for (int i = 0; i < dependencies.length(); i++) {
            String url = translateVaadinUri(dependencies.get(i));
            getLogger().info("Loading stylesheet dependency from " + url);
            ApplicationConfiguration.startDependencyLoading();
            loader.loadStylesheet(url, resourceLoadListener);
        }
    }

    public void loadHtmlDependencies(JsArrayString dependencies) {
        if (dependencies.length() == 0) {
            return;
        }

        ResourceLoader loader = ResourceLoader.get();

        // Load all at once
        for (int i = 0; i < dependencies.length(); i++) {
            String url = translateVaadinUri(dependencies.get(i));
            getLogger().info("Loading HTML dependency from " + url);
            ApplicationConfiguration.startDependencyLoading();
            loader.loadHtml(url, new ResourceLoadListener() {
                @Override
                public void onLoad(ResourceLoadEvent event) {
                    ApplicationConfiguration.endDependencyLoading();
                }

                @Override
                public void onError(ResourceLoadEvent event) {
                    getLogger().severe(
                            event.getResourceUrl() + " could not be loaded.");
                    // The show must go on
                    onLoad(event);
                }
            });
        }
    }

    public void loadScriptDependencies(final JsArrayString dependencies) {
        if (dependencies.length() == 0) {
            return;
        }

        // Listener that loads the next when one is completed
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                if (dependencies.length() != 0) {
                    String url = translateVaadinUri(dependencies.shift());
                    ApplicationConfiguration.startDependencyLoading();
                    // Load next in chain (hopefully already preloaded)
                    event.getResourceLoader().loadScript(url, this);
                }
                // Call start for next before calling end for current
                ApplicationConfiguration.endDependencyLoading();
            }

            @Override
            public void onError(ResourceLoadEvent event) {
                getLogger().severe(
                        event.getResourceUrl() + " could not be loaded.");
                // The show must go on
                onLoad(event);
            }
        };

        ResourceLoader loader = ResourceLoader.get();

        // Start chain by loading first
        String url = translateVaadinUri(dependencies.shift());
        getLogger().info("Loading script dependency from " + url);
        ApplicationConfiguration.startDependencyLoading();
        loader.loadScript(url, resourceLoadListener);

        if (ResourceLoader.supportsInOrderScriptExecution()) {
            for (int i = 0; i < dependencies.length(); i++) {
                String preloadUrl = translateVaadinUri(dependencies.get(i));
                loader.loadScript(preloadUrl, null);
            }
        } else {
            // Preload all remaining
            for (int i = 0; i < dependencies.length(); i++) {
                String preloadUrl = translateVaadinUri(dependencies.get(i));
                loader.preloadResource(preloadUrl, null);
            }
        }
    }

    /**
     * Get either an existing ComponentConnector or create a new
     * ComponentConnector with the given type and id.
     *
     * If a ComponentConnector with the given id already exists, returns it.
     * Otherwise creates and registers a new ComponentConnector of the given
     * type.
     *
     * @param connectorId
     *            Id of the paintable
     * @param connectorType
     *            Type of the connector, as passed from the server side
     *
     * @return Either an existing ComponentConnector or a new ComponentConnector
     *         of the given type
     */
    public ServerConnector getConnector(String connectorId, int connectorType) {
        if (!connectorMap.hasConnector(connectorId)) {
            return createAndRegisterConnector(connectorId, connectorType);
        }
        return connectorMap.getConnector(connectorId);
    }

    /**
     * Creates a new ServerConnector with the given type and id.
     *
     * Creates and registers a new ServerConnector of the given type. Should
     * never be called with the connector id of an existing connector.
     *
     * @param connectorId
     *            Id of the new connector
     * @param connectorType
     *            Type of the connector, as passed from the server side
     *
     * @return A new ServerConnector of the given type
     */
    private ServerConnector createAndRegisterConnector(String connectorId,
            int connectorType) {
        return null;
    }

    /**
     * Gets a resource that has been pre-loaded via UIDL, such as custom
     * layouts.
     *
     * @param name
     *            identifier of the resource to get
     * @return the resource
     */
    public String getResource(String name) {
        return resourcesMap.get(name);
    }

    /**
     * Sets a resource that has been pre-loaded via UIDL, such as custom
     * layouts.
     *
     * @param name
     *            identifier of the resource to Set
     * @param resource
     *            the resource
     */
    public void setResource(String name, String resource) {
        resourcesMap.put(name, resource);
    }

    /**
     * Gets an {@link Icon} instance corresponding to a URI.
     *
     * @since 7.2
     * @param uri
     * @return Icon object
     */
    public Icon getIcon(String uri) {
        Icon icon;
        if (uri == null) {
            return null;
        } else if (FontIcon.isFontIconUri(uri)) {
            icon = GWT.create(FontIcon.class);
        } else {
            icon = GWT.create(ImageIcon.class);
        }
        icon.setUri(translateVaadinUri(uri));
        return icon;
    }

    /**
     * Translates custom protocols in UIDL URI's to be recognizable by browser.
     * All uri's from UIDL should be routed via this method before giving them
     * to browser due URI's in UIDL may contain custom protocols like theme://.
     *
     * @param uidlUri
     *            Vaadin URI from uidl
     * @return translated URI ready for browser
     */
    public String translateVaadinUri(String uidlUri) {
        return uriResolver.resolveVaadinUri(uidlUri);
    }

    /**
     * Gets the URI for the current theme. Can be used to reference theme
     * resources.
     *
     * @return URI to the current theme
     */
    public String getThemeUri() {
        return configuration.getVaadinDirUrl() + "themes/"
                + getUIConnector().getActiveTheme();
    }

    private ConnectorMap connectorMap = GWT.create(ConnectorMap.class);

    /**
     * Gets the main view
     *
     * @return the main view
     */
    public UIConnector getUIConnector() {
        return uIConnector;
    }

    /**
     * Gets the {@link ApplicationConfiguration} for the current application.
     *
     * @see ApplicationConfiguration
     * @return the configuration for this application
     */
    public ApplicationConfiguration getConfiguration() {
        return configuration;
    }

    ConnectorMap getConnectorMap() {
        return connectorMap;
    }

    public void handleCommunicationError(String details, int statusCode) {
        boolean handled = false;
        if (communicationErrorDelegate != null) {
            handled = communicationErrorDelegate.onError(details, statusCode);

        }

        if (!handled) {
            showCommunicationError(details, statusCode);
        }

    }

    /**
     * Sets the delegate that is called whenever a communication error occurrs.
     *
     * @param delegate
     *            the delegate.
     */
    public void setCommunicationErrorDelegate(
            CommunicationErrorHandler delegate) {
        communicationErrorDelegate = delegate;
    }

    public void setApplicationRunning(boolean applicationRunning) {
        if (getState() == State.TERMINATED) {
            if (applicationRunning) {
                getLogger().severe(
                        "Tried to restart a terminated application. This is not supported");
            } else {
                getLogger().warning(
                        "Tried to stop a terminated application. This should not be done");
            }
            return;
        } else if (getState() == State.INITIALIZING) {
            if (applicationRunning) {
                state = State.RUNNING;
            } else {
                getLogger().warning(
                        "Tried to stop the application before it has started. This should not be done");
            }
        } else if (getState() == State.RUNNING) {
            if (!applicationRunning) {
                state = State.TERMINATED;
                eventBus.fireEvent(new ApplicationStoppedEvent());
            } else {
                getLogger().warning(
                        "Tried to start an already running application. This should not be done");
            }
        }
    }

    /**
     * Checks if the application is in the {@link State#RUNNING} state.
     *
     * @since
     * @return true if the application is in the running state, false otherwise
     */
    public boolean isApplicationRunning() {
        return state == State.RUNNING;
    }

    public <H extends EventHandler> HandlerRegistration addHandler(
            GwtEvent.Type<H> type, H handler) {
        return eventBus.addHandler(type, handler);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }

    /**
     * Calls {@link ComponentConnector#flush()} on the active connector. Does
     * nothing if there is no active (focused) connector.
     */
    public void flushActiveConnector() {
        ComponentConnector activeConnector = getActiveConnector();
        if (activeConnector == null) {
            return;
        }
        activeConnector.flush();
    }

    /**
     * Gets the active connector for focused element in browser.
     *
     * @return Connector for focused element or null.
     */
    private ComponentConnector getActiveConnector() {
        Element focusedElement = WidgetUtil.getFocusedElement();
        if (focusedElement == null) {
            return null;
        }
        return Util.getConnectorForElement(this, getUIConnector().getWidget(),
                focusedElement);
    }

    private static Logger getLogger() {
        return Logger.getLogger(ApplicationConnection.class.getName());
    }

    /**
     * Returns the hearbeat instance.
     */
    public Heartbeat getHeartbeat() {
        return heartbeat;
    }

    /**
     * Returns the state of this application. An application state goes from
     * "initializing" to "running" to "stopped". There is no way for an
     * application to go back to a previous state, i.e. a stopped application
     * can never be re-started
     *
     * @since
     * @return the current state of this application
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the server RPC queue for this application
     *
     * @return the server RPC queue
     */
    public ServerRpcQueue getServerRpcQueue() {
        return serverRpcQueue;
    }

    /**
     * Gets the communication error handler for this application
     *
     * @return the server RPC queue
     */
    public CommunicationProblemHandler getCommunicationProblemHandler() {
        return communicationProblemHandler;
    }

    /**
     * Gets the server message handler for this application
     *
     * @return the server message handler
     */
    public ServerMessageHandler getServerMessageHandler() {
        return serverMessageHandler;
    }

    /**
     * Gets the server communication handler for this application
     *
     * @return the server communication handler
     */
    public ServerCommunicationHandler getServerCommunicationHandler() {
        return serverCommunicationHandler;
    }

    public int getLastSeenServerSyncId() {
        return getServerMessageHandler().getLastSeenServerSyncId();
    }

}
