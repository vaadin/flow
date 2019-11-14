/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_FILE;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_URL;
import static com.vaadin.flow.server.Constants.EXTERNAL_STATS_URL_TOKEN;
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
 *
 * @since 1.2
 */
public final class DeploymentConfigurationFactory implements Serializable {

    public static final Object DEV_MODE_ENABLE_STRATEGY = new Serializable() {
    };

    public static final Object FALLBACK_CHUNK = new Serializable() {
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

    public static final String DEV_FOLDER_MISSING_MESSAGE = "Running project in development mode with no access to folder '%s'.%n"
            + "Build project in production mode instead, see https://vaadin.com/docs/v14/flow/production/tutorial-production-mode-basic.html";

    private DeploymentConfigurationFactory() {
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that is filled with
     * all parameters, specified for the current app.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param vaadinConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     * @throws VaadinConfigurationException thrown if property construction fails
     */
    public static DeploymentConfiguration createDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig)
            throws VaadinConfigurationException {
        return new DefaultDeploymentConfiguration(systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, vaadinConfig));
    }

    /**
     * Creates a {@link DeploymentConfiguration} instance that has all
     * parameters, specified for the current app without doing checks so
     * property states and only returns default.
     *
     * @param systemPropertyBaseClass
     *            the class to look for properties defined with annotations
     * @param vaadinConfig
     *            the config to get the rest of the properties from
     * @return {@link DeploymentConfiguration} instance
     * @throws VaadinConfigurationException thrown if property construction fails
     */
    public static DeploymentConfiguration createPropertyDeploymentConfiguration(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig)
            throws VaadinConfigurationException {
        return new PropertyDeploymentConfiguration(systemPropertyBaseClass,
                createInitParameters(systemPropertyBaseClass, vaadinConfig));
    }

    /**
     * Generate Property containing parameters for with all parameters contained
     * in current application.
     *
     * @param systemPropertyBaseClass
     *         the class to look for properties defined with annotations
     * @param vaadinConfig
     *         the config to get the rest of the properties from
     * @return {@link Properties} instance
     * @throws VaadinConfigurationException thrown if property construction fails
     */
    protected static Properties createInitParameters(
            Class<?> systemPropertyBaseClass, VaadinConfig vaadinConfig)
            throws VaadinConfigurationException {
        Properties initParameters = new Properties();
        readUiFromEnclosingClass(systemPropertyBaseClass, initParameters);
        readConfigurationAnnotation(systemPropertyBaseClass, initParameters);

        // Read default parameters from server.xml
        final VaadinContext context = vaadinConfig.getVaadinContext();
        for (final Enumeration<String> e = context.getContextParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, context.getContextParameter(name));
        }

        // Override with application config from web.xml
        for (final Enumeration<String> e = vaadinConfig
                .getConfigParameterNames(); e.hasMoreElements(); ) {
            final String name = e.nextElement();
            initParameters
                    .setProperty(name, vaadinConfig.getConfigParameter(name));
        }

        readBuildInfo(initParameters);
        return initParameters;
    }

    private static void readBuildInfo(Properties initParameters) {
        String json = getTokenFileContents(initParameters);

        // Read the json and set the appropriate system properties if not
        // already set.
        if (json != null) {
            JsonObject buildInfo = JsonUtil.parse(json);
            if (buildInfo.hasKey(EXTERNAL_STATS_FILE_TOKEN) || buildInfo
                    .hasKey(EXTERNAL_STATS_URL_TOKEN)) {
                // If external stats file is flagged then we should always run in
                // npm production mode.
                initParameters.setProperty(SERVLET_PARAMETER_PRODUCTION_MODE,
                        Boolean.toString(true));
                initParameters.setProperty(SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        Boolean.toString(false));
                initParameters.setProperty(SERVLET_PARAMETER_ENABLE_DEV_SERVER,
                        Boolean.toString(false));
                initParameters.setProperty(EXTERNAL_STATS_FILE,
                        Boolean.toString(true));
                if (buildInfo.hasKey(EXTERNAL_STATS_URL_TOKEN)) {
                    initParameters.setProperty(EXTERNAL_STATS_URL,
                            buildInfo.getString(EXTERNAL_STATS_URL_TOKEN));
                }
                return;
            }
            if (buildInfo.hasKey(SERVLET_PARAMETER_PRODUCTION_MODE)) {
                initParameters.setProperty(SERVLET_PARAMETER_PRODUCTION_MODE,
                        String.valueOf(buildInfo.getBoolean(
                                SERVLET_PARAMETER_PRODUCTION_MODE)));
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
                // Only verify frontend folder if it's not a subfolder of the
                // npm folder.
                if (!buildInfo.hasKey(NPM_TOKEN)
                        || !buildInfo.getString(FRONTEND_TOKEN)
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

            FallbackChunk fallbackChunk = FrontendUtils
                    .readFallbackChunk(buildInfo);
            if (fallbackChunk != null) {
                initParameters.put(FALLBACK_CHUNK, fallbackChunk);
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

    private static String getTokenFileContents(Properties initParameters) {
        String json = null;
        try {
            json = getResourceFromFile(initParameters);
            if (json == null) {
                json = getResourceFromClassloader();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return json;
    }

    private static String getResourceFromFile(Properties initParameters)
            throws IOException {
        String json = null;
        // token file location passed via init parameter property
        String tokenLocation = initParameters.getProperty(PARAM_TOKEN_FILE);
        if (tokenLocation != null) {
            File tokenFile = new File(tokenLocation);
            if (tokenFile != null && tokenFile.canRead()) {
                json = FileUtils
                        .readFileToString(tokenFile, StandardCharsets.UTF_8);
            }
        }
        return json;
    }

    private static String getResourceFromClassloader()
            throws IOException {
        String json = null;
        // token file is in the class-path of the application
        String tokenResource = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;
        List<URL> resources = Collections
                .list(DeploymentConfiguration.class.getClassLoader()
                        .getResources(tokenResource));
        // Accept resource that doesn't contain
        // 'jar!/META-INF/Vaadin/config/flow-build-info.json'
        URL resource = resources.stream()
                .filter(url -> !url.getPath().endsWith("jar!/" + tokenResource))
                .findFirst().orElse(null);
        if (resource == null && !resources.isEmpty()) {
            // For no non jar build info, in production mode check for
            // webpack.generated.json if it's in a jar in a jar then accept
            // single jar flow-build-info.
            json = getPossibleJarResource(resources);
        } else if (resource != null) {
            json = FrontendUtils.streamToString(resource.openStream());
        }
        return json;
    }

    private static boolean isProductionMode(Properties initParameters) {
        String booleanString;
        // First check system property then initParameters
        if (System
                .getProperty(VAADIN_PREFIX + SERVLET_PARAMETER_PRODUCTION_MODE)
                != null) {
            booleanString = System.getProperty(
                    VAADIN_PREFIX + SERVLET_PARAMETER_PRODUCTION_MODE);
        } else {
            booleanString = initParameters
                    .getProperty(SERVLET_PARAMETER_PRODUCTION_MODE,
                            Boolean.FALSE.toString());
        }

        boolean parsedBoolean = Boolean.parseBoolean(booleanString);
        if (Boolean.toString(parsedBoolean).equalsIgnoreCase(booleanString)) {
            return parsedBoolean;
        }
        LoggerFactory.getLogger(DeploymentConfigurationFactory.class)
                .debug(String
                        .format("Property named '%s' is boolean, but contains incorrect value '%s' that is not boolean '%s'",
                                SERVLET_PARAMETER_PRODUCTION_MODE,
                                booleanString, parsedBoolean));
        return false;
    }

    /**
     * Check if the webpack.generated.js resources is inside 2 jars
     * (flow-server.jar and application.jar) if this is the case then we can
     * accept a build info file from inside  jar with a single jar in the path.
     *
     * @param resources
     *         flow-build-info url resource files
     * @return flow-build-info json string or <code>null</code> if no applicable files found
     * @throws IOException
     *         exception reading stream
     */
    private static String getPossibleJarResource(List<URL> resources)
            throws IOException {
        URL webpackGenerated = DeploymentConfiguration.class.getClassLoader()
                .getResource(FrontendUtils.WEBPACK_GENERATED);
        // If jar!/ exists 2 times for webpack.generated.json then we are
        // running from a jar
        if (countInstances(webpackGenerated.getPath(), "jar!/") >= 2) {
            for (URL resource : resources) {
                // As we now know that we are running from a jar we can accept a
                // build info with a single jar in the path
                if (countInstances(resource.getPath(), "jar!/") == 1) {
                    return FrontendUtils.streamToString(resource.openStream());
                }
            }
        }
        // No applicable resources found.
        return null;
    }

    private static int countInstances(String input, String value) {
        return input.split(value, -1).length - 1;
    }

    /**
     * Verify that given folder actually exists on the system if we are not in
     * production mode.
     * <p>
     * If folder doesn't exist throw IllegalStateException saying that this
     * should probably be a production mode build.
     *
     * @param initParameters
     *            deployment init parameters
     * @param folder
     *            folder to check exists
     */
    private static void verifyFolderExists(Properties initParameters,
            String folder) {
        Boolean productionMode = Boolean.parseBoolean(initParameters
                .getProperty(SERVLET_PARAMETER_PRODUCTION_MODE, "false"));
        if (!productionMode && !new File(folder).exists()) {
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

    /**
     * Read the VaadinServletConfiguration annotation for initialization name
     * value pairs and add them to the intial properties object.
     *
     * @param systemPropertyBaseClass
     *         base class for constructing the configuration
     * @param initParameters
     *         current initParameters object
     * @throws VaadinConfigurationException
     *         exception thrown for failure in invoking method on configuration
     *         annotation
     */
    private static void readConfigurationAnnotation(
            Class<?> systemPropertyBaseClass, Properties initParameters)
            throws VaadinConfigurationException {
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
            } catch (IllegalAccessException | InvocationTargetException e) {
                // This should never happen
                throw new VaadinConfigurationException(
                        "Could not read @VaadinServletConfiguration value "
                                + method.getName(), e);
            }
        }
    }
}
