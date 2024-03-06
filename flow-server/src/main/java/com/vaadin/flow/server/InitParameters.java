/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Constants for all servlet init parameters. Keeping them in a separate class
 * allows using reflection to expose the parameters in the Spring add-on.
 *
 * <p>
 * Note: do not add other constants than String constants representing init
 * parameters here.
 *
 * @author Vaadin Ltd
 * @since
 */
public class InitParameters implements Serializable {

    /**
     * The name of the parameter that is by default used in e.g. web.xml to
     * define the name of the default {@link UI} class.
     */
    // javadoc in UI should be updated if this value is changed
    public static final String UI_PARAMETER = "UI";

    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = "productionMode";

    /**
     * Enable it if your project is using client-side bootstrapping (CCDM).
     */
    public static final String SERVLET_PARAMETER_USE_V14_BOOTSTRAP = "useDeprecatedV14Bootstrapping";

    public static final String SERVLET_PARAMETER_INITIAL_UIDL = "eagerServerLoad";
    public static final String SERVLET_PARAMETER_ENABLE_DEV_SERVER = "enableDevServer";
    public static final String SERVLET_PARAMETER_REUSE_DEV_SERVER = "reuseDevServer";
    public static final String SERVLET_PARAMETER_REQUEST_TIMING = "requestTiming";
    // Javadocs for VaadinService should be updated if this value is changed
    public static final String SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION = "disable-xsrf-protection";
    public static final String SERVLET_PARAMETER_HEARTBEAT_INTERVAL = "heartbeatInterval";
    public static final String SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT = "webComponentDisconnect";
    public static final String SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS = "closeIdleSessions";
    public static final String SERVLET_PARAMETER_PUSH_MODE = "pushMode";
    public static final String SERVLET_PARAMETER_PUSH_SERVLET_MAPPING = "pushServletMapping";
    public static final String SERVLET_PARAMETER_SYNC_ID_CHECK = "syncIdCheck";
    public static final String SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS = "sendUrlsAsParameters";
    public static final String SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING = "pushLongPollingSuspendTimeout";
    public static final String SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT = "maxMessageSuspendTimeout";
    public static final String SERVLET_PARAMETER_JSBUNDLE = "module.bundle";
    public static final String SERVLET_PARAMETER_POLYFILLS = "module.polyfills";
    public static final String NODE_VERSION = "node.version";
    public static final String NODE_DOWNLOAD_ROOT = "node.download.root";

    /**
     * Configuration name for the parameter that determines whether Brotli
     * compression should be used for static resources in cases when a
     * precompressed file is available.
     */
    public static final String SERVLET_PARAMETER_BROTLI = "brotli";

    /**
     * Configuration name for the WebPack profile statistics json file to use to
     * determine template contents.
     * <p>
     * File needs to be available either for the ClassLoader as a resource, or
     * as a static web resource. By default it returns the value in
     * {@link Constants#STATISTICS_JSON_DEFAULT}
     */
    public static final String SERVLET_PARAMETER_STATISTICS_JSON = "statistics.file.path";

    /**
     * Configuration name for the time waiting for webpack output success or
     * error pattern defined in
     * {@link Constants#SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN} and
     * {@link Constants#SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN}
     * parameters.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT = "devmode.webpack.output.pattern.timeout";

    /**
     * Configuration name for the pattern used to inspect the webpack output to
     * assure it is up and running. Default value is <code>: Compiled</code>.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN = "devmode.webpack.output.success.pattern";

    /**
     * Configuration name for the pattern used to inspect the webpack output to
     * detecting when compilation failed. Default value is
     * <code>: Failed</code>.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN = "devmode.webpack.output.error.pattern";

    /**
     * Configuration name for adding extra options to the webpack-dev-server.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS = "devmode.webpack.options";

    /**
     * Configuration name for adding extra options to the vite.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_VITE_OPTIONS = "devmode.vite.options";

    /**
     * Boolean parameter for enabling/disabling bytecode scanning in dev mode.
     * If enabled, entry points are scanned for reachable frontend resources. If
     * disabled, all classes on the classpath are scanned.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE = "devmode.optimizeBundle";

    /**
     * Configuration parameter name for enabling pnpm.
     *
     * @since 2.2
     */
    public static final String SERVLET_PARAMETER_ENABLE_PNPM = "pnpm.enable";

    /*
     * Configuration parameter name for enabling usage statistics.
     *
     * @since
     */
    public static final String SERVLET_PARAMETER_DEVMODE_STATISTICS = "devmode.usageStatistics.enabled";

    /**
     * Configuration parameter name for using globally installed pnpm or default
     * one.
     */
    public static final String SERVLET_PARAMETER_GLOBAL_PNPM = "pnpm.global";

    /**
     * Configuration parameter name for enabling live reload.
     * <p>
     * Note that if the dev tools are disabled
     * ({@link #SERVLET_PARAMETER_DEVMODE_ENABLE_DEV_TOOLS} is set to {@code
     * false}), the live reload will be disabled as well.
     *
     * @since
     */
    public static final String SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD = "devmode.liveReload.enabled";

    /**
     * Configuration parameter name for enabling dev tools.
     *
     * @since 9.0
     */
    public static final String SERVLET_PARAMETER_DEVMODE_ENABLE_DEV_TOOLS = "devmode.devTools.enabled";

    /**
     * Configuration parameter name for enabling session serialization in
     * development. If enabled, all the session's associated
     * {@link com.vaadin.flow.component.UI} instances will be serialized.
     * Otherwise, it won't be serialized.
     *
     * @since
     */
    public static final String APPLICATION_PARAMETER_DEVMODE_ENABLE_SERIALIZE_SESSION = "devmode.sessionSerialization.enabled";

    /**
     * I18N provider property.
     */
    public static final String I18N_PROVIDER = "i18n.provider";

    /**
     * Configuration name for the parameter that determines if Flow should
     * automatically register servlets needed for the application to work.
     */
    public static final String DISABLE_AUTOMATIC_SERVLET_REGISTRATION = "disable.automatic.servlet.registration";

    /**
     * Configuration parameter name for requiring node executable installed in
     * home directory.
     *
     * @since
     */
    public static final String REQUIRE_HOME_NODE_EXECUTABLE = "require.home.node";

    /**
     * Configuration parameter name for requiring node executable installed in
     * home directory.
     *
     * @since
     */
    public static final String NODE_AUTO_UPDATE = "node.auto.update";

    /**
     * Configuration name for the parameter that sets the compiled web
     * components path. The path should be the same as
     * {@code webComponentOutputDirectoryName} in the maven plugin that
     * transpiles ES6 code. This path is only used for generated web components
     * (server side web components) module in case they are transpiled: web
     * component UI imports them as dependencies.
     */
    public static final String COMPILED_WEB_COMPONENTS_PATH = "compiled.web.components.path";

    /**
     * Configuration name for the build folder.
     *
     * @since
     */
    public static final String BUILD_FOLDER = "build.folder";

    /**
     * Packages, in addition to the internally used ones, to run postinstall
     * scripts for.
     *
     * @since
     */
    public static final String ADDITIONAL_POSTINSTALL_PACKAGES = "npm.postinstallPackages";

    /**
     * Configuration name for enabling ci build for npm/pnpm.
     */
    public static final String CI_BUILD = "ci.build";
}
