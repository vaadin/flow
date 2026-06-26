/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.Set;

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
    public static final String REQUIRED_ATMOSPHERE_RUNTIME_VERSION = "3.0.5.slf4jvaadin1";

    /**
     * The prefix used for System property parameters.
     * 
     * @since 2.0
     */
    public static final String VAADIN_PREFIX = "vaadin.";

    // Token file keys used for defining folder paths for dev server
    public static final String NPM_TOKEN = "npmFolder";
    public static final String FRONTEND_TOKEN = "frontendFolder";
    public static final String CONNECT_JAVA_SOURCE_FOLDER_TOKEN = "connect.javaSourceFolder";
    public static final String JAVA_RESOURCE_FOLDER_TOKEN = "javaResourceFolder";
    public static final String CONNECT_APPLICATION_PROPERTIES_TOKEN = "connect.applicationProperties";
    public static final String CONNECT_OPEN_API_FILE_TOKEN = "connect.openApiFile";
    public static final String PROJECT_FRONTEND_GENERATED_DIR_TOKEN = "project.frontend.generated";
    public static final String EXTERNAL_STATS_FILE_TOKEN = "externalStatsFile";
    public static final String EXTERNAL_STATS_URL_TOKEN = "externalStatsUrl";
    public static final String DAU_TOKEN = "dau.enable";
    public static final String PREMIUM_FEATURES = "premiumFeatures.enable";
    public static final String COMMERCIAL_BANNER_TOKEN = "commercialBanner.enable";

    public static final String POLYFILLS_DEFAULT_VALUE = "";

    /**
     * Default path for the frontend statistics json file. It can be modified by
     * setting the system property "statistics.file.path".
     * 
     * @since 2.0
     */
    public static final String STATISTICS_JSON_DEFAULT = Constants.VAADIN_CONFIGURATION
            + "stats.json";

    /**
     * Default resource directory to place template sources in. This is used
     * used for Vite production mode instead of a stats.json file.
     * 
     * @since 9.0
     */
    public static final String TEMPLATE_DIRECTORY = Constants.VAADIN_CONFIGURATION
            + "templates/";

    /**
     * Name of the <code>npm</code> main file.
     * 
     * @since 2.0
     */
    public static final String PACKAGE_JSON = "package.json";

    /**
     * Name of the <code>npm</code> version locking file.
     * 
     * @since 23.0
     */
    public static final String PACKAGE_LOCK_JSON = "package-lock.json";

    /**
     * Name of the <code>pnpm</code> version locking file.
     * 
     * @since 24.1
     */
    public static final String PACKAGE_LOCK_YAML = "pnpm-lock.yaml";

    /**
     * Name of the <code>bun</code> version locking file.
     * 
     * @since 24.3
     */
    public static final String PACKAGE_LOCK_BUN = "bun.lockb";

    /**
     * Name of the <code>bun</code> version locking file, starting from bun 1.2.
     * 
     * @since 24.6.3
     */
    public static final String PACKAGE_LOCK_BUN_1_2 = "bun.lock";

    /**
     * Target folder constant.
     * 
     * @since 7.0
     */
    public static final String TARGET = "target";

    /**
     * Location for the frontend resources in jar files for compatibility mode
     * (also obsolete but supported for npm mode).
     * 
     * @since 2.0.11
     */
    public static final String COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT = "META-INF/resources/frontend";

    /**
     * Location for the frontend resources in jar files.
     * 
     * @since 2.0
     */
    public static final String RESOURCES_FRONTEND_DEFAULT = "META-INF/frontend";

    /**
     * The name of the application theme root folder.
     * 
     * @since 6.0
     */
    public static final String APPLICATION_THEME_ROOT = "themes";

    /**
     * Location for the resources in jar files.
     * 
     * @since 6.0
     */
    public static final String RESOURCES_JAR_DEFAULT = "META-INF/resources/";

    /**
     * Location for the theme resources in jar files.
     * 
     * @since 6.0
     */
    public static final String RESOURCES_THEME_JAR_DEFAULT = RESOURCES_JAR_DEFAULT
            + APPLICATION_THEME_ROOT + "/";

    /**
     * Constant for whether pnpm is default or not.
     * 
     * @since 9.0
     */
    public static final boolean ENABLE_PNPM_DEFAULT = false;

    /**
     * Constant for whether bun is default or not.
     * 
     * @since 24.3
     */
    public static final boolean ENABLE_BUN_DEFAULT = false;

    /**
     * Constant for setting the pinned supported version of pnpm to be used by
     * default.
     * 
     * @since 9.0
     */
    public static final boolean GLOBAL_PNPM_DEFAULT = false;

    /**
     * The default value for
     * {@link InitParameters#REQUIRE_HOME_NODE_EXECUTABLE}.
     * 
     * @since 9.0
     */
    public static final boolean DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE = false;

    /**
     * The name of the environment variable that controls whether server-side
     * usage statistics is enabled.
     *
     * Usage statistics are disabled if the environment variable is set to
     * "false".
     * 
     * @since 25.0
     */
    public static final String VAADIN_USAGE_STATS_ENABLED = "VAADIN_USAGE_STATS_ENABLED";

    /**
     * The default value for whether usage statistics is enabled.
     * 
     * @since 9.0.2
     */
    public static final boolean DEFAULT_DEVMODE_STATS = !"false"
            .equalsIgnoreCase(
                    System.getenv(Constants.VAADIN_USAGE_STATS_ENABLED));

    /**
     * Internal parameter which prevent validation for annotations which are
     * allowed on an AppShell class
     * 
     * @since 4.0
     */
    public static final String ALLOW_APPSHELL_ANNOTATIONS = "allow.appshell.annotations";

    /**
     * The path used in the vaadin servlet for handling static resources.
     * 
     * @since 2.0
     */
    public static final String META_INF = "META-INF/";

    /**
     * The path used in the vaadin servlet for handling static resources.
     * 
     * @since 2.0
     */
    public static final String VAADIN_MAPPING = "VAADIN/";

    /**
     * The path used in the vaadin servlet for handling push.
     * 
     * @since 23.3
     */
    public static final String PUSH_MAPPING = VAADIN_MAPPING + "push";

    /**
     * The static build resources folder.
     * 
     * @since 2.0
     */
    public static final String VAADIN_BUILD = "build/";

    /**
     * The static configuration resources folder.
     * 
     * @since 2.0
     */
    public static final String VAADIN_CONFIGURATION = "config/";

    /**
     * The static resources root folder.
     * 
     * @since 6.0
     */
    public static final String VAADIN_WEBAPP = "webapp/";

    /**
     * The generated PWA icons folder.
     * 
     * @since 24.6
     */
    public static final String VAADIN_PWA_ICONS = "pwa-icons/";

    /**
     * The path to meta-inf/VAADIN/ where static resources are put on the
     * servlet.
     * 
     * @since 2.0
     */
    public static final String VAADIN_SERVLET_RESOURCES = META_INF
            + VAADIN_MAPPING;

    /**
     * The path to webapp/ public resources root.
     * 
     * @since 6.0
     */
    public static final String VAADIN_WEBAPP_RESOURCES = VAADIN_SERVLET_RESOURCES
            + VAADIN_WEBAPP;

    /**
     * The prefix used for all internal static files, relative to context root.
     * 
     * @since 2.0
     */
    public static final String VAADIN_BUILD_FILES_PATH = VAADIN_MAPPING
            + VAADIN_BUILD;

    /**
     * Default path for local frontend resources packaged for jar add-ons.
     * 
     * @since 2.0.5
     */
    public static final String LOCAL_FRONTEND_RESOURCES_PATH = "src/main/resources/META-INF/resources/frontend";

    /**
     * Property boolean for marking stats.json to be fetched from external
     * location.
     * 
     * @since 2.2
     */
    public static final String EXTERNAL_STATS_FILE = "external.stats.file";
    /**
     * Property String for external stats.json location url.
     * 
     * @since 2.2
     */
    public static final String EXTERNAL_STATS_URL = "external.stats.url";
    /**
     * Default location to look for the external stats.json.
     * 
     * @since 2.2
     */
    public static final String DEFAULT_EXTERNAL_STATS_URL = "/vaadin-static/VAADIN/config/stats.json";

    /**
     * A request parameter that can be given in browser to force the Vaadin
     * application to create a new UI and session instance, thus overriding
     * {@code @PreserveOnRefresh} annotation.
     * 
     * @since 3.0
     */
    public static final String URL_PARAMETER_RESTART_APPLICATION = "restartApplication";

    /**
     * A request parameter that can be given in browser to force the Vaadin
     * application to close an existing UI and session. Unlike
     * {@link #URL_PARAMETER_RESTART_APPLICATION}, this will not create a new
     * session.
     * 
     * @since 3.0
     */
    public static final String URL_PARAMETER_CLOSE_APPLICATION = "closeApplication";

    /**
     * UsageEntry name for UsageStatistics BootstrapHandler.
     * 
     * @since 3.0
     */
    public static final String STATISTIC_FLOW_BOOTSTRAPHANDLER = "flow/BootstrapHandler";

    /**
     * UsageEntry name for UsageStatistics Routing Server.
     * 
     * @since 3.0
     */
    public static final String STATISTIC_ROUTING_SERVER = "routing/server";

    /**
     * UsageEntry name for UsageStatistics Routing Client.
     * 
     * @since 3.0
     */
    public static final String STATISTIC_ROUTING_CLIENT = "routing/client";

    /**
     * UsageEntry name for UsageStatistics Hybrid.
     * 
     * @since 3.0
     */
    public static final String STATISTIC_ROUTING_HYBRID = "routing/hybrid";

    /**
     * UsageEntry name for Flow routes definitions. Marked used, if
     * RouteRegistry is not empty.
     * 
     * @since 24.4
     */
    public static final String STATISTIC_HAS_FLOW_ROUTE = "has-flow-route";

    /**
     * UsageEntry name for automatic layout. Marked used, if Layout annotation
     * is used or RouteRegistry#setLayout is used directly.
     * 
     * @since 24.5
     */
    public static final String STATISTIC_HAS_AUTO_LAYOUT = "has-auto-layout";

    /**
     * UsageEntry name for client route using automatic layout. Marked used, if
     * AvailableViewInfo#flowLayout is true for any client route.
     * 
     * @since 24.5
     */
    public static final String STATISTIC_HAS_CLIENT_ROUTE_WITH_AUTO_LAYOUT = "has-auto-layout/client";

    /**
     * UsageEntry name for server route using automatic layout. Marked used, if
     * any server route's layout matches Layout annotated layout.
     * 
     * @since 24.5
     */
    public static final String STATISTIC_HAS_SERVER_ROUTE_WITH_AUTO_LAYOUT = "has-auto-layout/server";

    /**
     * UsageEntry name for exported web components. Marked used, if either
     * WebComponentExporter or WebComponentExporterFactory is found in a project
     * 
     * @since 24.4
     */
    public static final String STATISTIC_HAS_EXPORTED_WC = "has-exported-wc";

    /**
     * UsageEntry for rendering a Flow route. Marked as used, if a user
     * navigates to a Flow route and navigation doesn't end up with an error.
     * 
     * @since 24.4
     */
    public static final String STATISTICS_FLOW_ROUTER = "flow-router";

    /**
     * UsageEntry for rendering an exported web component. Marked as used, if an
     * exported web component is instantiated on the server.
     * 
     * @since 24.4
     */
    public static final String STATISTICS_EXPORTED_WC = "exported-wc";

    /**
     * The name of platform core components and tools versions file.
     * 
     * @since 23.1.3
     */
    public static final String VAADIN_CORE_VERSIONS_JSON = "vaadin-core-versions.json";

    /**
     * The name of platform commercial components and tools versions file.
     * 
     * @since 2.2
     */
    public static final String VAADIN_VERSIONS_JSON = "vaadin-versions.json";

    /**
     * Default live reload port as defined in Spring Boot Dev Tools.
     * 
     * @since 4.0
     */
    public static final int SPRING_BOOT_DEFAULT_LIVE_RELOAD_PORT = 35729;

    /**
     * The name of the default dev bundle for the Express Build mode.
     * 
     * @since 24.0
     */
    public static final String DEV_BUNDLE_NAME = "vaadin-dev-bundle";

    /**
     * The name of the default production bundle.
     * 
     * @since 24.1
     */
    public static final String PROD_BUNDLE_NAME = "vaadin-prod-bundle";

    /**
     * The folder in the project where Flow generates Express Build mode
     * application dev bundle.
     * 
     * @since 24.0
     */
    public static final String DEV_BUNDLE_LOCATION = "dev-bundle";

    /**
     * The folder where the bundle zip files are stored.
     * 
     * @since 24.3
     */
    public static final String BUNDLE_LOCATION = "src/main/bundles/";

    /**
     * The file name of the compressed development bundle.
     * 
     * @since 24.3
     */
    public static final String DEV_BUNDLE_COMPRESSED_FILE = "dev.bundle";

    /**
     * Location of the compressed development bundle file.
     * 
     * @since 24.3
     */
    public static final String DEV_BUNDLE_COMPRESSED_FILE_LOCATION = BUNDLE_LOCATION
            + DEV_BUNDLE_COMPRESSED_FILE;

    /**
     * The path part where dev-bundle is located inside the jar.
     * 
     * @since 24.0
     */
    public static final String DEV_BUNDLE_JAR_PATH = DEV_BUNDLE_NAME + "/";

    /**
     * The path part where production bundle is located inside the jar.
     * 
     * @since 24.1
     */
    public static final String PROD_BUNDLE_JAR_PATH = PROD_BUNDLE_NAME + "/";

    /**
     * The file name of the compressed production bundle.
     * 
     * @since 24.3
     */
    public static final String PROD_BUNDLE_COMPRESSED_FILE = "prod.bundle";

    /**
     * Location of the compressed production bundle file.
     * 
     * @since 24.3
     */
    public static final String PROD_BUNDLE_COMPRESSED_FILE_LOCATION = BUNDLE_LOCATION
            + PROD_BUNDLE_COMPRESSED_FILE;

    /**
     * The directory name inside dev bundle for the frontend assets.
     * 
     * @since 24.0
     */
    public static final String ASSETS = "assets";

    /**
     * Name of the temporary file storing internal flag showing that Flow needs
     * to re-build the production bundle or not.
     * 
     * @since 24.1
     */
    public static final String NEEDS_BUNDLE_BUILD_FILE = Constants.VAADIN_CONFIGURATION
            + "needs-build";

    /**
     * Key for storing the value of `alwaysExecutePrepareFrontend` flag of
     * Gradle builds in a build info (token) file.
     * 
     * @since 24.2
     */
    public static final String DISABLE_PREPARE_FRONTEND_CACHE = "disable.prepare.frontend.cache";

    /**
     * Attribute used by HasSize to mark elements that have been set to full
     * width.
     * 
     * @since 24.7
     */
    public static final String ATTRIBUTE_WIDTH_FULL = "data-width-full";

    /**
     * Attribute used by HasSize to mark elements that have been set to full
     * height.
     * 
     * @since 24.7
     */
    public static final String ATTRIBUTE_HEIGHT_FULL = "data-height-full";

    /**
     * maximum allowed size of a complete request for multipart stream upload
     * requests.
     * 
     * @since 24.8
     */
    public static final long DEFAULT_REQUEST_SIZE_MAX = -1;

    /**
     * maximum allowed size of a single uploaded file for multipart stream
     * upload requests.
     * 
     * @since 24.8
     */
    public static final long DEFAULT_FILE_SIZE_MAX = -1;

    /**
     * maximum number of files allowed per multipart stream upload requests.
     * 
     * @since 24.8
     */
    public static final long DEFAULT_FILE_COUNT_MAX = 10000;

    /**
     * Default set of URL schemes considered safe when no custom set is
     * configured through {@link InitParameters#URL_SAFE_SCHEMES}.
     * <p>
     * Script-capable schemes such as {@code javascript} and {@code data} are
     * intentionally excluded as they can be used to execute scripts in the
     * browser when used as a link or frame target.
     * 
     * @since 25.2
     */
    public static final Set<String> DEFAULT_URL_SAFE_SCHEMES = Set.of("http",
            "https", "mailto", "tel", "ftp");

    /**
     * Special {@link InitParameters#URL_SAFE_SCHEMES} entry that marks every
     * scheme as safe, disabling scheme validation. Mixing this entry with other
     * schemes still disables validation.
     * 
     * @since 25.2
     */
    public static final String URL_SAFE_SCHEMES_WILDCARD = "*";

    private Constants() {
        // prevent instantiation constants class only
    }
}
