/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.Constants.VAADIN_PREFIX;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_TOKEN_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.PROJECT_BASEDIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

/**
 * Creates {@link DeploymentConfiguration} filled with all parameters specified
 * by the framework users.
 */
public final class DeploymentConfigurationFactory implements Serializable {

    public static final Object DEV_MODE_ENABLE_STRATEGY = new Serializable() {
    };

    public static final String ERROR_COMPATIBILITY_MODE_UNSET = "Unable to determine mode of operation. To use npm mode, ensure "
            + "'flow-build-info.json' exists on the classpath. With Maven, "
            + "this is handled by the 'prepare-frontend' goal. To use "
            + "compatibility mode, add the 'flow-server-compatibility-mode' "
            + "dependency. If using Vaadin with Spring Boot, instead set the "
            + "property 'vaadin.compatibilityMode' to 'true' in "
            + "'application.properties'.";

    public static final String ERROR_DEV_MODE_NO_FILES = "The compatibility mode is explicitly set to 'false', "
            + "but there are neither 'flow-build-info.json' nor 'webpack.config.js' file available in "
            + "the project/working directory. Ensure 'webpack.config.js' is present or trigger creation of "
            + "'flow-build-info.json' via running 'prepare-frontend' Maven goal.";

    public static final String DEV_FOLDER_MISSING_MESSAGE =
            "Running project in development mode with no access to folder '%s'.%n"
                    + "Build project in production mode instead, see https://vaadin.com/docs/v14/flow/production/tutorial-production-mode-basic.html";

    private DeploymentConfigurationFactory() {
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that is filled with
     * all parameters, specified for the current app.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param servletConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for the parameters
     *             fails
     */
    public static DeploymentConfiguration createDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, ServletConfig servletConfig)
            throws ServletException {
        return new DefaultDeploymentConfiguration(systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, servletConfig));
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that has all
     * parameters, specified for the current app without doing checks so
     * property states and only returns default.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param servletConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for the parameters
     *             fails
     */
    public static DeploymentConfiguration createPropertyDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, ServletConfig servletConfig)
            throws ServletException {
        return new PropertyDeploymentConfiguration(systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, servletConfig));
    }

    /**
     * Generate Property containing parameters for with all parameters contained
     * in current application.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param servletConfig
     *            the config to get the rest of the properties from
     * @return {@link Properties} instance
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for the parameters
     *             fails
     */
    protected static Properties createInitParameters(
            Class<?> systemPropertyBaseClass, ServletConfig servletConfig)
            throws ServletException {
        Properties initParameters = new Properties();
        readUiFromEnclosingClass(systemPropertyBaseClass, initParameters);
        readConfigurationAnnotation(systemPropertyBaseClass, initParameters);

        // Read default parameters from server.xml
        final ServletContext context = servletConfig.getServletContext();
        for (final Enumeration<String> e = context.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, context.getInitParameter(name));
        }

        // Override with application config from web.xml
        for (final Enumeration<String> e = servletConfig
                .getInitParameterNames(); e.hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name,
                    servletConfig.getInitParameter(name));
        }

        readBuildInfo(initParameters);
        return initParameters;
    }

    private static void readBuildInfo(Properties initParameters) {
        String json = null;
        try {
            // token file location passed via init parameter property
            String tokenLocation = initParameters.getProperty(PARAM_TOKEN_FILE);
            if (tokenLocation != null) {
                File tokenFile = new File(tokenLocation);
                if (tokenFile != null && tokenFile.canRead()) {
                    json = FileUtils.readFileToString(tokenFile, "UTF-8");
                }
            }

            // token file is in the class-path of the application
            if (json == null) {
                URL resource = DeploymentConfigurationFactory.class
                        .getClassLoader()
                        .getResource(VAADIN_SERVLET_RESOURCES + TOKEN_FILE);
                if (resource != null) {
                    json = FrontendUtils.streamToString(resource.openStream());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Read the json and set the appropriate system properties if not
        // already set.
        if (json != null) {
            JsonObject buildInfo = JsonUtil.parse(json);
            if (buildInfo.hasKey(SERVLET_PARAMETER_PRODUCTION_MODE)) {
                initParameters.setProperty(SERVLET_PARAMETER_PRODUCTION_MODE,
                        String.valueOf(buildInfo.getBoolean(
                                SERVLET_PARAMETER_PRODUCTION_MODE)));
                // Need to be sure that we remove the system property,
                // because
                // it has priority in the configuration getter
                System.clearProperty(
                        VAADIN_PREFIX + SERVLET_PARAMETER_PRODUCTION_MODE);
            }
            if (buildInfo.hasKey(SERVLET_PARAMETER_COMPATIBILITY_MODE)) {
                initParameters.setProperty(SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        String.valueOf(buildInfo.getBoolean(
                                SERVLET_PARAMETER_COMPATIBILITY_MODE)));
                // Need to be sure that we remove the system property,
                // because it has priority in the configuration getter
                System.clearProperty(
                        VAADIN_PREFIX + SERVLET_PARAMETER_COMPATIBILITY_MODE);
            }

            if (buildInfo.hasKey(NPM_TOKEN)) {
                initParameters.setProperty(PROJECT_BASEDIR,
                        buildInfo.getString(NPM_TOKEN));
                verifyFolderExists(initParameters,
                        buildInfo.getString(NPM_TOKEN));
            }

            if (buildInfo.hasKey(FRONTEND_TOKEN)) {
                initParameters.setProperty(FrontendUtils.PARAM_FRONTEND_DIR,
                        buildInfo.getString(FRONTEND_TOKEN));
                // Only verify frontend folder if it's not a subfolder of the npm folder.
                if (!buildInfo.hasKey(NPM_TOKEN) || !buildInfo
                        .getString(FRONTEND_TOKEN)
                        .startsWith(buildInfo.getString(NPM_TOKEN))) {
                    verifyFolderExists(initParameters,
                            buildInfo.getString(FRONTEND_TOKEN));
                }
            }

            // These should be internal only so if there is a System
            // property override then the user probably knows what
            // they are doing.
            if (buildInfo.hasKey(SERVLET_PARAMETER_ENABLE_DEV_SERVER)) {
                initParameters.setProperty(SERVLET_PARAMETER_ENABLE_DEV_SERVER,
                        String.valueOf(buildInfo.getBoolean(
                                SERVLET_PARAMETER_ENABLE_DEV_SERVER)));
            }
            if (buildInfo.hasKey(SERVLET_PARAMETER_REUSE_DEV_SERVER)) {
                initParameters.setProperty(SERVLET_PARAMETER_REUSE_DEV_SERVER,
                        String.valueOf(buildInfo.getBoolean(
                                SERVLET_PARAMETER_REUSE_DEV_SERVER)));
            }
        }

        try {
            boolean hasWebPackConfig = hasWebpackConfig(initParameters);
            boolean hasTokenFile = json != null;
            SerializableConsumer<CompatibilityModeStatus> strategy = value -> verifyMode(
                    value, hasTokenFile, hasWebPackConfig);
            initParameters.put(DEV_MODE_ENABLE_STRATEGY, strategy);
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
     * @param initParameters
     *         deployment init parameters
     * @param folder
     *         folder to check exists
     */
    private static void verifyFolderExists(Properties initParameters,
            String folder) {
        Boolean productionMode = Boolean.parseBoolean(initParameters
                .getProperty(SERVLET_PARAMETER_PRODUCTION_MODE, "false"));
        if(!productionMode && !new File(folder).exists()) {
            String message = String.format(DEV_FOLDER_MISSING_MESSAGE, folder);
            throw new IllegalStateException(message);
        }
    }

    private static void verifyMode(CompatibilityModeStatus value,
            boolean hasTokenFile, boolean hasWebpackConfig) {
        // Don't handle the case when compatibility mode is enabled.

        // If no compatibility mode setting is defined
        // and the project/working directory doesn't contain an appropriate
        // webpack.config.js, then show the error message.
        if (value == CompatibilityModeStatus.UNDEFINED) {
            if (!hasWebpackConfig) {
                throw new IllegalStateException(ERROR_COMPATIBILITY_MODE_UNSET);
            }
        } else if (!hasTokenFile && !hasWebpackConfig) {
            // If compatibility mode is explicitly set to false, no
            // flow-build-info.json file exists, and no appropriate
            // webpack.config.js is found in the current working directory, then
            // show an error message that suggest either triggering creation of
            // flow-bulid-info.json or ensuring webpack.config.js is present in
            // the working directory.
            throw new IllegalStateException(ERROR_DEV_MODE_NO_FILES);
        }

        // If flow-bulid-info.json doesn't exist, but an appropriate
        // webpack.config.js is found in the working directory, then launch a
        // dev server with configuration based on the project/working directory
        // location
        if (!hasTokenFile && hasWebpackConfig) {
            // the current working directory will be used automatically by the
            // dev server unless it's specified explicitly
            LoggerFactory.getLogger(DeploymentConfigurationFactory.class).warn(
                    "Found 'webpack.config.js' in the project/working directory. "
                            + "Will use it for webpack dev server.");
        }
    }

    private static boolean hasWebpackConfig(Properties initParameters)
            throws IOException {
        String baseDir = initParameters
                .getProperty(FrontendUtils.PROJECT_BASEDIR);
        File projectBaseDir = baseDir == null ? new File(".")
                : new File(baseDir);
        File webPackConfig = new File(projectBaseDir,
                FrontendUtils.WEBPACK_CONFIG);
        return FrontendUtils.isWebpackConfigFile(webPackConfig);
    }

    private static void readUiFromEnclosingClass(
            Class<?> systemPropertyBaseClass, Properties initParameters) {
        Class<?> enclosingClass = systemPropertyBaseClass.getEnclosingClass();

        if (enclosingClass != null
                && UI.class.isAssignableFrom(enclosingClass)) {
            initParameters.put(VaadinSession.UI_PARAMETER,
                    enclosingClass.getName());
        }
    }

    private static void readConfigurationAnnotation(
            Class<?> systemPropertyBaseClass, Properties initParameters)
            throws ServletException {
        Optional<VaadinServletConfiguration> optionalConfigAnnotation = AnnotationReader
                .getAnnotationFor(systemPropertyBaseClass,
                        VaadinServletConfiguration.class);

        if (!optionalConfigAnnotation.isPresent()) {
            return;
        }

        VaadinServletConfiguration configuration = optionalConfigAnnotation
                .get();
        Method[] methods = VaadinServletConfiguration.class
                .getDeclaredMethods();
        for (Method method : methods) {
            VaadinServletConfiguration.InitParameterName name = method
                    .getAnnotation(
                            VaadinServletConfiguration.InitParameterName.class);
            assert name != null : "All methods declared in VaadinServletConfiguration should have a @InitParameterName annotation";

            try {
                Object value = method.invoke(configuration);

                String stringValue;
                if (value instanceof Class<?>) {
                    stringValue = ((Class<?>) value).getName();
                } else {
                    stringValue = value.toString();
                }

                initParameters.setProperty(name.value(), stringValue);
            } catch (Exception e) {
                // This should never happen
                throw new ServletException(
                        "Could not read @VaadinServletConfiguration value "
                                + method.getName(),
                        e);
            }
        }

    }
}
