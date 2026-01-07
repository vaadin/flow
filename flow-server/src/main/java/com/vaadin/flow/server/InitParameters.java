/*
 * Copyright 2000-2025 Vaadin Ltd.
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
 */
public class InitParameters implements Serializable {

    /**
     * The name of the parameter that is by default used in e.g. web.xml to
     * define the name of the default {@link UI} class.
     */
    // javadoc in UI should be updated if this value is changed
    public static final String UI_PARAMETER = "UI";

    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = "productionMode";

    public static final String SERVLET_PARAMETER_INITIAL_UIDL = "eagerServerLoad";
    public static final String SERVLET_PARAMETER_REUSE_DEV_SERVER = "reuseDevServer";
    public static final String SERVLET_PARAMETER_REQUEST_TIMING = "requestTiming";
    // Javadocs for VaadinService should be updated if this value is changed
    public static final String SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION = "disable-xsrf-protection";
    public static final String SERVLET_PARAMETER_HEARTBEAT_INTERVAL = "heartbeatInterval";
    public static final String SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT = "webComponentDisconnect";
    public static final String SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS = "closeIdleSessions";
    public static final String SERVLET_PARAMETER_PUSH_MODE = "pushMode";
    public static final String SERVLET_PARAMETER_SESSION_LOCK_CHECK_STRATEGY = "sessionLockCheckStrategy";
    public static final String SERVLET_PARAMETER_PUSH_SERVLET_MAPPING = "pushServletMapping";
    public static final String SERVLET_PARAMETER_SYNC_ID_CHECK = "syncIdCheck";
    public static final String SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS = "sendUrlsAsParameters";
    public static final String SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING = "pushLongPollingSuspendTimeout";
    public static final String SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT = "maxMessageSuspendTimeout";
    public static final String SERVLET_PARAMETER_JSBUNDLE = "module.bundle";
    public static final String SERVLET_PARAMETER_POLYFILLS = "module.polyfills";
    public static final String NODE_VERSION = "node.version";
    public static final String NODE_DOWNLOAD_ROOT = "node.download.root";
    public static final String REACT_ENABLE = "react.enable";

    /**
     * Configuration name for the parameter that determines whether Brotli
     * compression should be used for static resources in cases when a
     * precompressed file is available.
     */
    public static final String SERVLET_PARAMETER_BROTLI = "brotli";

    /**
     * Configuration name for the frontend statistics json file to use to
     * determine template contents.
     * <p>
     * File needs to be available either for the ClassLoader as a resource, or
     * as a static web resource. By default it returns the value in
     * {@link Constants#STATISTICS_JSON_DEFAULT}
     */
    public static final String SERVLET_PARAMETER_STATISTICS_JSON = "statistics.file.path";

    /**
     * Configuration name for the time waiting for the frontend build tool to
     * output a success or error pattern.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_TIMEOUT = "devmode.output.pattern.timeout";

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
     * A comma separated list of IP addresses, potentially with wildcards, which
     * can connect to the dev tools. If not specified, only localhost
     * connections are allowed.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_HOSTS_ALLOWED = "devmode.hostsAllowed";

    /**
     * The name of the custom HTTP header that contains the client IP address
     * that is checked to allow access to the dev mode server. The HTTP header
     * is supposed to contain a single address, and the HTTP request to have a
     * single occurrence of the header. If not specified, remote address are
     * read from the {@literal X-Forwarded-For} header.
     */
    public static final String SERVLET_PARAMETER_DEVMODE_REMOTE_ADDRESS_HEADER = "devmode.remoteAddressHeader";

    /**
     * Configuration parameter name for enabling pnpm.
     *
     * @since 2.2
     */
    public static final String SERVLET_PARAMETER_ENABLE_PNPM = "pnpm.enable";

    /**
     * Configuration parameter name for enabling bun.
     */
    public static final String SERVLET_PARAMETER_ENABLE_BUN = "bun.enable";

    /*
     * Configuration parameter name for enabling usage statistics.
     *
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
     */
    public static final String APPLICATION_PARAMETER_DEVMODE_ENABLE_SERIALIZE_SESSION = "devmode.sessionSerialization.enabled";

    /**
     * Configuration parameter name for enabling component tracking in
     * development mode. If not set, tracking is enabled by default.
     *
     */
    public static final String APPLICATION_PARAMETER_DEVMODE_ENABLE_COMPONENT_TRACKER = "devmode.componentTracker.enabled";

    /**
     * Configuration parameter name for adding extra file extensions for stats
     * bundle to generate hashes for.
     */
    public static final String FRONTEND_EXTRA_EXTENSIONS = "devmode.frontendExtraFileExtensions";

    /**
     * I18N provider property.
     */
    public static final String I18N_PROVIDER = "i18n.provider";

    /**
     * Menu access control property.
     */
    public static final String MENU_ACCESS_CONTROL = "menu.access.control";

    /**
     * Configuration name for the parameter that determines if Flow should
     * automatically register servlets needed for the application to work.
     */
    public static final String DISABLE_AUTOMATIC_SERVLET_REGISTRATION = "disable.automatic.servlet.registration";

    /**
     * Configuration parameter name for requiring node executable installed in
     * home directory.
     *
     */
    public static final String REQUIRE_HOME_NODE_EXECUTABLE = "require.home.node";

    /**
     * Configuration parameter name for specifying the folder containing the
     * Node.js executable.
     * <p>
     * When this parameter is set to a non-empty value, the Node.js binary will
     * be exclusively used from the specified folder. If the binary is not found
     * in this folder, an exception will be thrown with no fallback to global or
     * alternative installations.
     * <p>
     * Example: "/usr/local/custom-node" or "C:\\custom\\node"
     */
    public static final String NODE_FOLDER = "node.folder";

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
     */
    public static final String BUILD_FOLDER = "build.folder";

    /**
     * Packages, in addition to the internally used ones, to run postinstall
     * scripts for.
     *
     */
    public static final String ADDITIONAL_POSTINSTALL_PACKAGES = "npm.postinstallPackages";

    /**
     * Configuration name for enabling development using the frontend
     * development server instead of using an application bundle.
     */
    public static final String FRONTEND_HOTDEPLOY = "frontend.hotdeploy";

    /**
     * Configuration name for adding dependencies on other projects when using
     * the frontend development server.
     */
    public static final String FRONTEND_HOTDEPLOY_DEPENDENCIES = "frontend.hotdeploy.dependencies";

    /**
     * Configuration name for enabling ci build for npm/pnpm.
     */
    public static final String CI_BUILD = "vaadin.ci.build";

    /**
     * Configuration name for disabling dev bundle rebuild.
     */
    public static final String SKIP_DEV_BUNDLE_REBUILD = "vaadin.skip.dev.bundle";

    /**
     * Configuration name for forcing optimized production bundle build.
     */
    public static final String FORCE_PRODUCTION_BUILD = "vaadin.force.production.build";
    /**
     * Configuration name for forcing optimized production bundle build.
     */
    public static final String COMPRESS_BUNDLE = "vaadin.compress.bundle";

    /**
     * Configuration name to enable adding a commercial banner to the
     * application when commercial components are used without a valid license
     * key.
     */
    public static final String COMMERCIAL_WITH_BANNER = "commercialWithBanner";

    /**
     * Configuration name for cleaning or leaving frontend files in build.
     */
    public static final String CLEAN_BUILD_FRONTEND_FILES = "vaadin.clean.build.frontend.files";

    /**
     * Configuration name for how long since last browser open before we open a
     * new tab for the application in development mode.
     *
     * Time is given in minutes.
     */
    public static final String LAUNCH_BROWSER_DELAY = "launch-browser-delay";

    /**
     * Configuration name for setting the application identifier.
     */
    public static final String APPLICATION_IDENTIFIER = "applicationIdentifier";

    /**
     * Configuration name for excluding npm packages for web components.
     */
    public static final String NPM_EXCLUDE_WEB_COMPONENTS = "npm.excludeWebComponents";

}
