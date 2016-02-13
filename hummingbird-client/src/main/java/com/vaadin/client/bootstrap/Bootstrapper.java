package com.vaadin.client.bootstrap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.Profiler;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.shared.ApplicationConstants;

import elemental.client.Browser;

public class Bootstrapper implements EntryPoint {

    private static boolean moduleLoaded = false;

    static JsArray<Runnable> callbacks = JsCollections.array();

    private static int dependenciesLoading;

    private static JsArray<ApplicationConnection> runningApplications = JsCollections
            .array();

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
                Profiler.enter("Bootstrapper.startApplication");
                ApplicationConfiguration appConf = getConfigFromDOM(
                        applicationId);
                ApplicationConnection a = new ApplicationConnection(appConf);
                runningApplications.push(a);
                Profiler.leave("Bootstrapper.startApplication");

                String initialUidl = getJsoConfiguration(applicationId)
                        .getUIDL();
                a.start(initialUidl);
            }
        });
    }

    public static JsArray<ApplicationConnection> getRunningApplications() {
        return runningApplications;
    }

    public static ApplicationConfiguration getConfigFromDOM(String appId) {
        ApplicationConfiguration conf = new ApplicationConfiguration();
        conf.setAppId(appId);
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
        String serviceUrlParameter = jsoConfiguration.getConfigString(
                ApplicationConstants.SERVICE_URL_PARAMETER_NAME);

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
        if (serviceUrlParameter == null && !serviceUrl.endsWith("/")) {
            serviceUrl += '/';
        }
        conf.setServiceUrl(serviceUrl);
        conf.setServiceUrlParameterName(serviceUrlParameter);

        conf.setVaadinDirUrl(WidgetUtil.getAbsoluteUrl(jsoConfiguration
                .getConfigString(ApplicationConstants.VAADIN_DIR_URL)));
        conf.setUIId(jsoConfiguration
                .getConfigInteger(ApplicationConstants.UI_ID_PARAMETER)
                .intValue());

        // null -> false
        conf.setStandalone(jsoConfiguration
                .getConfigBoolean("standalone") == Boolean.TRUE);

        conf.setHeartbeatInterval(
                jsoConfiguration.getConfigInteger("heartbeatInterval"));

        // conf.setVersionInfo(jsoConfiguration.getVersionInfoJSObject());
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
     * Checks if we are in debug mode (not disabled from server and debug flag
     * present)
     *
     * @return
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

    private native static boolean isDebugAvailable()
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
    private native static JsoConfiguration getJsoConfiguration(String appId)
    /*-{
        return $wnd.vaadin.getApp(appId);
     }-*/;

    private native boolean vaadinBootstrapLoaded()
    /*-{
         return $wnd.vaadin != null;
     }-*/;

    /**
     * Registers that callback that the bootstrap javascript uses to start
     * applications once the widgetset is loaded and all required information is
     * available.
     *
     * @param widgetsetName
     *            the name of this widgetset
     */
    public native static void registerCallback(String widgetsetName)
    /*-{
        var callbackHandler = $entry(@com.vaadin.client.bootstrap.Bootstrapper::startApplication(Ljava/lang/String;));
        $wnd.vaadin.registerWidgetset(widgetsetName, callbackHandler);
    }-*/;

    /**
     * @since 7.6
     * @param c
     */
    public static void runWhenDependenciesLoaded(Runnable c) {
        if (dependenciesLoading == 0) {
            c.run();
        } else {
            callbacks.push(c);
        }
    }

    public static void startDependencyLoading() {
        dependenciesLoading++;
    }

    public static void endDependencyLoading() {
        dependenciesLoading--;
        if (dependenciesLoading == 0 && callbacks.length() != 0) {
            for (int i = 0; i < callbacks.length(); i++) {
                Runnable cmd = callbacks.get(i);
                cmd.run();
            }
            callbacks.clear();
        }
    }

}
