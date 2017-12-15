/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * A collection of properties configured at deploy time as well as a way of
 * accessing third party properties not explicitly supported by this class.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public interface DeploymentConfiguration extends Serializable {

    /**
     * Returns whether Vaadin is in production mode.
     *
     * @return true if in production mode, false otherwise.
     */
    boolean isProductionMode();

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
     * @since 7.3
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
     * Returns whether the sending of URL's as GET and POST parameters in
     * requests with content-type <code>application/x-www-form-urlencoded</code>
     * is enabled or not.
     *
     * @return <code>false</code> if set to false or <code>true</code> otherwise
     */
    boolean isSendUrlsAsParameters();

    /**
     * Returns whether a session should be closed when all its open UIs have
     * been idle for longer than its configured maximum inactivity time.
     * <p>
     * A UI is idle if it is open on the client side but has no activity other
     * than heartbeat requests. If {@code isCloseIdleSessions() == false},
     * heartbeat requests cause the session to stay open for as long as there
     * are open UIs on the client side. If it is {@code true}, the session is
     * eventually closed if the open UIs do not have any user interaction.
     *
     * @see WrappedSession#getMaxInactiveInterval()
     *
     * @since 7.0.0
     *
     * @return True if UIs and sessions receiving only heartbeat requests are
     *         eventually closed; false if heartbeat requests extend UI and
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
     * @since 7.4
     */
    String getUIClassName();

    /**
     * Gets the {@link RouterConfigurator} class configuration option value.
     *
     * @return the router configurator class name
     *
     */
    String getRouterConfiguratorClassName();

    /**
     * Gets class loader configuration option value.
     *
     * @return the configured class loader name
     * @since 7.4
     */
    String getClassLoaderName();

    /**
     * Gets the location from which the Web Components polyfill is loaded.
     * Should end in an <code>/</code>.
     *
     * @return the Web Components polyfill URI, or an empty optional if no
     *         polyfill should be loaded
     */
    Optional<String> getWebComponentsPolyfillBase();

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
        return isProductionMode()
                ? getStringProperty(Constants.FRONTEND_URL_ES6, Constants.FRONTEND_URL_ES6_DEFAULT_VALUE)
                : getDevelopmentFrontendPrefix();
    }

    /**
     * Gets the URL from which frontend resources should be loaded in ES5
     * compatible browsers.
     *
     * @return the ES5 resource URL
     */
    default String getEs5FrontendPrefix() {
        return isProductionMode()
                ? getStringProperty(Constants.FRONTEND_URL_ES5, Constants.FRONTEND_URL_ES5_DEFAULT_VALUE)
                : getDevelopmentFrontendPrefix();
    }

    /**
     * Whether to use the new annotation based routing implementation instead of
     * the old routing implementation that relies on a
     * {@link RouterConfigurator}.
     *
     * @return whether to use the new annotation based routing implementation
     */
    default boolean isUsingNewRouting() {
        return true;
    }

    /**
     * Determines if webJars mechanism is enabled. It is disabled if the user
     * have explicitly set {@link Constants#DISABLE_WEBJARS} property to
     * {@code true} or the user have not set the property at all and the
     * production mode is enabled.
     *
     * @return {@code true} if webJars are enabled, {@code false} otherwise
     */
    default boolean areWebJarsEnabled() {
        return !getBooleanProperty(Constants.DISABLE_WEBJARS,
                isProductionMode());
    }
}
