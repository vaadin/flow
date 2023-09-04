/*
 * Copyright 2000-2023 Vaadin Ltd.
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
    public static final String REQUIRED_ATMOSPHERE_RUNTIME_VERSION = "3.0.3.slf4jvaadin1";

    /**
     * The prefix used for System property parameters.
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

    public static final String POLYFILLS_DEFAULT_VALUE = "";

    /**
     * Default path for the frontend statistics json file. It can be modified by
     * setting the system property "statistics.file.path".
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
     * Name of the <code>pnpm</code> version locking ile.
     */
    public static final String PACKAGE_LOCK_YAML = "pnpm-lock.yaml";

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
     * The default value for
     * {@link InitParameters#REQUIRE_HOME_NODE_EXECUTABLE}.
     */
    public static final boolean DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE = false;

    /**
     * The default value for whether usage statistics is enabled.
     */
    public static final boolean DEFAULT_DEVMODE_STATS = true;

    /**
     * Internal parameter which prevent validation for annotations which are
     * allowed on an AppShell class
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
     * Default live reload port as defined in Spring Boot Dev Tools.
     */
    public static final int SPRING_BOOT_DEFAULT_LIVE_RELOAD_PORT = 35729;

    /**
     * The name of the default dev bundle for the Express Build mode.
     */
    public static final String DEV_BUNDLE_NAME = "vaadin-dev-bundle";

    /**
     * The name of the default production bundle.
     */
    public static final String PROD_BUNDLE_NAME = "vaadin-prod-bundle";

    /**
     * The folder in the project where Flow generates Express Build mode
     * application dev bundle.
     */
    public static final String DEV_BUNDLE_LOCATION = "src/main/dev-bundle";

    /**
     * The path part where dev-bundle is located inside the jar.
     */
    public static final String DEV_BUNDLE_JAR_PATH = DEV_BUNDLE_NAME + "/";

    /**
     * The path part where production bundle is located inside the jar.
     */
    public static final String PROD_BUNDLE_JAR_PATH = PROD_BUNDLE_NAME + "/";

    /**
     * The directory name inside dev bundle for the frontend assets.
     */
    public static final String ASSETS = "assets";

    /**
     * Name of the temporary file storing internal flag showing that Flow needs
     * to re-build the production bundle or not.
     */
    public static final String NEEDS_BUNDLE_BUILD_FILE = Constants.VAADIN_CONFIGURATION
            + "needs-build";

    /**
     * Key for storing the value of `alwaysExecutePrepareFrontend` flag of
     * Gradle builds in a build info (token) file.
     */
    public static final String DISABLE_PREPARE_FRONTEND_CACHE = "disable.prepare.frontend.cache";

    private Constants() {
        // prevent instantiation constants class only
    }
}
