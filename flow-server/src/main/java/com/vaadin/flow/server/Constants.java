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

/**
 * Constants used by the server side framework.
 * <p>
 * Not available on the client side, for that use
 * {@link com.vaadin.flow.shared.ApplicationConstants}.
 *
 * @since 1.0
 */
public final class Constants implements Serializable {

    // Keep the version number in sync with flow-push/pom.xml
    public static final String REQUIRED_ATMOSPHERE_RUNTIME_VERSION = "2.7.3.slf4jvaadin7";

    /**
     * The prefix used for System property parameters.
     */
    public static final String VAADIN_PREFIX = "vaadin.";

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_PRODUCTION_MODE}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;

    // Token file keys used for defining folder paths for dev server
    public static final String NPM_TOKEN = "npmFolder";
    public static final String FRONTEND_TOKEN = "frontendFolder";
    public static final String GENERATED_TOKEN = "generatedFolder";
    public static final String CONNECT_JAVA_SOURCE_FOLDER_TOKEN = "connect.javaSourceFolder";
    public static final String JAVA_RESOURCE_FOLDER_TOKEN = "javaResourceFolder";
    public static final String CONNECT_APPLICATION_PROPERTIES_TOKEN = "connect.applicationProperties";
    public static final String CONNECT_OPEN_API_FILE_TOKEN = "connect.openApiFile";
    public static final String PROJECT_FRONTEND_GENERATED_DIR_TOKEN = "project.frontend.generated";
    public static final String EXTERNAL_STATS_FILE_TOKEN = "externalStatsFile";
    public static final String EXTERNAL_STATS_URL_TOKEN = "externalStatsUrl";

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_USE_V14_BOOTSTRAP}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_USE_V14_BOOTSTRAP = InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_INITIAL_UIDL}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_INITIAL_UIDL = InitParameters.SERVLET_PARAMETER_INITIAL_UIDL;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_ENABLE_DEV_SERVER}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_ENABLE_DEV_SERVER = InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_REUSE_DEV_SERVER}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_REUSE_DEV_SERVER = InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_REQUEST_TIMING}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_REQUEST_TIMING = InitParameters.SERVLET_PARAMETER_REQUEST_TIMING;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION = InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_HEARTBEAT_INTERVAL}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_HEARTBEAT_INTERVAL = InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT = InitParameters.SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS = InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_PUSH_MODE}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_PUSH_MODE = InitParameters.SERVLET_PARAMETER_PUSH_MODE;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_SYNC_ID_CHECK}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_SYNC_ID_CHECK = InitParameters.SERVLET_PARAMETER_SYNC_ID_CHECK;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS = InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING = InitParameters.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT = InitParameters.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_JSBUNDLE}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_JSBUNDLE = InitParameters.SERVLET_PARAMETER_JSBUNDLE;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_POLYFILLS}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_POLYFILLS = InitParameters.SERVLET_PARAMETER_POLYFILLS;

    public static final String POLYFILLS_DEFAULT_VALUE = "";

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_BROTLI} instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_BROTLI = InitParameters.SERVLET_PARAMETER_BROTLI;

    /**
     * @deprecated Use {@link InitParameters#I18N_PROVIDER} instead.
     */
    @Deprecated
    public static final String I18N_PROVIDER = InitParameters.I18N_PROVIDER;

    /**
     * @deprecated Use
     *             {@link InitParameters#DISABLE_AUTOMATIC_SERVLET_REGISTRATION}
     *             instead.
     */
    @Deprecated
    public static final String DISABLE_AUTOMATIC_SERVLET_REGISTRATION = InitParameters.DISABLE_AUTOMATIC_SERVLET_REGISTRATION;

    /**
     * @deprecated Use {@link InitParameters#COMPILED_WEB_COMPONENTS_PATH}
     *             instead.
     */
    @Deprecated
    public static final String COMPILED_WEB_COMPONENTS_PATH = InitParameters.COMPILED_WEB_COMPONENTS_PATH;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_STATISTICS_JSON}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_STATISTICS_JSON = InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;

    /**
     * Default path for the WebPack profile statistics json file. It can be
     * modified by setting the system property "statistics.file.path".
     */
    public static final String STATISTICS_JSON_DEFAULT = Constants.VAADIN_CONFIGURATION
            + "stats.json";

    /**
     * Default resource directory to place template sources in. This is used
     * used for Vite production mode instead of a stats.json file.
     */
    public static final String TEMPLATE_DIRECTORY = Constants.VAADIN_CONFIGURATION
            + "templates/";

    /**
     * Name of the <code>npm</code> main file.
     */
    public static final String PACKAGE_JSON = "package.json";

    /**
     * Name of the <code>npm</code> version locking ile.
     */

    public static final String PACKAGE_LOCK_JSON = "package-lock.json";

    /**
     * Target folder constant.
     */
    public static final String TARGET = "target";

    /**
     * Location for the frontend resources in jar files for compatibility mode
     * (also obsolete but supported for npm mode).
     */
    public static final String COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT = "META-INF/resources/frontend";

    /**
     * Location for the frontend resources in jar files.
     */
    public static final String RESOURCES_FRONTEND_DEFAULT = "META-INF/frontend";

    /**
     * The name of the application theme root folder.
     */
    public static final String APPLICATION_THEME_ROOT = "themes";

    /**
     * Location for the resources in jar files.
     */
    public static final String RESOURCES_JAR_DEFAULT = "META-INF/resources/";

    /**
     * Location for the theme resources in jar files.
     */
    public static final String RESOURCES_THEME_JAR_DEFAULT = RESOURCES_JAR_DEFAULT
            + APPLICATION_THEME_ROOT + "/";

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE = InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE;

    /**
     * @deprecated Use {@link InitParameters#SERVLET_PARAMETER_ENABLE_PNPM}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_ENABLE_PNPM = InitParameters.SERVLET_PARAMETER_ENABLE_PNPM;

    /**
     * Constant for whether pnpm is default or not.
     */
    public static final boolean ENABLE_PNPM_DEFAULT = false;

    /**
     * Constant for setting the pinned supported version of pnpm to be used by
     * default (see
     * {@link com.vaadin.flow.server.frontend.FrontendTools#DEFAULT_PNPM_VERSION}).
     */
    public static final boolean GLOBAL_PNPM_DEFAULT = false;

    /**
     * The default value for {@link InitParameters#NODE_AUTO_UPDATE}.
     */
    public static final boolean DEFAULT_NODE_AUTO_UPDATE = true;

    /**
     * The default value for {@link #REQUIRE_HOME_NODE_EXECUTABLE}.
     */
    public static final boolean DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE = false;

    /**
     * The default value for whether usage statistics is enabled.
     */
    public static final boolean DEFAULT_DEVMODE_STATS = true;

    /**
     * @deprecated Use {@link InitParameters#REQUIRE_HOME_NODE_EXECUTABLE}
     *             instead.
     */
    @Deprecated
    public static final String REQUIRE_HOME_NODE_EXECUTABLE = InitParameters.REQUIRE_HOME_NODE_EXECUTABLE;

    /**
     * Internal parameter which prevent validation for annotations which are
     * allowed on an AppShell class in non V14 bootstrap mode.
     */
    public static final String ALLOW_APPSHELL_ANNOTATIONS = "allow.appshell.annotations";

    /**
     * The path used in the vaadin servlet for handling static resources.
     */
    public static final String META_INF = "META-INF/";

    /**
     * The path used in the vaadin servlet for handling static resources.
     */
    public static final String VAADIN_MAPPING = "VAADIN/";

    /**
     * The path used in the vaadin servlet for handling push.
     */
    public static final String PUSH_MAPPING = VAADIN_MAPPING + "push";

    /**
     * The static build resources folder.
     */
    public static final String VAADIN_BUILD = "build/";

    /**
     * The static configuration resources folder.
     */
    public static final String VAADIN_CONFIGURATION = "config/";

    /**
     * The static resources root folder.
     */
    public static final String VAADIN_WEBAPP = "webapp/";

    /**
     * The generated PWA icons folder.
     */
    public static final String VAADIN_PWA_ICONS = "pwa-icons/";

    /**
     * The path to meta-inf/VAADIN/ where static resources are put on the
     * servlet.
     */
    public static final String VAADIN_SERVLET_RESOURCES = META_INF
            + VAADIN_MAPPING;

    /**
     * The path to webapp/ public resources root.
     */
    public static final String VAADIN_WEBAPP_RESOURCES = VAADIN_SERVLET_RESOURCES
            + VAADIN_WEBAPP;

    /**
     * The prefix used for all internal static files, relative to context root.
     */
    public static final String VAADIN_BUILD_FILES_PATH = VAADIN_MAPPING
            + VAADIN_BUILD;

    /**
     * Default path for local frontend resources packaged for jar add-ons.
     */
    public static final String LOCAL_FRONTEND_RESOURCES_PATH = "src/main/resources/META-INF/resources/frontend";

    /**
     * Property boolean for marking stats.json to be fetched from external
     * location.
     */
    public static final String EXTERNAL_STATS_FILE = "external.stats.file";
    /**
     * Property String for external stats.json location url.
     */
    public static final String EXTERNAL_STATS_URL = "external.stats.url";
    /**
     * Default location to look for the external stats.json.
     */
    public static final String DEFAULT_EXTERNAL_STATS_URL = "/vaadin-static/VAADIN/config/stats.json";

    /**
     * A request parameter that can be given in browser to force the Vaadin
     * application to create a new UI and session instance, thus overriding
     * {@code @PreserveOnRefresh} annotation.
     */
    public static final String URL_PARAMETER_RESTART_APPLICATION = "restartApplication";

    /**
     * A request parameter that can be given in browser to force the Vaadin
     * application to close an existing UI and session. Unlike
     * {@link #URL_PARAMETER_RESTART_APPLICATION}, this will not create a new
     * session.
     */
    public static final String URL_PARAMETER_CLOSE_APPLICATION = "closeApplication";

    /**
     * UsageEntry name for UsageStatistics BootstrapHandler.
     */
    public static final String STATISTIC_FLOW_BOOTSTRAPHANDLER = "flow/BootstrapHandler";

    /**
     * UsageEntry name for UsageStatistics Routing Server.
     */
    public static final String STATISTIC_ROUTING_SERVER = "routing/server";

    /**
     * UsageEntry name for UsageStatistics Routing Client.
     */
    public static final String STATISTIC_ROUTING_CLIENT = "routing/client";

    /**
     * UsageEntry name for UsageStatistics Hybrid.
     */
    public static final String STATISTIC_ROUTING_HYBRID = "routing/hybrid";

    /**
     * The name of platform core components and tools versions file.
     */
    public static final String VAADIN_CORE_VERSIONS_JSON = "vaadin-core-versions.json";

    /**
     * The name of platform commercial components and tools versions file.
     */
    public static final String VAADIN_VERSIONS_JSON = "vaadin-versions.json";

    /**
     * @deprecated Use
     *             {@link InitParameters#SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD}
     *             instead.
     */
    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD = InitParameters.SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD;

    /**
     * Default live reload port as defined in Spring Boot Dev Tools.
     */
    // Non-default port currently not supported (#7970)
    public static final int SPRING_BOOT_DEFAULT_LIVE_RELOAD_PORT = 35729;

    private Constants() {
        // prevent instantiation constants class only
    }
}
