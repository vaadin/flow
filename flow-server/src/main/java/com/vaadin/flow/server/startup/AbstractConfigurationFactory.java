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
package com.vaadin.flow.server.startup;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import tools.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_OPEN_API_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.DAU_TOKEN;
import static com.vaadin.flow.server.Constants.DISABLE_PREPARE_FRONTEND_CACHE;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_FILE;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_URL;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_URL_TOKEN;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.PREMIUM_FEATURES;
import static com.vaadin.flow.server.Constants.PROJECT_FRONTEND_GENERATED_DIR_TOKEN;
import static com.vaadin.flow.server.Constants.VAADIN_PREFIX;
import static com.vaadin.flow.server.Constants.COMMERCIAL_BANNER_TOKEN;
import static com.vaadin.flow.server.InitParameters.APPLICATION_IDENTIFIER;
import static com.vaadin.flow.server.InitParameters.BUILD_FOLDER;
import static com.vaadin.flow.server.InitParameters.FRONTEND_HOTDEPLOY;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.InitParameters.NPM_EXCLUDE_WEB_COMPONENTS;
import static com.vaadin.flow.server.InitParameters.REACT_ENABLE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_INITIAL_UIDL;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.PROJECT_BASEDIR;

/**
 * A configuration factory base logic which reads the token file.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class AbstractConfigurationFactory implements Serializable {

    public static final String DEV_FOLDER_MISSING_MESSAGE = "Running project in development mode with no access to folder '%s'.%n"
            + "Build project in production mode instead, see https://vaadin.com/docs/latest/flow/production/overview";

    /**
     * Returns the config parameters from the token file data {@code buildInfo}.
     *
     * @param buildInfo
     *            the token file data
     * @return the config parameters
     */
    protected Map<String, String> getConfigParametersUsingTokenData(
            JsonNode buildInfo) {
        Map<String, String> params = new HashMap<>();
        if (buildInfo.has(SERVLET_PARAMETER_PRODUCTION_MODE)) {
            params.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                    String.valueOf(
                            buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE)
                                    .booleanValue()));
        }
        if (buildInfo.has(EXTERNAL_STATS_FILE_TOKEN)
                || buildInfo.has(EXTERNAL_STATS_URL_TOKEN)) {
            params.put(EXTERNAL_STATS_FILE, Boolean.toString(true));
            if (buildInfo.has(EXTERNAL_STATS_URL_TOKEN)) {
                params.put(EXTERNAL_STATS_URL,
                        buildInfo.get(EXTERNAL_STATS_URL_TOKEN).textValue());
            }
            // NO OTHER CONFIGURATION:
            return params;
        }
        if (buildInfo.has(SERVLET_PARAMETER_INITIAL_UIDL)) {
            params.put(SERVLET_PARAMETER_INITIAL_UIDL, String.valueOf(buildInfo
                    .get(SERVLET_PARAMETER_INITIAL_UIDL).booleanValue()));
            // Need to be sure that we remove the system property,
            // because it has priority in the configuration getter
            System.clearProperty(
                    VAADIN_PREFIX + SERVLET_PARAMETER_INITIAL_UIDL);
        }

        if (buildInfo.has(NPM_TOKEN)) {
            params.put(PROJECT_BASEDIR, buildInfo.get(NPM_TOKEN).textValue());
            verifyFolderExists(params, buildInfo.get(NPM_TOKEN).textValue());
        }

        if (buildInfo.has(NODE_VERSION)) {
            params.put(NODE_VERSION, buildInfo.get(NODE_VERSION).textValue());
        }
        if (buildInfo.has(NODE_DOWNLOAD_ROOT)) {
            params.put(NODE_DOWNLOAD_ROOT,
                    buildInfo.get(NODE_DOWNLOAD_ROOT).textValue());
        }

        if (buildInfo.has(FRONTEND_TOKEN)) {
            params.put(FrontendUtils.PARAM_FRONTEND_DIR,
                    buildInfo.get(FRONTEND_TOKEN).textValue());
            // Only verify frontend folder if it's not a subfolder of the
            // npm folder.
            if (!buildInfo.has(NPM_TOKEN)
                    || !buildInfo.get(FRONTEND_TOKEN).textValue()
                            .startsWith(buildInfo.get(NPM_TOKEN).textValue())) {
                verifyFolderExists(params,
                        buildInfo.get(FRONTEND_TOKEN).textValue());
            }
        }

        // These should be internal only so if there is a System
        // property override then the user probably knows what
        // they are doing.
        if (buildInfo.has(FRONTEND_HOTDEPLOY)) {
            params.put(FRONTEND_HOTDEPLOY, String
                    .valueOf(buildInfo.get(FRONTEND_HOTDEPLOY).booleanValue()));
        }
        if (buildInfo.has(SERVLET_PARAMETER_REUSE_DEV_SERVER)) {
            params.put(SERVLET_PARAMETER_REUSE_DEV_SERVER,
                    String.valueOf(
                            buildInfo.get(SERVLET_PARAMETER_REUSE_DEV_SERVER)
                                    .booleanValue()));
        }
        if (buildInfo.has(CONNECT_JAVA_SOURCE_FOLDER_TOKEN)) {
            params.put(CONNECT_JAVA_SOURCE_FOLDER_TOKEN, buildInfo
                    .get(CONNECT_JAVA_SOURCE_FOLDER_TOKEN).textValue());
        }
        if (buildInfo.has(Constants.JAVA_RESOURCE_FOLDER_TOKEN)) {
            params.put(Constants.JAVA_RESOURCE_FOLDER_TOKEN, buildInfo
                    .get(Constants.JAVA_RESOURCE_FOLDER_TOKEN).textValue());
        }
        if (buildInfo.has(CONNECT_OPEN_API_FILE_TOKEN)) {
            params.put(CONNECT_OPEN_API_FILE_TOKEN,
                    buildInfo.get(CONNECT_OPEN_API_FILE_TOKEN).textValue());
        }
        if (buildInfo.has(CONNECT_APPLICATION_PROPERTIES_TOKEN)) {
            params.put(CONNECT_APPLICATION_PROPERTIES_TOKEN, buildInfo
                    .get(CONNECT_APPLICATION_PROPERTIES_TOKEN).textValue());
        }
        if (buildInfo.has(PROJECT_FRONTEND_GENERATED_DIR_TOKEN)) {
            params.put(PROJECT_FRONTEND_GENERATED_DIR_TOKEN, buildInfo
                    .get(PROJECT_FRONTEND_GENERATED_DIR_TOKEN).textValue());
        }
        if (buildInfo.has(BUILD_FOLDER)) {
            params.put(BUILD_FOLDER, buildInfo.get(BUILD_FOLDER).textValue());
        }
        if (buildInfo.has(DISABLE_PREPARE_FRONTEND_CACHE)) {
            UsageStatistics.markAsUsed("flow/always-execute-prepare-frontend",
                    null);
        }
        if (buildInfo.has(REACT_ENABLE)) {
            params.put(REACT_ENABLE,
                    String.valueOf(buildInfo.get(REACT_ENABLE).booleanValue()));
        }
        if (buildInfo.has(APPLICATION_IDENTIFIER)) {
            params.put(APPLICATION_IDENTIFIER,
                    buildInfo.get(APPLICATION_IDENTIFIER).textValue());
        }
        if (buildInfo.has(DAU_TOKEN)) {
            params.put(DAU_TOKEN,
                    String.valueOf(buildInfo.get(DAU_TOKEN).booleanValue()));
        }
        if (buildInfo.has(PREMIUM_FEATURES)) {
            params.put(PREMIUM_FEATURES, String
                    .valueOf(buildInfo.get(PREMIUM_FEATURES).booleanValue()));
        }

        if (buildInfo.has(InitParameters.FRONTEND_EXTRA_EXTENSIONS)) {
            params.put(InitParameters.FRONTEND_EXTRA_EXTENSIONS, buildInfo
                    .get(InitParameters.FRONTEND_EXTRA_EXTENSIONS).textValue());
        }

        if (buildInfo.has(NPM_EXCLUDE_WEB_COMPONENTS)) {
            params.put(NPM_EXCLUDE_WEB_COMPONENTS, String.valueOf(
                    buildInfo.get(NPM_EXCLUDE_WEB_COMPONENTS).booleanValue()));
        }

        if (buildInfo.has(COMMERCIAL_BANNER_TOKEN)) {
            params.put(COMMERCIAL_BANNER_TOKEN, String.valueOf(
                    buildInfo.get(COMMERCIAL_BANNER_TOKEN).booleanValue()));
        }

        setDevModePropertiesUsingTokenData(params, buildInfo);
        return params;
    }

    /**
     * Sets to the dev mode properties to the configuration parameters.
     *
     * @see #getConfigParametersUsingTokenData(JsonNode)
     *
     * @param params
     *            the configuration parameters to set dev mode properties to
     * @param buildInfo
     *            the token file data
     */
    protected void setDevModePropertiesUsingTokenData(
            Map<String, String> params, JsonNode buildInfo) {
        // read dev mode properties from the token and set init parameter only
        // if it's not yet set
        if (params.get(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM) == null
                && buildInfo
                        .has(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)) {
            params.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                    String.valueOf(buildInfo
                            .get(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)
                            .booleanValue()));
        }
        if (params.get(InitParameters.SERVLET_PARAMETER_ENABLE_BUN) == null
                && buildInfo.has(InitParameters.SERVLET_PARAMETER_ENABLE_BUN)) {
            params.put(InitParameters.SERVLET_PARAMETER_ENABLE_BUN,
                    String.valueOf(buildInfo
                            .get(InitParameters.SERVLET_PARAMETER_ENABLE_BUN)
                            .booleanValue()));
        }
        if (params.get(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE) == null
                && buildInfo.has(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE)) {
            params.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE,
                    String.valueOf(buildInfo
                            .get(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE)
                            .booleanValue()));
        }
    }

    /**
     * Gets the content of the token file with given {@code locationProvider}.
     *
     * @param locationProvider
     *            the token file location provider
     * @return the token file location, may be {@code null}
     */
    protected String getTokenFileContent(
            Function<String, String> locationProvider) {
        String location = locationProvider
                .apply(FrontendUtils.PARAM_TOKEN_FILE);
        String json = null;
        // token file location passed via init parameter property
        try {
            if (location != null) {
                File tokenFile = new File(location);
                if (tokenFile != null && tokenFile.canRead()) {
                    json = FileUtils.readFileToString(tokenFile,
                            StandardCharsets.UTF_8);
                }
            }
            return json;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Verify that given folder actually exists on the system if we are not in
     * production mode.
     * <p>
     * If folder doesn't exist throw IllegalStateException saying that this
     * should probably be a production mode build.
     *
     * @param params
     *            parameters map
     * @param folder
     *            folder to check exists
     */
    protected void verifyFolderExists(Map<String, String> params,
            String folder) {
        Boolean productionMode = Boolean.parseBoolean(params
                .getOrDefault(SERVLET_PARAMETER_PRODUCTION_MODE, "false"));
        if (!productionMode && !new File(folder).exists()) {
            String message = String.format(DEV_FOLDER_MISSING_MESSAGE, folder);
            throw new IllegalStateException(message);
        }
    }
}
