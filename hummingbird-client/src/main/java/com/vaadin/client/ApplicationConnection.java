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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;
import com.vaadin.client.hummingbird.BasicElementBinder;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.shared.Version;
import com.vaadin.shared.ui.ui.UIState.ReconnectDialogConfigurationState;

import elemental.client.Browser;
import elemental.dom.Element;

/**
 * Main class for an application / UI.
 * <p>
 * Initializes the registry and starts the application.
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
    private EventBus eventBus = new SimpleEventBus();

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

    private final Registry registry;

    /**
     * Creates an application connection using the given configuration.
     *
     * @param applicationConfiguration
     *            the configuration object for the application
     */
    public ApplicationConnection(
            ApplicationConfiguration applicationConfiguration) {
        configuration = applicationConfiguration;
        registry = new DefaultRegistry(this);

        StateNode rootNode = registry.getStateTree().getRootNode();
        Element body = Browser.getDocument().getBody();

        BasicElementBinder.bind(rootNode, body);

        Console.log("Starting application "
                + applicationConfiguration.getApplicationId());

        Console.log("Vaadin application servlet version: "
                + configuration.getServletVersion());

        if (!configuration.getServletVersion()
                .equals(Version.getFullVersion())) {
            Console.error(
                    "Warning: your widget set seems to be built with a different "
                            + "version than the one used on server. Unexpected "
                            + "behavior may occur.");
        }

        String appRootPanelName = applicationConfiguration.getApplicationId();
        // remove the end (window name) of autogenerated rootpanel id
        appRootPanelName = appRootPanelName.replaceFirst("-\\d+$", "");

        publishJavascriptMethods(appRootPanelName);

        registry.getLoadingIndicator().show();
    }

    /**
     * Starts this application.
     * <p>
     * Called by the bootstrapper, which ensures applications are started in
     * order.
     *
     * @param initialUidl
     *            the initial UIDL or null if the server did not provide any
     */
    public void start(String initialUidl) {
        if (initialUidl == null) {
            // initial UIDL not in DOM, request from server
            registry.getMessageSender().resynchronize();
        } else {
            // initial UIDL provided in DOM, continue as if returned by request

            // Hack to avoid logging an error in endRequest()
            registry.getMessageSender().startRequest();
            registry.getMessageHandler()
                    .handleMessage(MessageHandler.parseJson(initialUidl));
        }

    }

    /**
     * Checks if there is some work to be done on the client side.
     *
     * @return true if the client has some work to be done, false otherwise
     */
    private boolean isActive() {
        return !registry.getMessageHandler().isInitialUidlHandled()
                || registry.getMessageSender().hasActiveRequest()
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
            var smh = ap.@com.vaadin.client.ApplicationConnection::registry.@com.vaadin.client.Registry::getMessageHandler();
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
        return configuration.getVersionInfo();
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
        if (s instanceof TrackingScheduler) {
            return ((TrackingScheduler) s).hasWorkQueued();
        } else {
            return false;
        }
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

    public ReconnectDialogConfigurationState getReconnectDialogConfiguration() {
        // FIXME from the server
        return new ReconnectDialogConfigurationState();
    }

}
