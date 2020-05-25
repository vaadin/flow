/*
 * Copyright 2000-2020 Vaadin Ltd.
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
    public static final String REQUIRED_ATMOSPHERE_RUNTIME_VERSION = "2.4.30.slf4jvaadin1";

    /**
     * The prefix used for System property parameters.
     */
    public static final String VAADIN_PREFIX = "vaadin.";

    @Deprecated
    public static final String SERVLET_PARAMETER_PRODUCTION_MODE = InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;

    // Token file keys used for defining folder paths for dev server
    public static final String NPM_TOKEN = "npmFolder";
    public static final String FRONTEND_TOKEN = "frontendFolder";
    public static final String GENERATED_TOKEN = "generatedFolder";
    public static final String CONNECT_JAVA_SOURCE_FOLDER_TOKEN = "connect.javaSourceFolder";
    public static final String CONNECT_APPLICATION_PROPERTIES_TOKEN = "connect.applicationProperties";
    public static final String CONNECT_OPEN_API_FILE_TOKEN = "connect.openApiFile";
    public static final String CONNECT_GENERATED_TS_DIR_TOKEN = "connect.generated";
    public static final String EXTERNAL_STATS_FILE_TOKEN = "externalStatsFile";
    public static final String EXTERNAL_STATS_URL_TOKEN = "externalStatsUrl";

    @Deprecated
    public static final String SERVLET_PARAMETER_USE_V14_BOOTSTRAP = InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;
    @Deprecated
    public static final String SERVLET_PARAMETER_INITIAL_UIDL = InitParameters.SERVLET_PARAMETER_INITIAL_UIDL;
    @Deprecated
    public static final String SERVLET_PARAMETER_ENABLE_DEV_SERVER = InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
    @Deprecated
    public static final String SERVLET_PARAMETER_REUSE_DEV_SERVER = InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER;

    @Deprecated
    public static final String SERVLET_PARAMETER_REQUEST_TIMING = InitParameters.SERVLET_PARAMETER_REQUEST_TIMING;
    @Deprecated
    public static final String SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION = InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION;
    @Deprecated
    public static final String SERVLET_PARAMETER_HEARTBEAT_INTERVAL = InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL;
    @Deprecated
    public static final String SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT = InitParameters.SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT;
    @Deprecated
    public static final String SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS = InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS;
    @Deprecated
    public static final String SERVLET_PARAMETER_PUSH_MODE = InitParameters.SERVLET_PARAMETER_PUSH_MODE;
    @Deprecated
    public static final String SERVLET_PARAMETER_PUSH_URL = InitParameters.SERVLET_PARAMETER_PUSH_URL;
    @Deprecated
    public static final String SERVLET_PARAMETER_SYNC_ID_CHECK = InitParameters.SERVLET_PARAMETER_SYNC_ID_CHECK;
    @Deprecated
    public static final String SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS = InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS;
    @Deprecated
    public static final String SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING = InitParameters.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING;
    @Deprecated
    public static final String SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT = InitParameters.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT;
    @Deprecated
    public static final String SERVLET_PARAMETER_JSBUNDLE = InitParameters.SERVLET_PARAMETER_JSBUNDLE;
    @Deprecated
    public static final String SERVLET_PARAMETER_POLYFILLS = InitParameters.SERVLET_PARAMETER_POLYFILLS;

    public static final String POLYFILLS_DEFAULT_VALUE = "";

    @Deprecated
    public static final String SERVLET_PARAMETER_BROTLI = InitParameters.SERVLET_PARAMETER_BROTLI;

    @Deprecated
    public static final String I18N_PROVIDER = InitParameters.I18N_PROVIDER;

    @Deprecated
    public static final String DISABLE_AUTOMATIC_SERVLET_REGISTRATION = InitParameters.DISABLE_AUTOMATIC_SERVLET_REGISTRATION;

    @Deprecated
    public static final String COMPILED_WEB_COMPONENTS_PATH = InitParameters.COMPILED_WEB_COMPONENTS_PATH;

    @Deprecated
    public static final String SERVLET_PARAMETER_STATISTICS_JSON = InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;

    /**
     * Default path for the WebPack profile statistics json file. It can be
     * modified by setting the system property "statistics.file.path".
     */
    public static final String STATISTICS_JSON_DEFAULT = Constants.VAADIN_CONFIGURATION
            + "stats.json";

    /**
     * Name of the <code>npm</code> main file.
     */
    public static final String PACKAGE_JSON = "package.json";

    /**
     * Location for the frontend resources in jar files for compatibility mode
     * (also obsolete but supported for NPM mode).
     */
    public static final String COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT = "META-INF/resources/frontend";

    /**
     * Location for the frontend resources in jar files.
     */
    public static final String RESOURCES_FRONTEND_DEFAULT = "META-INF/frontend";

    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;

    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN;

    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN;

    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS = InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS;

    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE = InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE;

    @Deprecated
    public static final String SERVLET_PARAMETER_ENABLE_PNPM = InitParameters.SERVLET_PARAMETER_ENABLE_PNPM;

    /**
     * Constant for whether pnpm is default or not. Mojos need the value as
     * string and constant, so need to use string instead of boolean.
     */
    public static final String ENABLE_PNPM_DEFAULT_STRING = "true";

    @Deprecated
    public static final String REQUIRE_HOME_NODE_EXECUTABLE = InitParameters.REQUIRE_HOME_NODE_EXECUTABLE;

    /**
     * The path used in the vaadin servlet for handling static resources.
     */
    public static final String META_INF = "META-INF/";

    /**
     * The path used in the vaadin servlet for handling static resources.
     */
    public static final String VAADIN_MAPPING = "VAADIN/";

    /**
     * The path to meta-inf/VAADIN/ where static resources are put on the
     * servlet.
     */
    public static final String VAADIN_SERVLET_RESOURCES = META_INF
            + VAADIN_MAPPING;

    /**
     * The static build resources folder.
     */
    public static final String VAADIN_BUILD = "build/";

    /**
     * The static configuration resources folder.
     */
    public static final String VAADIN_CONFIGURATION = "config/";

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
     * The name of platform versions file.
     */
    public static final String VAADIN_VERSIONS_JSON = "vaadin_versions.json";

    @Deprecated
    public static final String SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD = InitParameters.SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD;

    private Constants() {
        // prevent instantiation constants class only
    }
}
