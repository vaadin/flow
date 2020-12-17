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

package com.vaadin.flow.function;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.communication.PushMode;

import static com.vaadin.flow.server.Constants.POLYFILLS_DEFAULT_VALUE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_POLYFILLS;

/**
 * A collection of properties configured at deploy time as well as a way of
 * accessing third party properties not explicitly supported by this class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface DeploymentConfiguration
        extends AbstractConfiguration, Serializable {

    /**
     * Returns whether the server provides timing info to the client.
     *
     * @return true if timing info is provided, false otherwise.
     */
    boolean isRequestTiming();

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
     * In certain cases, such as when combining XmlHttpRequests and push over
     * low bandwidth connections, messages may be received out of order by the
     * client. This property specifies the maximum time (in milliseconds) that
     * the client will then wait for the predecessors of a received out-order
     * message, before considering them missing and requesting a full
     * resynchronization of the application state from the server.
     * 
     * @return The maximum message suspension timeout
     */
    int getMaxMessageSuspendTimeout();

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
    @Override
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
     * Determines if Flow should automatically register servlets. For more
     * information on the servlets registered, refer to
     * {@link com.vaadin.flow.server.startup.ServletDeployer} javadoc.
     *
     * User can explicitly disable automatic servlet registration by setting the
     * {@link InitParameters#DISABLE_AUTOMATIC_SERVLET_REGISTRATION} property to
     * {@code true}.
     *
     * @return {@code true} if Flow should not automatically register servlets
     * @see com.vaadin.flow.server.startup.ServletDeployer
     */
    default boolean disableAutomaticServletRegistration() {
        return getBooleanProperty(
                InitParameters.DISABLE_AUTOMATIC_SERVLET_REGISTRATION, false);
    }

    /**
     * Checks whether precompressed Brotli files should be used if available.
     *
     * @return <code>true</code> to serve precompressed Brotli files,
     *         <code>false</code> to not serve Brotli files.
     */
    default boolean isBrotli() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_BROTLI,
                false);
    }

    default String getCompiledWebComponentsPath() {
        return getStringProperty(InitParameters.COMPILED_WEB_COMPONENTS_PATH,
                "vaadin-web-components");
    }

    /**
     * Returns an array with polyfills to be loaded when the app is loaded.
     *
     * The default value is empty, but it can be changed by setting the
     * {@link InitParameters#SERVLET_PARAMETER_POLYFILLS} as a comma separated
     * list of JS files to load.
     *
     * @return polyfills to load
     */
    default List<String> getPolyfills() {
        return Arrays
                .asList(getStringProperty(SERVLET_PARAMETER_POLYFILLS,
                        POLYFILLS_DEFAULT_VALUE).split("[, ]+"))
                .stream().filter(polyfill -> !polyfill.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Get if the stats.json file should be retrieved from an external service
     * or through the classpath.
     *
     * @return true if stats.json is served from an external location
     */
    default boolean isStatsExternal() {
        return getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false);
    }

    /**
     * Get the url from where stats.json should be retrieved from. If not given
     * this will default to '/vaadin-static/VAADIN/config/stats.json'
     *
     * @return external stats.json location
     */
    default String getExternalStatsUrl() {
        return getStringProperty(Constants.EXTERNAL_STATS_URL,
                Constants.DEFAULT_EXTERNAL_STATS_URL);
    }

    /**
     * Get if the bootstrap page should include the initial UIDL fragment. This
     * only makes sense for the client-side bootstrapping.
     * <p>
     * By default it is <code>false</code>.
     * <p>
     * Enabling this flag, it will make the initial application load a couple of
     * seconds faster in very slow networks because of the extra round-trip to
     * request the UIDL after the index.html is loaded.
     * <p>
     * Otherwise, keeping the flag as false is beneficial, specially in
     * application that mix client and server side views, since the `index.html`
     * can be cached and served by service workers in PWAs, as well as in the
     * server side session and UI initialization is deferred until a server view
     * is actually requested by the user, saving some server resources.
     *
     * @return true if initial UIDL should be included in page
     */
    default boolean isEagerServerLoad() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_INITIAL_UIDL,
                false);
    }

    /**
     * Checks if dev mode live reload is enabled or not.
     *
     * @return {@code true} if dev mode live reload is enabled, {@code false}
     *         otherwise
     */
    boolean isDevModeLiveReloadEnabled();

}
