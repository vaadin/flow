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

package com.vaadin.flow.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.VaadinUriResolver;
import com.vaadin.shared.communication.PushMode;

/**
 * The default implementation of {@link DeploymentConfiguration} based on a base
 * class for resolving system properties and a set of init parameters.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public class DefaultDeploymentConfiguration
        extends AbstractDeploymentConfiguration {
    private static final String SEPARATOR = "\n===========================================================";
    private static final String WEB_COMPONENTS_LOADER_JS_NAME = "webcomponents-loader.js";
    private static final String CONTEXT_ROOT_PATH = "/";

    public static final String NOT_PRODUCTION_MODE_INFO = SEPARATOR
            + "\nVaadin is running in DEBUG MODE.\nAdd productionMode=true to web.xml "
            + "to disable debug features.\nTo show debug window, add ?debug to "
            + "your application URL." + SEPARATOR;

    public static final String WARNING_XSRF_PROTECTION_DISABLED = SEPARATOR
            + "\nWARNING: Cross-site request forgery protection is disabled!"
            + SEPARATOR;

    public static final String WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC = SEPARATOR
            + "\nWARNING: heartbeatInterval has been set to a non integer value "
            + "in web.xml. The default of 5min will be used." + SEPARATOR;

    public static final String WARNING_PUSH_MODE_NOT_RECOGNIZED = SEPARATOR
            + "\nWARNING: pushMode has been set to an unrecognized value\n"
            + "in web.xml. The permitted values are \"disabled\", \"manual\",\n"
            + "and \"automatic\". The default of \"disabled\" will be used."
            + SEPARATOR;

    /**
     * Default value for {@link #getHeartbeatInterval()} = {@value} .
     */
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 300;

    /**
     * Default value for {@link #isCloseIdleSessions()} = {@value} .
     */
    public static final boolean DEFAULT_CLOSE_IDLE_SESSIONS = false;

    /**
     * Default value for {@link #isSyncIdCheckEnabled()} = {@value} .
     *
     * @since 7.3
     */
    public static final boolean DEFAULT_SYNC_ID_CHECK = true;

    public static final boolean DEFAULT_SEND_URLS_AS_PARAMETERS = true;

    private final Properties initParameters;
    private boolean productionMode;
    private boolean xsrfProtectionEnabled;
    private int heartbeatInterval;
    private boolean closeIdleSessions;
    private PushMode pushMode;
    private final Class<?> systemPropertyBaseClass;
    private boolean syncIdCheck;
    private boolean sendUrlsAsParameters;
    private String webComponentsPolyfillBase;
    private boolean usingNewRouting;

    /**
     * Create a new deployment configuration instance.
     *
     * @param systemPropertyBaseClass
     *            the class that should be used as a basis when reading system
     *            properties
     * @param initParameters
     *            the init parameters that should make up the foundation for
     *            this configuration
     * @param resourceScanner
     *            callback for traversing the available resources, not
     *            <code>null</code>
     */
    public DefaultDeploymentConfiguration(Class<?> systemPropertyBaseClass,
            Properties initParameters,
            BiConsumer<String, Predicate<String>> resourceScanner) {
        assert resourceScanner != null;

        this.initParameters = initParameters;
        this.systemPropertyBaseClass = systemPropertyBaseClass;

        checkProductionMode();
        checkXsrfProtection();
        checkHeartbeatInterval();
        checkCloseIdleSessions();
        checkPushMode();
        checkSyncIdCheck();
        checkSendUrlsAsParameters();
        checkWebComponentsPolyfillBase(resourceScanner);
        checkUsingNewRouting();
    }

    @Override
    public <T> T getApplicationOrSystemProperty(String propertyName,
            T defaultValue, Function<String, T> converter) {
        // Try system properties
        String val = getSystemProperty(propertyName);
        if (val != null) {
            return converter.apply(val);
        }

        // Try application properties
        val = getApplicationProperty(propertyName);
        if (val != null) {
            return converter.apply(val);
        }

        return defaultValue;
    }

    /**
     * Gets an system property value.
     *
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    protected String getSystemProperty(String parameterName) {
        String pkgName;
        final Package pkg = systemPropertyBaseClass.getPackage();
        if (pkg != null) {
            pkgName = pkg.getName();
        } else {
            final String className = systemPropertyBaseClass.getName();
            int index = className.lastIndexOf('.');
            if (index >= 0) {
                pkgName = className.substring(0, index);
            } else {
                pkgName = null;
            }
        }
        if (pkgName == null) {
            pkgName = "";
        } else {
            pkgName += '.';
        }
        String val = System.getProperty(pkgName + parameterName);
        if (val != null) {
            return val;
        }

        // Try lowercased system properties
        val = System.getProperty(
                pkgName + parameterName.toLowerCase(Locale.ENGLISH));

        if (val != null) {
            return val;
        }

        // version prefixed with just "vaadin."
        val = System.getProperty("vaadin." + parameterName);

        return val;
    }

    /**
     * Gets an application property value.
     *
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    public String getApplicationProperty(String parameterName) {

        String val = initParameters.getProperty(parameterName);
        if (val != null) {
            return val;
        }

        // Try lower case application properties for backward compatibility with
        // 3.0.2 and earlier
        val = initParameters.getProperty(parameterName.toLowerCase());

        return val;
    }

    /**
     * {@inheritDoc}
     *
     * The default is false.
     */
    @Override
    public boolean isProductionMode() {
        return productionMode;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default is true.
     */
    @Override
    public boolean isXsrfProtectionEnabled() {
        return xsrfProtectionEnabled;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default interval is 300 seconds (5 minutes).
     */
    @Override
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default value is false.
     */
    @Override
    public boolean isCloseIdleSessions() {
        return closeIdleSessions;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default value is <code>true</code>.
     */
    @Override
    public boolean isSyncIdCheckEnabled() {
        return syncIdCheck;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default value is <code>true</code>.
     */
    @Override
    public boolean isSendUrlsAsParameters() {
        return sendUrlsAsParameters;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default mode is {@link PushMode#DISABLED}.
     */
    @Override
    public PushMode getPushMode() {
        return pushMode;
    }

    @Override
    public Optional<String> getWebComponentsPolyfillBase() {
        return Optional.ofNullable(webComponentsPolyfillBase);
    }

    @Override
    public Properties getInitParameters() {
        return initParameters;
    }

    @Override
    public boolean isUsingNewRouting() {
        return usingNewRouting;
    }

    /**
     * Log a warning if Vaadin is not running in production mode.
     */
    private void checkProductionMode() {
        productionMode = getBooleanProperty(
                Constants.SERVLET_PARAMETER_PRODUCTION_MODE, false);
        if (!productionMode) {
            getLogger().warn(NOT_PRODUCTION_MODE_INFO);
        }
    }

    /**
     * Log a warning if cross-site request forgery protection is disabled.
     */
    private void checkXsrfProtection() {
        xsrfProtectionEnabled = !getBooleanProperty(
                Constants.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION, false);
        if (!xsrfProtectionEnabled) {
            getLogger().warn(WARNING_XSRF_PROTECTION_DISABLED);
        }
    }

    private void checkHeartbeatInterval() {
        try {
            heartbeatInterval = getApplicationOrSystemProperty(
                    Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                    DEFAULT_HEARTBEAT_INTERVAL, Integer::parseInt);
        } catch (NumberFormatException e) {
            getLogger().warn(WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC);
            heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        }
    }

    private void checkCloseIdleSessions() {
        closeIdleSessions = getBooleanProperty(
                Constants.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS,
                DEFAULT_CLOSE_IDLE_SESSIONS);
    }

    private void checkPushMode() {
        try {
            pushMode = getApplicationOrSystemProperty(
                    Constants.SERVLET_PARAMETER_PUSH_MODE, PushMode.DISABLED,
                    stringMode -> Enum.valueOf(PushMode.class,
                            stringMode.toUpperCase()));
        } catch (IllegalArgumentException e) {
            getLogger().warn(WARNING_PUSH_MODE_NOT_RECOGNIZED);
            pushMode = PushMode.DISABLED;
        }
    }

    private void checkSyncIdCheck() {
        syncIdCheck = getBooleanProperty(
                Constants.SERVLET_PARAMETER_SYNC_ID_CHECK,
                DEFAULT_SYNC_ID_CHECK);
    }

    private void checkSendUrlsAsParameters() {
        sendUrlsAsParameters = getBooleanProperty(
                Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                DEFAULT_SEND_URLS_AS_PARAMETERS);
    }

    private void checkUsingNewRouting() {
        usingNewRouting = getBooleanProperty(
                Constants.SERVLET_PARAMETER_USING_NEW_ROUTING, true);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getName());
    }

    private void checkWebComponentsPolyfillBase(
            BiConsumer<String, Predicate<String>> resourceScanner) {
        String propertyValue = getStringProperty(
                Constants.SERVLET_PARAMETER_POLYFILL_BASE, null);
        if (null == propertyValue) {
            Optional<String> locatedPolyfill = getWebComponentsPolyfill(
                    resourceScanner);
            if (!locatedPolyfill.isPresent()) {
                getLogger().warn(
                        "Unable to locate polyfill {}, proceeding without it",
                        WEB_COMPONENTS_LOADER_JS_NAME);
            }
            propertyValue = locatedPolyfill.orElse(null);
        } else if (propertyValue.trim().isEmpty()) {
            propertyValue = null;
        }
        webComponentsPolyfillBase = propertyValue;
    }

    private Optional<String> getWebComponentsPolyfill(
            BiConsumer<String, Predicate<String>> resourceScanner) {
        Set<String> excludedDirectories = new HashSet<>(
                Arrays.asList("node/", "node_modules/"));
        if (!areWebJarsEnabled()) {
            excludedDirectories.add("webjars/");
        }

        Optional<String> frontendDirectoryPath = getFrontendDirectoryPath();
        Set<String> frontendDirectoryPolyfills = frontendDirectoryPath
                .map(searchBase -> locatePolyfills(resourceScanner, searchBase,
                        excludedDirectories))
                .orElse(Collections.emptySet());
        if (frontendDirectoryPolyfills.isEmpty()) {
            getLogger().info(
                    "Unable to find {} polyfill in frontend directory, searching the whole context",
                    WEB_COMPONENTS_LOADER_JS_NAME);
            frontendDirectoryPath.ifPresent(excludedDirectories::add);
            return getPolyfillBasePath(
                    ApplicationConstants.CONTEXT_PROTOCOL_PREFIX,
                    locatePolyfills(resourceScanner, CONTEXT_ROOT_PATH,
                            excludedDirectories));
        } else {
            return getPolyfillBasePath(
                    ApplicationConstants.FRONTEND_PROTOCOL_PREFIX,
                    frontendDirectoryPolyfills);
        }
    }

    private Optional<String> getFrontendDirectoryPath() {
        VaadinUriResolver uriResolver = new VaadinUriResolver() {
            @Override
            protected String getContextRootUrl() {
                // ServletContext.getResource expects a leading slash
                return CONTEXT_ROOT_PATH;
            }

            @Override
            protected String getFrontendRootUrl() {
                return getEs6FrontendPrefix();
            }
        };
        String scanBase = uriResolver.resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        if (!scanBase.startsWith(CONTEXT_ROOT_PATH)) {
            String message = formatDefaultPolyfillMessage( String
                .format( "Cannot automatically find the %s polyfill because the property "
                        + "'%s' value is not absolute (doesn't start with '/')",
                    WEB_COMPONENTS_LOADER_JS_NAME, Constants.FRONTEND_URL_ES6 ) );
            getLogger().warn(message);
            return Optional.empty();
        }
        return Optional.of(scanBase);
    }

    private Set<String> locatePolyfills(
            BiConsumer<String, Predicate<String>> resourceScanner,
            String scanBase, Set<String> exclusions) {
        Set<String> visitedPaths = new HashSet<>();
        Set<String> foundPolyfills = new HashSet<>();
        resourceScanner.accept(scanBase, path -> {
            boolean notExcludedPath = !visitedPaths.contains(path)
                    && exclusions.stream().noneMatch(path::contains);
            if (notExcludedPath) {
                visitedPaths.add(path);
                if (path.endsWith(WEB_COMPONENTS_LOADER_JS_NAME)) {
                    foundPolyfills.add(path);
                }
            }
            return notExcludedPath;
        });
        return foundPolyfills;
    }

    private Optional<String> getPolyfillBasePath(String prefix,
            Set<String> polyfills) {
        if (polyfills.isEmpty()) {
            return Optional.empty();
        }
        if (polyfills.size() > 1) {
            getLogger().warn(
                    "Have located multiple {} polyfills: '{}', using the first one",
                    WEB_COMPONENTS_LOADER_JS_NAME, polyfills);
        }

        String fileName = polyfills.iterator().next();
        String dirName = fileName.substring(0, fileName.lastIndexOf('/'));

        getLogger().info("Will use {} polyfill discovered in {}", WEB_COMPONENTS_LOADER_JS_NAME, dirName);
        return Optional.of(prefix + dirName + '/');
    }

    private static String formatDefaultPolyfillMessage(String baseMessage) {
        return String.format("%1$s%n"
                + "Configure %2$s with an empty value to explicitly disable web components polyfill loading.%n"
                + "Configure %2$s with an explicit value to use that location instead of scanning for an implementation.",
                baseMessage, Constants.SERVLET_PARAMETER_POLYFILL_BASE);
    }
}
