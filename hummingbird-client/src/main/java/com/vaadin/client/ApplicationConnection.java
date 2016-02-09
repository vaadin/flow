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

package com.vaadin.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.http.client.URL;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.ApplicationConfiguration.ErrorMessage;
import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;
import com.vaadin.client.hummingbird.BasicElementBinder;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.Version;
import com.vaadin.shared.ui.ui.UIState.PushConfigurationState;
import com.vaadin.shared.ui.ui.UIState.ReconnectDialogConfigurationState;

import elemental.client.Browser;
import elemental.dom.Element;

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
public class ApplicationConnection {

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

    protected boolean cssLoaded = false;

    /** Parameters for this application connection loaded from the web-page. */
    private ApplicationConfiguration configuration;

    /** Event bus for communication events. */
    private EventBus eventBus = GWT.create(SimpleEventBus.class);

    public enum ApplicationState {
        INITIALIZING, RUNNING, TERMINATED;
    }

    private ApplicationState applicationState = ApplicationState.INITIALIZING;

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

        public static final Type<CommunicationHandler> TYPE = new Type<CommunicationHandler>();

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

        public static final Type<CommunicationHandler> TYPE = new Type<CommunicationHandler>();

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
            extends Event<CommunicationHandler> {

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

        public static final Type<CommunicationHandler> TYPE = new Type<CommunicationHandler>();

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
            extends Event<ApplicationStoppedHandler> {

        public static final Type<ApplicationStoppedHandler> TYPE = new Type<ApplicationStoppedHandler>();

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
         * {@link ApplicationConnection#setApplicationRunning(false)}.
         *
         * @param event
         *            the event triggered by the {@link ApplicationConnection}
         */
        void onApplicationStopped(ApplicationStoppedEvent event);
    }

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
        protected String encodeQueryStringParameterValue(String queryString) {
            return URL.encodeQueryString(queryString);
        }
    };

    public static class MultiStepDuration extends Duration {
        private int previousStep = elapsedMillis();

        public void logDuration(String message) {
            logDuration(message, 0);
        }

        public void logDuration(String message, int minDuration) {
            int currentTime = elapsedMillis();
            int stepDuration = currentTime - previousStep;
            if (stepDuration >= minDuration) {
                Console.log(message + ": " + stepDuration + " ms");
            }
            previousStep = currentTime;
        }
    }

    public ApplicationConnection() {
        loadingIndicator = GWT.create(VLoadingIndicator.class);
        connectionStateHandler = GWT.create(ConnectionStateHandler.class);
        messageHandler = GWT.create(MessageHandler.class);
        messageSender = GWT.create(MessageSender.class);
        serverRpcQueue = GWT.create(ServerRpcQueue.class);

        tree = new StateTree(serverRpcQueue);

        StateNode rootNode = tree.getRootNode();
        Element body = Browser.getDocument().getBody();

        BasicElementBinder.bind(rootNode, body);
    }

    public void init(ApplicationConfiguration cnf) {
        Console.log("Starting application " + cnf.getRootPanelId());

        Console.log("Vaadin application servlet version: "
                + cnf.getServletVersion());

        if (!cnf.getServletVersion().equals(Version.getFullVersion())) {
            Console.error(
                    "Warning: your widget set seems to be built with a different "
                            + "version than the one used on server. Unexpected "
                            + "behavior may occur.");
        }

        configuration = cnf;

        loadingIndicator.setConnection(this);
        messageHandler.setConnection(this);
        messageSender.setConnection(this);
        serverRpcQueue.setConnection(this);

        String appRootPanelName = cnf.getRootPanelId();
        // remove the end (window name) of autogenerated rootpanel id
        appRootPanelName = appRootPanelName.replaceFirst("-\\d+$", "");

        publishJavascriptMethods(appRootPanelName);

        // Connection state handler preloads the reconnect dialog, which uses
        // overlay container. This in turn depends on VUI being attached
        // (done in uiConnector.init)
        connectionStateHandler.setConnection(this);

        getLoadingIndicator().show();

        heartbeat.init(this);
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
            getMessageSender().resynchronize();
        } else {
            // initial UIDL provided in DOM, continue as if returned by request

            // Hack to avoid logging an error in endRequest()
            getMessageSender().startRequest();
            getMessageHandler()
                    .handleMessage(MessageHandler.parseJson(jsonText));
        }

    }

    /**
     * Checks if there is some work to be done on the client side.
     *
     * @return true if the client has some work to be done, false otherwise
     */
    private boolean isActive() {
        return !getMessageHandler().isInitialUidlHandled()
                || getMessageSender().hasActiveRequest()
                || isExecutingDeferredCommands();
    }

    private native void publishJavascriptMethods(String TTAppId)
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
    
        client.getProfilingData = $entry(function() {
            var smh = ap.@com.vaadin.client.ApplicationConnection::getMessageHandler();
            var pd = [
                smh.@com.vaadin.client.communication.MessageHandler::lastProcessingTime,
                    smh.@com.vaadin.client.communication.MessageHandler::totalProcessingTime
                ];
            pd = pd.concat(smh.@com.vaadin.client.communication.MessageHandler::serverTimingInfo);
            pd[pd.length] = smh.@com.vaadin.client.communication.MessageHandler::bootstrapTime;
            return pd;
        });
    
        client.initializing = false;
    
        $wnd.vaadin.clients[TTAppId] = client;
    }-*/;

    private JavaScriptObject getVersionInfo() {
        return configuration.getVersionInfoJSObject();
    }

    protected ServerRpcQueue serverRpcQueue;
    protected ConnectionStateHandler connectionStateHandler;
    protected MessageHandler messageHandler;
    protected MessageSender messageSender;

    private final StateTree tree;

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
        Console.error("Communication error: " + details);
        showError(details, configuration.getCommunicationError());
    }

    /**
     * Shows the authentication error notification.
     *
     * @param details
     *            Optional details.
     */
    public void showAuthenticationError(String details) {
        Console.error("Authentication error: " + details);
        showError(details, configuration.getAuthorizationError());
    }

    /**
     * Shows the session expiration notification.
     *
     * @param details
     *            Optional details.
     */
    public void showSessionExpiredError(String details) {
        Console.error("Session expired: " + details);
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
        showError(message.getCaption(), message.getMessage(), details,
                message.getUrl());
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
     * Returns the loading indicator used by this ApplicationConnection.
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
                Console.error(event.getResourceUrl()
                        + " could not be loaded, or the load detection failed because the stylesheet is empty.");
                // The show must go on
                onLoad(event);
            }
        };
        ResourceLoader loader = ResourceLoader.get();
        for (int i = 0; i < dependencies.length(); i++) {
            String url = translateVaadinUri(dependencies.get(i));
            ApplicationConfiguration.startDependencyLoading();
            loader.loadStylesheet(url, resourceLoadListener);
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
                Console.error(event.getResourceUrl() + " could not be loaded.");
                // The show must go on
                onLoad(event);
            }
        };

        ResourceLoader loader = ResourceLoader.get();

        // Start chain by loading first
        String url = translateVaadinUri(dependencies.shift());
        ApplicationConfiguration.startDependencyLoading();
        loader.loadScript(url, resourceLoadListener);

        for (int i = 0; i < dependencies.length(); i++) {
            String preloadUrl = translateVaadinUri(dependencies.get(i));
            loader.loadScript(preloadUrl, null);
        }
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
     * Gets the {@link ApplicationConfiguration} for the current application.
     *
     * @see ApplicationConfiguration
     * @return the configuration for this application
     */
    public ApplicationConfiguration getConfiguration() {
        return configuration;
    }

    public void setApplicationRunning(boolean applicationRunning) {
        if (getApplicationState() == ApplicationState.TERMINATED) {
            if (applicationRunning) {
                Console.error(
                        "Tried to restart a terminated application. This is not supported");
            } else {
                Console.warn(
                        "Tried to stop a terminated application. This should not be done");
            }
            return;
        } else if (getApplicationState() == ApplicationState.INITIALIZING) {
            if (applicationRunning) {
                applicationState = ApplicationState.RUNNING;
            } else {
                Console.warn(
                        "Tried to stop the application before it has started. This should not be done");
            }
        } else if (getApplicationState() == ApplicationState.RUNNING) {
            if (!applicationRunning) {
                applicationState = ApplicationState.TERMINATED;
                eventBus.fireEvent(new ApplicationStoppedEvent());
            } else {
                Console.warn(
                        "Tried to start an already running application. This should not be done");
            }
        }
    }

    /**
     * Checks if the application is in the {@link ApplicationState#RUNNING}
     * state.
     *
     * @since 7.6
     * @return true if the application is in the running state, false otherwise
     */
    public boolean isApplicationRunning() {
        return applicationState == ApplicationState.RUNNING;
    }

    public <H extends EventHandler> HandlerRegistration addHandler(
            Event.Type<H> type, H handler) {
        return eventBus.addHandler(type, handler);
    }

    /**
     * Fires the given event using the event bus for this class.
     *
     * @param event
     *            the event to fire
     */
    public void fireEvent(Event<?> event) {
        eventBus.fireEvent(event);
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
     * @since 7.6
     * @return the current state of this application
     */
    public ApplicationState getApplicationState() {
        return applicationState;
    }

    /**
     * Gets the server RPC queue for this application
     *
     * @since 7.6
     * @return the server RPC queue
     */
    public ServerRpcQueue getServerRpcQueue() {
        return serverRpcQueue;
    }

    /**
     * Gets the communication error handler for this application
     *
     * @since 7.6
     * @return the server RPC queue
     */
    public ConnectionStateHandler getConnectionStateHandler() {
        return connectionStateHandler;
    }

    /**
     * Gets the (server to client) message handler for this application
     *
     * @since 7.6
     * @return the message handler
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Gets the (client to server) message sender for this application
     *
     * @since 7.6
     * @return the message sender
     */
    public MessageSender getMessageSender() {
        return messageSender;
    }

    public PushConfigurationState getPushConfiguration() {
        // FIXME from the server
        return new PushConfigurationState();
    }

    public ReconnectDialogConfigurationState getReconnectDialogConfiguration() {
        // FIXME from the server
        return new ReconnectDialogConfigurationState();
    }

    public void showError(String caption, String message, String details,
            String url) {
        // FIXME Not like this
        Browser.getWindow().alert(caption + "\n" + message + "\n" + details);
        if (url != null) {
            WidgetUtil.redirect(url);
        }
    }

    /**
     * Gets the state tree for this connection.
     *
     * @return the state tree
     */
    public StateTree getTree() {
        return tree;
    }

}
