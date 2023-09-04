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
package com.vaadin.flow.server.startup;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_OPEN_API_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.DISABLE_PREPARE_FRONTEND_CACHE;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_FILE;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_URL;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_URL_TOKEN;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.PROJECT_FRONTEND_GENERATED_DIR_TOKEN;
import static com.vaadin.flow.server.Constants.VAADIN_PREFIX;
import static com.vaadin.flow.server.InitParameters.BUILD_FOLDER;
import static com.vaadin.flow.server.InitParameters.FRONTEND_HOTDEPLOY;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
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
            JsonObject buildInfo) {
        Map<String, String> params = new HashMap<>();
        if (buildInfo.hasKey(SERVLET_PARAMETER_PRODUCTION_MODE)) {
            params.put(SERVLET_PARAMETER_PRODUCTION_MODE, String.valueOf(
                    buildInfo.getBoolean(SERVLET_PARAMETER_PRODUCTION_MODE)));
        }
        if (buildInfo.hasKey(EXTERNAL_STATS_FILE_TOKEN)
                || buildInfo.hasKey(EXTERNAL_STATS_URL_TOKEN)) {
            params.put(EXTERNAL_STATS_FILE, Boolean.toString(true));
            if (buildInfo.hasKey(EXTERNAL_STATS_URL_TOKEN)) {
                params.put(EXTERNAL_STATS_URL,
                        buildInfo.getString(EXTERNAL_STATS_URL_TOKEN));
            }
            // NO OTHER CONFIGURATION:
            return params;
        }
        if (buildInfo.hasKey(SERVLET_PARAMETER_INITIAL_UIDL)) {
            params.put(SERVLET_PARAMETER_INITIAL_UIDL, String.valueOf(
                    buildInfo.getBoolean(SERVLET_PARAMETER_INITIAL_UIDL)));
            // Need to be sure that we remove the system property,
            // because it has priority in the configuration getter
            System.clearProperty(
                    VAADIN_PREFIX + SERVLET_PARAMETER_INITIAL_UIDL);
        }

        if (buildInfo.hasKey(NPM_TOKEN)) {
            params.put(PROJECT_BASEDIR, buildInfo.getString(NPM_TOKEN));
            verifyFolderExists(params, buildInfo.getString(NPM_TOKEN));
        }

        if (buildInfo.hasKey(NODE_VERSION)) {
            params.put(NODE_VERSION, buildInfo.getString(NODE_VERSION));
        }
        if (buildInfo.hasKey(NODE_DOWNLOAD_ROOT)) {
            params.put(NODE_DOWNLOAD_ROOT,
                    buildInfo.getString(NODE_DOWNLOAD_ROOT));
        }

        if (buildInfo.hasKey(FRONTEND_TOKEN)) {
            params.put(FrontendUtils.PARAM_FRONTEND_DIR,
                    buildInfo.getString(FRONTEND_TOKEN));
            // Only verify frontend folder if it's not a subfolder of the
            // npm folder.
            if (!buildInfo.hasKey(NPM_TOKEN)
                    || !buildInfo.getString(FRONTEND_TOKEN)
                            .startsWith(buildInfo.getString(NPM_TOKEN))) {
                verifyFolderExists(params, buildInfo.getString(FRONTEND_TOKEN));
            }
        }

        // These should be internal only so if there is a System
        // property override then the user probably knows what
        // they are doing.
        if (buildInfo.hasKey(FRONTEND_HOTDEPLOY)) {
            params.put(FRONTEND_HOTDEPLOY,
                    String.valueOf(buildInfo.getBoolean(FRONTEND_HOTDEPLOY)));
        } else if (buildInfo.hasKey(SERVLET_PARAMETER_ENABLE_DEV_SERVER)) {
            params.put(FRONTEND_HOTDEPLOY, String.valueOf(
                    buildInfo.getBoolean(SERVLET_PARAMETER_ENABLE_DEV_SERVER)));
        }
        if (buildInfo.hasKey(SERVLET_PARAMETER_REUSE_DEV_SERVER)) {
            params.put(SERVLET_PARAMETER_REUSE_DEV_SERVER, String.valueOf(
                    buildInfo.getBoolean(SERVLET_PARAMETER_REUSE_DEV_SERVER)));
        }
        if (buildInfo.hasKey(CONNECT_JAVA_SOURCE_FOLDER_TOKEN)) {
            params.put(CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                    buildInfo.getString(CONNECT_JAVA_SOURCE_FOLDER_TOKEN));
        }
        if (buildInfo.hasKey(Constants.JAVA_RESOURCE_FOLDER_TOKEN)) {
            params.put(Constants.JAVA_RESOURCE_FOLDER_TOKEN,
                    buildInfo.getString(Constants.JAVA_RESOURCE_FOLDER_TOKEN));
        }
        if (buildInfo.hasKey(CONNECT_OPEN_API_FILE_TOKEN)) {
            params.put(CONNECT_OPEN_API_FILE_TOKEN,
                    buildInfo.getString(CONNECT_OPEN_API_FILE_TOKEN));
        }
        if (buildInfo.hasKey(CONNECT_APPLICATION_PROPERTIES_TOKEN)) {
            params.put(CONNECT_APPLICATION_PROPERTIES_TOKEN,
                    buildInfo.getString(CONNECT_APPLICATION_PROPERTIES_TOKEN));
        }
        if (buildInfo.hasKey(PROJECT_FRONTEND_GENERATED_DIR_TOKEN)) {
            params.put(PROJECT_FRONTEND_GENERATED_DIR_TOKEN,
                    buildInfo.getString(PROJECT_FRONTEND_GENERATED_DIR_TOKEN));
        }
        if (buildInfo.hasKey(BUILD_FOLDER)) {
            params.put(BUILD_FOLDER, buildInfo.getString(BUILD_FOLDER));
        }
        if (buildInfo.hasKey(DISABLE_PREPARE_FRONTEND_CACHE)) {
            UsageStatistics.markAsUsed("flow/always-execute-prepare-frontend",
                    null);
        }

        setDevModePropertiesUsingTokenData(params, buildInfo);
        return params;
    }

    /**
     * Sets to the dev mode properties to the configuration parameters.
     *
     * @see #getConfigParametersUsingTokenData(JsonObject)
     *
     * @param params
     *            the configuration parameters to set dev mode properties to
     * @param buildInfo
     *            the token file data
     */
    protected void setDevModePropertiesUsingTokenData(
            Map<String, String> params, JsonObject buildInfo) {
        // read dev mode properties from the token and set init parameter only
        // if it's not yet set
        if (params.get(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM) == null
                && buildInfo
                        .hasKey(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)) {
            params.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                    String.valueOf(buildInfo.getBoolean(
                            InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)));
        }
        if (params.get(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE) == null
                && buildInfo
                        .hasKey(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE)) {
            params.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE,
                    String.valueOf(buildInfo.getBoolean(
                            InitParameters.REQUIRE_HOME_NODE_EXECUTABLE)));
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
