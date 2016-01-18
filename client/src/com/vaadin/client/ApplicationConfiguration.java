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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.ui.ui.UIConstants;

import elemental.client.Browser;

public class ApplicationConfiguration implements EntryPoint {

    /**
     * Helper class for reading configuration options from the bootstap
     * javascript
     * 
     * @since 7.0
     */
    private static class JsoConfiguration extends JavaScriptObject {
        protected JsoConfiguration() {
            // JSO Constructor
        }

        /**
         * Reads a configuration parameter as a string. Please note that the
         * javascript value of the parameter should also be a string, or else an
         * undefined exception may be thrown.
         * 
         * @param name
         *            name of the configuration parameter
         * @return value of the configuration parameter, or <code>null</code> if
         *         not defined
         */
        private native String getConfigString(String name)
        /*-{
            var value = this.getConfig(name);
            if (value === null || value === undefined) {
                return null;
            } else {
                return value +"";
            }
        }-*/;

        /**
         * Reads a configuration parameter as a boolean object. Please note that
         * the javascript value of the parameter should also be a boolean, or
         * else an undefined exception may be thrown.
         * 
         * @param name
         *            name of the configuration parameter
         * @return boolean value of the configuration paramter, or
         *         <code>null</code> if no value is defined
         */
        private native Boolean getConfigBoolean(String name)
        /*-{
            var value = this.getConfig(name);
            if (value === null || value === undefined) {
                return null;
            } else {
                 // $entry not needed as function is not exported
                return @java.lang.Boolean::valueOf(Z)(value);
            }
        }-*/;

        /**
         * Reads a configuration parameter as an integer object. Please note
         * that the javascript value of the parameter should also be an integer,
         * or else an undefined exception may be thrown.
         * 
         * @param name
         *            name of the configuration parameter
         * @return integer value of the configuration paramter, or
         *         <code>null</code> if no value is defined
         */
        private native Integer getConfigInteger(String name)
        /*-{
            var value = this.getConfig(name);
            if (value === null || value === undefined) {
                return null;
            } else {
                 // $entry not needed as function is not exported
                return @java.lang.Integer::valueOf(I)(value);
            }
        }-*/;

        /**
         * Reads a configuration parameter as an {@link ErrorMessage} object.
         * Please note that the javascript value of the parameter should also be
         * an object with appropriate fields, or else an undefined exception may
         * be thrown when calling this method or when calling methods on the
         * returned object.
         * 
         * @param name
         *            name of the configuration parameter
         * @return error message with the given name, or <code>null</code> if no
         *         value is defined
         */
        private native ErrorMessage getConfigError(String name)
        /*-{
            return this.getConfig(name);
        }-*/;

        /**
         * Returns a native javascript object containing version information
         * from the server.
         * 
         * @return a javascript object with the version information
         */
        private native JavaScriptObject getVersionInfoJSObject()
        /*-{
            return this.getConfig("versionInfo");
        }-*/;

        /**
         * Gets the version of the Vaadin framework used on the server.
         * 
         * @return a string with the version
         * 
         * @see com.vaadin.server.VaadinServlet#VERSION
         */
        private native String getVaadinVersion()
        /*-{
            return this.getConfig("versionInfo").vaadinVersion;
        }-*/;

        /**
         * Gets the version of the Atmosphere framework.
         * 
         * @return a string with the version
         * 
         * @see org.atmosphere.util#getRawVersion()
         */
        private native String getAtmosphereVersion()
        /*-{
            return this.getConfig("versionInfo").atmosphereVersion;
        }-*/;

        /**
         * Gets the JS version used in the Atmosphere framework.
         * 
         * @return a string with the version
         */
        private native String getAtmosphereJSVersion()
        /*-{
            if ($wnd.jQueryVaadin != undefined){
                return $wnd.jQueryVaadin.atmosphere.version;
            }
            else {
                return null;
            }
        }-*/;

        private native String getUIDL()
        /*-{
           return this.getConfig("uidl");
         }-*/;
    }

    /**
     * Wraps a native javascript object containing fields for an error message
     * 
     * @since 7.0
     */
    public static final class ErrorMessage extends JavaScriptObject {

        protected ErrorMessage() {
            // JSO constructor
        }

        public final native String getCaption()
        /*-{
            return this.caption;
        }-*/;

        public final native String getMessage()
        /*-{
            return this.message;
        }-*/;

        public final native String getUrl()
        /*-{
            return this.url;
        }-*/;
    }

    private String id;
    /**
     * The URL to the VAADIN directory containing themes and widgetsets. Should
     * always end with a slash (/).
     */
    private String vaadinDirUrl;
    private String serviceUrl;
    private int uiId;
    private boolean standalone;
    private ErrorMessage communicationError;
    private ErrorMessage authorizationError;
    private ErrorMessage sessionExpiredError;
    private int heartbeatInterval;

    private HashMap<Integer, String> unknownComponents;

    private boolean widgetsetVersionSent = false;
    private static boolean moduleLoaded = false;

    static// TODO consider to make this hashmap per application
    LinkedList<Runnable> callbacks = new LinkedList<Runnable>();

    private static int dependenciesLoading;

    private static ArrayList<ApplicationConnection> runningApplications = new ArrayList<ApplicationConnection>();

    /**
     * Checks whether path info in requests to the server-side service should be
     * in a request parameter (named <code>v-resourcePath</code>) or appended to
     * the end of the service URL.
     * 
     * @see #getServiceUrl()
     * 
     * @return <code>true</code> if path info should be a request parameter;
     *         <code>false</code> if the path info goes after the service URL
     */
    public boolean useServiceUrlPathParam() {
        return getServiceUrlParameterName() != null;
    }

    /**
     * Return the name of the parameter used to to send data to the service url.
     * This method should only be called if {@link #useServiceUrlPathParam()} is
     * true.
     * 
     * @since 7.1.6
     * @return The parameter name, by default <code>v-resourcePath</code>
     */
    public String getServiceUrlParameterName() {
        return getJsoConfiguration(id).getConfigString(
                ApplicationConstants.SERVICE_URL_PARAMETER_NAME);
    }

    public String getRootPanelId() {
        return id;
    }

    /**
     * Gets the URL to the server-side VaadinService. If
     * {@link #useServiceUrlPathParam()} return <code>true</code>, the requested
     * path info should be in the <code>v-resourcePath</code> query parameter;
     * else the path info should be appended to the end of the URL.
     * 
     * @see #useServiceUrlPathParam()
     * 
     * @return the URL to the server-side service as a string
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Gets the URL of the VAADIN directory on the server.
     * 
     * @return the URL of the VAADIN directory
     */
    public String getVaadinDirUrl() {
        return vaadinDirUrl;
    }

    public void setAppId(String appId) {
        id = appId;
    }

    /**
     * Gets the initial UIDL from the DOM, if it was provided during the init
     * process.
     * 
     * @return
     */
    public String getUIDL() {
        return getJsoConfiguration(id).getUIDL();
    }

    /**
     * @return true if the application is served by std. Vaadin servlet and is
     *         considered to be the only or main content of the host page.
     */
    public boolean isStandalone() {
        return standalone;
    }

    /**
     * Gets the UI id of the server-side UI associated with this client-side
     * instance. The UI id should be included in every request originating from
     * this instance in order to associate the request with the right UI
     * instance on the server.
     * 
     * @return the UI id
     */
    public int getUIId() {
        return uiId;
    }

    /**
     * @return The interval in seconds between heartbeat requests, or a
     *         non-positive number if heartbeat is disabled.
     */
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public JavaScriptObject getVersionInfoJSObject() {
        return getJsoConfiguration(id).getVersionInfoJSObject();
    }

    public ErrorMessage getCommunicationError() {
        return communicationError;
    }

    public ErrorMessage getAuthorizationError() {
        return authorizationError;
    }

    public ErrorMessage getSessionExpiredError() {
        return sessionExpiredError;
    }

    /**
     * Reads the configuration values defined by the bootstrap javascript.
     */
    private void loadFromDOM() {
        JsoConfiguration jsoConfiguration = getJsoConfiguration(id);
        serviceUrl = jsoConfiguration
                .getConfigString(ApplicationConstants.SERVICE_URL);
        if (serviceUrl == null || "".equals(serviceUrl)) {
            /*
             * Use the current url without query parameters and fragment as the
             * default value.
             */
            serviceUrl = Browser.getWindow().getLocation().getHref()
                    .replaceFirst("[?#].*", "");
        } else {
            /*
             * Resolve potentially relative URLs to ensure they point to the
             * desired locations even if the base URL of the page changes later
             * (e.g. with pushState)
             */
            serviceUrl = WidgetUtil.getAbsoluteUrl(serviceUrl);
        }
        // Ensure there's an ending slash (to make appending e.g. UIDL work)
        if (!useServiceUrlPathParam() && !serviceUrl.endsWith("/")) {
            serviceUrl += '/';
        }

        vaadinDirUrl = WidgetUtil.getAbsoluteUrl(jsoConfiguration
                .getConfigString(ApplicationConstants.VAADIN_DIR_URL));
        uiId = jsoConfiguration.getConfigInteger(UIConstants.UI_ID_PARAMETER)
                .intValue();

        // null -> false
        standalone = jsoConfiguration
                .getConfigBoolean("standalone") == Boolean.TRUE;

        heartbeatInterval = jsoConfiguration
                .getConfigInteger("heartbeatInterval");

        communicationError = jsoConfiguration.getConfigError("comErrMsg");
        authorizationError = jsoConfiguration.getConfigError("authErrMsg");
        sessionExpiredError = jsoConfiguration.getConfigError("sessExpMsg");
    }

    /**
     * Starts the application with a given id by reading the configuration
     * options stored by the bootstrap javascript.
     * 
     * @param applicationId
     *            id of the application to load, this is also the id of the html
     *            element into which the application should be rendered.
     */
    public static void startApplication(final String applicationId) {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                Profiler.enter("ApplicationConfiguration.startApplication");
                ApplicationConfiguration appConf = getConfigFromDOM(
                        applicationId);
                ApplicationConnection a = GWT
                        .create(ApplicationConnection.class);
                a.init(appConf);
                runningApplications.add(a);
                Profiler.leave("ApplicationConfiguration.startApplication");

                a.start();
            }
        });
    }

    public static List<ApplicationConnection> getRunningApplications() {
        return runningApplications;
    }

    /**
     * Gets the configuration object for a specific application from the
     * bootstrap javascript.
     * 
     * @param appId
     *            the id of the application to get configuration data for
     * @return a native javascript object containing the configuration data
     */
    private native static JsoConfiguration getJsoConfiguration(String appId)
    /*-{
        return $wnd.vaadin.getApp(appId);
     }-*/;

    public static ApplicationConfiguration getConfigFromDOM(String appId) {
        ApplicationConfiguration conf = new ApplicationConfiguration();
        conf.setAppId(appId);
        conf.loadFromDOM();
        return conf;
    }

    public String getServletVersion() {
        return getJsoConfiguration(id).getVaadinVersion();
    }

    /**
     * Return Atmosphere version.
     * 
     * @since 7.4
     * 
     * @return Atmosphere version.
     */
    public String getAtmosphereVersion() {
        return getJsoConfiguration(id).getAtmosphereVersion();
    }

    /**
     * Return Atmosphere JS version.
     * 
     * @since 7.4
     * 
     * @return Atmosphere JS version.
     */
    public String getAtmosphereJSVersion() {
        return getJsoConfiguration(id).getAtmosphereJSVersion();
    }

    /**
     * @since 7.6
     * @param c
     */
    public static void runWhenDependenciesLoaded(Runnable c) {
        if (dependenciesLoading == 0) {
            c.run();
        } else {
            callbacks.add(c);
        }
    }

    static void startDependencyLoading() {
        dependenciesLoading++;
    }

    static void endDependencyLoading() {
        dependenciesLoading--;
        if (dependenciesLoading == 0 && !callbacks.isEmpty()) {
            for (Runnable cmd : callbacks) {
                cmd.run();
            }
            callbacks.clear();
        }
    }

    private native boolean vaadinBootstrapLoaded()
    /*-{
         return $wnd.vaadin != null;
     }-*/;

    @Override
    public void onModuleLoad() {

        // Don't run twice if the module has been inherited several times,
        // and don't continue if vaadinBootstrap was not executed.
        if (moduleLoaded || !vaadinBootstrapLoaded()) {
            Browser.getWindow().getConsole().warn(
                    "vaadinBootstrap.js was not loaded, skipping vaadin application configuration.");
            return;
        }
        moduleLoaded = true;

        Profiler.initialize();

        registerCallback(GWT.getModuleName());
    }

    /**
     * Registers that callback that the bootstrap javascript uses to start
     * applications once the widgetset is loaded and all required information is
     * available
     * 
     * @param widgetsetName
     *            the name of this widgetset
     */
    public native static void registerCallback(String widgetsetName)
    /*-{
        var callbackHandler = $entry(@com.vaadin.client.ApplicationConfiguration::startApplication(Ljava/lang/String;));
        $wnd.vaadin.registerWidgetset(widgetsetName, callbackHandler);
    }-*/;

    /**
     * Checks if production mode is enabled. When production mode is enabled,
     * client-side logging is disabled. There may also be other performance
     * optimizations.
     * 
     * @since 7.1.2
     * @return <code>true</code> if production mode is enabled; otherwise
     *         <code>false</code>.
     */
    public static boolean isProductionMode() {
        return !isDebugAvailable();
    }

    private native static boolean isDebugAvailable()
    /*-{
        if($wnd.vaadin.debug) {
            return true;
        } else {
            return false;
        }
    }-*/;

    /**
     * Checks whether the widget set version has been sent to the server. It is
     * sent in the first UIDL request.
     * 
     * @return <code>true</code> if browser information has already been sent
     */
    public boolean isWidgetsetVersionSent() {
        return widgetsetVersionSent;
    }

    /**
     * Registers that the widget set version has been sent to the server.
     */
    public void setWidgetsetVersionSent() {
        widgetsetVersionSent = true;
    }
}
