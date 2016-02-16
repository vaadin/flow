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
package com.vaadin.client.bootstrap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.Profiler;
import com.vaadin.client.ValueMap;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.shared.ApplicationConstants;

import elemental.client.Browser;

/**
 * Handles bootstrapping of the application.
 * <p>
 * Reads the configuration provided by the server in the DOM and starts the
 * client engine ({@link ApplicationConnection}).
 * <p>
 * Acts as the GWT entry point.
 *
 * @author Vaadin
 * @since
 */
public class Bootstrapper implements EntryPoint {

    private static boolean moduleLoaded = false;

    private static JsArray<ApplicationConnection> runningApplications = JsCollections
            .array();

    @Override
    public void onModuleLoad() {
        initModule();
    }

    private static void initModule() {
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
     * Starts the application with a given id by reading the configuration
     * options stored by the bootstrap javascript.
     *
     * @param applicationId
     *            id of the application to load, this is also the id of the html
     *            element into which the application should be rendered.
     */
    public static void startApplication(final String applicationId) {
        Scheduler.get().scheduleDeferred(() -> {
            Profiler.enter("Bootstrapper.startApplication");
            ApplicationConfiguration appConf = getConfigFromDOM(applicationId);
            ApplicationConnection applicationConnection = new ApplicationConnection(
                    appConf);
            runningApplications.push(applicationConnection);
            Profiler.leave("Bootstrapper.startApplication");

            ValueMap initialUidl = getJsoConfiguration(applicationId).getUIDL();
            applicationConnection.start(initialUidl);
        });
    }

    /**
     * Gets a list of references to all running application instances.
     *
     * @return a list of ApplicationConnections currently active
     */
    public static JsArray<ApplicationConnection> getRunningApplications() {
        return runningApplications;
    }

    /**
     * Constructs an ApplicationConfiguration object based on the information
     * available in the DOM.
     *
     * @param appId
     *            the application id
     * @return an application configuration object containing the read
     *         information
     */
    private static ApplicationConfiguration getConfigFromDOM(String appId) {
        ApplicationConfiguration conf = new ApplicationConfiguration();
        conf.setApplicationId(appId);
        populateApplicationConfiguration(conf, getJsoConfiguration(appId));
        return conf;
    }

    /**
     * Reads the configuration values defined by the bootstrap Javascript.
     *
     * @param conf
     */
    private static void populateApplicationConfiguration(
            ApplicationConfiguration conf, JsoConfiguration jsoConfiguration) {
        String serviceUrl = jsoConfiguration
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
        if (!serviceUrl.endsWith("/")) {
            serviceUrl += Character.toString('/');
        }
        conf.setServiceUrl(serviceUrl);

        conf.setVaadinDirUrl(WidgetUtil.getAbsoluteUrl(jsoConfiguration
                .getConfigString(ApplicationConstants.VAADIN_DIR_URL)));
        conf.setUIId(jsoConfiguration
                .getConfigInteger(ApplicationConstants.UI_ID_PARAMETER)
                .intValue());

        // null -> false
        conf.setStandalone(jsoConfiguration.getConfigBoolean("standalone"));

        conf.setHeartbeatInterval(
                jsoConfiguration.getConfigInteger("heartbeatInterval"));

        conf.setServletVersion(jsoConfiguration.getVaadinVersion());
        conf.setAtmosphereVersion(jsoConfiguration.getAtmosphereVersion());
        conf.setAtmosphereJSVersion(jsoConfiguration.getAtmosphereJSVersion());
        conf.setCommunicationError(
                jsoConfiguration.getConfigError("comErrMsg"));
        conf.setAuthorizationError(
                jsoConfiguration.getConfigError("authErrMsg"));
        conf.setSessionExpiredError(
                jsoConfiguration.getConfigError("sessExpMsg"));

        // Debug or production mode?
        conf.setDebugMode(isDebugMode());
    }

    /**
     * Checks if debug mode is active (not disabled from server and debug flag
     * present).
     *
     * @return {@code true} if debug mode is active, {@code false} otherwise
     */
    private static boolean isDebugMode() {
        return isDebugAvailable() && getParameter("debug") != null;
    }

    private static String getParameter(String parameter) {
        String[] keyValues = Browser.getDocument().getLocation().getSearch()
                .substring(1).split("&");
        for (String keyValue : keyValues) {
            String[] param = keyValue.split("=", 1);
            if (param[0].equals(parameter)) {
                return param[1];
            }
        }

        return null;
    }

    private static native boolean isDebugAvailable()
    /*-{
        if($wnd.vaadin.debug) {
            return true;
        } else {
            return false;
        }
    }-*/;

    /**
     * Gets the configuration object for a specific application from the
     * bootstrap javascript.
     *
     * @param appId
     *            the id of the application to get configuration data for
     * @return a native javascript object containing the configuration data
     */
    private static native JsoConfiguration getJsoConfiguration(String appId)
    /*-{
        return $wnd.vaadin.getApp(appId);
     }-*/;

    private static native boolean vaadinBootstrapLoaded()
    /*-{
         return $wnd.vaadin != null;
     }-*/;

    /**
     * Registers the callback that the bootstrap javascript uses to start
     * applications once the widgetset is loaded and all required information is
     * available.
     *
     * @param widgetsetName
     *            the name of this widgetset
     */
    public static native void registerCallback(String widgetsetName)
    /*-{
        var callbackHandler = $entry(@com.vaadin.client.bootstrap.Bootstrapper::startApplication(Ljava/lang/String;));
        $wnd.vaadin.registerWidgetset(widgetsetName, callbackHandler);
    }-*/;

}
