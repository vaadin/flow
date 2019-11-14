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

package com.vaadin.flow.function;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.communication.PushMode;

import static com.vaadin.flow.server.Constants.POLYFILLS_DEFAULT_VALUE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_POLYFILLS;

/**
 * A collection of properties configured at deploy time as well as a way of
 * accessing third party properties not explicitly supported by this class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface DeploymentConfiguration extends Serializable {

    /**
     * Returns whether Vaadin is in production mode.
     *
     * @return true if in production mode, false otherwise.
     */
    boolean isProductionMode();

    /**
     * Returns whether Vaadin is running in Vaadin 13 compatibility mode.
     *
     * NOTE: compatibility mode will be unsupported in future versions.
     *
     * @deprecated use {@link #isCompatibilityMode()}
     *
     * @return true if in compatibility mode, false otherwise.
     */
    @Deprecated
    boolean isBowerMode();

    /**
     * Returns whether Vaadin is running in Vaadin 13 compatibility mode.
     *
     * NOTE: compatibility mode will be unsupported in future versions.
     *
     * @return true if in compatibility mode, false otherwise.
     */
    default boolean isCompatibilityMode() {
        return isBowerMode();
    }

    /**
     * Returns whether the server provides timing info to the client.
     *
     * @return true if timing info is provided, false otherwise.
     */
    boolean isRequestTiming();

    /**
     * Returns whether cross-site request forgery protection is enabled.
     *
     * @return true if XSRF protection is enabled, false otherwise.
     */
    boolean isXsrfProtectionEnabled();

    /**
     * Returns whether sync id checking is enabled. The sync id is used to
     * gracefully handle situations when the client sends a message to a
     * connector that has recently been removed on the server.
     *
     * @return <code>true</code> if sync id checking is enabled;
     *         <code>false</code> otherwise
     */
    boolean isSyncIdCheckEnabled();

    /**
     * Returns the number of seconds between heartbeat requests of a UI, or a
     * non-positive number if heartbeat is disabled.
     *
     * @return The time between heartbeats.
     */
    int getHeartbeatInterval();

    /**
     * Returns the number of seconds that a WebComponent will wait for a
     * reconnect before removing the server-side component from memory.
     *
     * @return time to wait after a disconnect has happened
     */
    int getWebComponentDisconnect();

    /**
     * Returns whether the sending of URL's as GET and POST parameters in
     * requests with content-type <code>application/x-www-form-urlencoded</code>
     * is enabled or not.
     *
     * @return <code>false</code> if set to false or <code>true</code> otherwise
     */
    boolean isSendUrlsAsParameters();

    /**
     * Returns whether a Vaadin session should be closed when all its open UIs
     * have been idle for longer than its configured maximum inactivity time.
     * <p>
     * A UI is idle if it is open on the client side but has no activity other
     * than heartbeat requests. If {@code isCloseIdleSessions() == false},
     * heartbeat requests cause the session to stay open for as long as there
     * are open UIs on the client side. If it is {@code true}, the session is
     * eventually closed if the open UIs do not have any user interaction.
     *
     * @see WrappedSession#getMaxInactiveInterval()
     *
     *
     * @return True if UIs and Vaadin sessions receiving only heartbeat requests
     *         are eventually closed; false if heartbeat requests extend UI and
     *         session lifetime indefinitely.
     */
    boolean isCloseIdleSessions();

    /**
     * Returns the mode of bidirectional ("push") client-server communication
     * that should be used.
     *
     * @return The push mode in use.
     */
    PushMode getPushMode();

    /**
     * Returns the URL that bidirectional ("push") client-server communication
     * should use.
     *
     * @return The push URL to use
     */
    String getPushURL();

    /**
     * Gets the properties configured for the deployment, e.g. as init
     * parameters to the servlet.
     *
     * @return properties for the application.
     */
    Properties getInitParameters();

    /**
     * Gets a configured property. The properties are typically read from e.g.
     * web.xml or from system properties of the JVM.
     *
     * @param propertyName
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @param converter
     *            the way string should be converted into the required property
     * @param <T>
     *            type of a property
     * @return the property value, or the passed default value if no property
     *         value is found
     */
    <T> T getApplicationOrSystemProperty(String propertyName, T defaultValue,
            Function<String, T> converter);

    /**
     * A shorthand of
     * {@link DeploymentConfiguration#getApplicationOrSystemProperty(String, Object, Function)}
     * for {@link String} type.
     *
     * @param propertyName
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     */
    default String getStringProperty(String propertyName, String defaultValue) {
        return getApplicationOrSystemProperty(propertyName, defaultValue,
                Function.identity());
    }

    /**
     * A shorthand of
     * {@link DeploymentConfiguration#getApplicationOrSystemProperty(String, Object, Function)}
     * for {@link String} type.
     *
     * Considers {@code ""} to be equal {@code true} in order to treat params
     * like {@code -Dtest.param} as enabled ({@code test.param == true}).
     *
     * Additionally validates the property value, requiring non-empty strings to
     * be equal to boolean string representation. An exception thrown if it's
     * not true.
     *
     * @param propertyName
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     *
     * @throws IllegalArgumentException
     *             if property value string is not a boolean value
     */
    default boolean getBooleanProperty(String propertyName,
            boolean defaultValue) throws IllegalArgumentException {
        String booleanString = getStringProperty(propertyName, null);
        if (booleanString == null) {
            return defaultValue;
        } else if (booleanString.isEmpty()) {
            return true;
        } else {
            boolean parsedBoolean = Boolean.parseBoolean(booleanString);
            if (Boolean.toString(parsedBoolean)
                    .equalsIgnoreCase(booleanString)) {
                return parsedBoolean;
            } else {
                throw new IllegalArgumentException(String.format(
                        "Property named '%s' is boolean, but contains incorrect value '%s' that is not boolean '%s'",
                        propertyName, booleanString, parsedBoolean));
            }
        }
    }

    /**
     * Gets UI class configuration option value.
     *
     * @return UI class name
     *
     */
    String getUIClassName();

    /**
     * Gets class loader configuration option value.
     *
     * @return the configured class loader name
     */
    String getClassLoaderName();

    /**
     * Gets the URL from which frontend resources should be loaded during
     * development, unless explicitly configured to use the production es6 and
     * es5 URLs.
     *
     * @return the development resource URL
     */
    default String getDevelopmentFrontendPrefix() {
        return Constants.FRONTEND_URL_DEV_DEFAULT;
    }

    /**
     * Gets the URL from which frontend resources should be loaded in ES6
     * compatible browsers.
     *
     * @return the ES6 resource URL
     */
    default String getEs6FrontendPrefix() {
        return useCompiledFrontendResources()
                ? getStringProperty(Constants.FRONTEND_URL_ES6,
                        Constants.FRONTEND_URL_ES6_DEFAULT_VALUE)
                : getDevelopmentFrontendPrefix();
    }

    /**
     * Gets the URL from which frontend resources should be loaded in ES5
     * compatible browsers.
     *
     * @return the ES5 resource URL
     */
    default String getEs5FrontendPrefix() {
        return useCompiledFrontendResources()
                ? getStringProperty(Constants.FRONTEND_URL_ES5,
                        Constants.FRONTEND_URL_ES5_DEFAULT_VALUE)
                : getDevelopmentFrontendPrefix();
    }

    /**
     * Gets the URL from which frontend resources should be loaded in NPM mode.
     *
     * @return the NPM resource URL
     */
    default String getNpmFrontendPrefix() {
        return getDevelopmentFrontendPrefix();
    }

    /**
     * Determines if webJars mechanism is enabled. It is disabled if the user
     * have explicitly set the {@link Constants#DISABLE_WEBJARS} property to
     * {@code true}, or the user have not set the property at all and the
     * {@link #useCompiledFrontendResources()} returns false.
     *
     * @return {@code true} if webJars are enabled, {@code false} otherwise
     */
    default boolean areWebJarsEnabled() {
        return !getBooleanProperty(Constants.DISABLE_WEBJARS,
                useCompiledFrontendResources());
    }

    /**
     * Determines if Flow should use compiled or original frontend resources.
     *
     * User can explicitly disable bundled resources usage by setting the
     * {@link Constants#USE_ORIGINAL_FRONTEND_RESOURCES} property to
     * {@code true}.
     *
     * @return {@code true} if Flow should use compiled frontend resources.
     */
    default boolean useCompiledFrontendResources() {
        return isProductionMode() && !getBooleanProperty(
                Constants.USE_ORIGINAL_FRONTEND_RESOURCES, false);
    }

    /**
     * Determines if Flow should automatically register servlets. For more
     * information on the servlets registered, refer to
     * {@link com.vaadin.flow.server.startup.ServletDeployer} javadoc.
     *
     * User can explicitly disable automatic servlet registration by setting the
     * {@link Constants#DISABLE_AUTOMATIC_SERVLET_REGISTRATION} property to
     * {@code true}.
     *
     * @return {@code true} if Flow should not automatically register servlets
     * @see com.vaadin.flow.server.startup.ServletDeployer
     */
    default boolean disableAutomaticServletRegistration() {
        return getBooleanProperty(
                Constants.DISABLE_AUTOMATIC_SERVLET_REGISTRATION, false);
    }

    /**
     * Checks whether precompressed Brotli files should be used if available.
     *
     * @return <code>true</code> to serve precompressed Brotli files,
     *         <code>false</code> to not serve Brotli files.
     */
    default boolean isBrotli() {
        return getBooleanProperty(Constants.SERVLET_PARAMETER_BROTLI, false);
    }

    default String getCompiledWebComponentsPath() {
        return getStringProperty(Constants.COMPILED_WEB_COMPONENTS_PATH,
                "vaadin-web-components");
    }

    /**
     * Returns an array with polyfills to be loaded when the app is loaded.
     *
     * The default value is
     * <code>build/webcomponentsjs/webcomponents-loader.js</code> but it can be
     * changed by setting the {@link Constants#SERVLET_PARAMETER_POLYFILLS} as a
     * comma separated list of JS files to load.
     *
     * @return polyfills to load
     */
    default List<String> getPolyfills() {
        return Arrays.asList(getStringProperty(SERVLET_PARAMETER_POLYFILLS,
                POLYFILLS_DEFAULT_VALUE).split("[, ]+"));
    }

    /**
     * Get if the dev server should be enabled. True by default
     *
     * @return true if dev server should be used
     */
    default boolean enableDevServer() {
        return getBooleanProperty(Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER,
                true);
    }

    /**
     * Get if the dev server should be reused on each reload. True by default,
     * set it to false in tests so as dev server is not kept as a daemon after
     * the test.
     *
     * @return true if dev server should be reused
     */
    default boolean reuseDevServer() {
        return getBooleanProperty(Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER,
                true);
    }

    /**
     * Get if the stats.json file should be retrieved from an external service or
     * through the classpath.
     *
     * @return true if stats.json is served from an external location
     */
    default boolean isStatsExternal() {
        return getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false);
    }

    /**
     * Get the url from where stats.json should be retrieved from.
     * If not given this will default to '/vaadin-static/VAADIN/config/stats.json'
     *
     * @return external stats.json location
     */
    default String getExternalStatsUrl() {
        return getStringProperty(Constants.EXTERNAL_STATS_URL,
                Constants.DEFAULT_EXTERNAL_STATS_URL);
    }
}
