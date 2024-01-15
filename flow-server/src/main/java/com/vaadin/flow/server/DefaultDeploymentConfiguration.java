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

package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * The default implementation of {@link DeploymentConfiguration} based on a base
 * class for resolving system properties and a set of init parameters.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultDeploymentConfiguration
        extends PropertyDeploymentConfiguration {

    public static final String NOT_PRODUCTION_MODE_INFO = "\nVaadin is running in DEBUG MODE.\n"
            + "When deploying application for production, remember to disable debug features. See more from https://vaadin.com/docs/";

    public static final String WARNING_COMPATIBILITY_MODE = "Running in Vaadin 13 (Flow 1) compatibility mode.\n\n"
            + "This mode uses webjars/Bower for client side dependency management and HTML imports for dependency loading.\n\n"
            + "The default mode in Vaadin 14+ (Flow 2+) is based on npm for dependency management and JavaScript modules for dependency inclusion.\n\n"
            + "See http://vaadin.com/docs for more information.\n\n"
            + "Note: WebJars/Bower support has been deprecated and will be removed in the near future.";

    public static final String WARNING_LIVERELOAD_DISABLED_AND_NEW_LICENSE_CHECKER = "Server-side and offline new license checking features are enabled "
            + "while the development mode live reload is not available.\n"
            + "New license checking requires enabled live reload and would fallback to old license checker otherwise.\n"
            + "Check that the application is not running in compatibility mode, live reload is not disabled and dev server is enabled.";

    public static final String NOT_PRODUCTION_MODE_WARNING = "\nWARNING: Vaadin is running in DEBUG MODE with debug features enabled, but with a prebuild frontend bundle (production ready).\n"
            + "When deploying application for production, disable debug features by enabling production mode!\n"
            + "See more from https://vaadin.com/docs/v14/flow/production/overview";

    private static final String DEPLOYMENT_WARNINGS = "Following issues were discovered with deployment configuration:";

    public static final String WARNING_XSRF_PROTECTION_DISABLED = "WARNING: Cross-site request forgery protection is disabled!";

    public static final String WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC = "WARNING: heartbeatInterval has been set to a non integer value."
            + "\n The default of 5min will be used.";

    public static final String WARNING_PUSH_MODE_NOT_RECOGNIZED = "WARNING: pushMode has been set to an unrecognized value.\n"
            + "The permitted values are \"disabled\", \"manual\",\n"
            + "and \"automatic\". The default of \"disabled\" will be used.";

    /**
     * Default value for {@link #getHeartbeatInterval()} = {@value} .
     */
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 300;

    /**
     * Default value for {@link #getMaxMessageSuspendTimeout()} ()} = {@value} .
     */
    public static final int DEFAULT_MAX_MESSAGE_SUSPEND_TIMEOUT = 5000;

    /**
     * Default value for {@link #getWebComponentDisconnect()} = {@value}.
     */
    public static final int DEFAULT_WEB_COMPONENT_DISCONNECT = 300;

    /**
     * Default value for {@link #isCloseIdleSessions()} = {@value} .
     */
    public static final boolean DEFAULT_CLOSE_IDLE_SESSIONS = false;

    /**
     * Default value for {@link #isSyncIdCheckEnabled()} = {@value} .
     */
    public static final boolean DEFAULT_SYNC_ID_CHECK = true;

    public static final boolean DEFAULT_SEND_URLS_AS_PARAMETERS = true;

    private boolean productionMode;
    private boolean compatibilityMode;
    private boolean xsrfProtectionEnabled;
    private int heartbeatInterval;
    private int maxMessageSuspendTimeout;
    private int webComponentDisconnect;
    private boolean closeIdleSessions;
    private PushMode pushMode;
    private String pushURL;
    private boolean syncIdCheck;
    private boolean sendUrlsAsParameters;
    private boolean requestTiming;

    private static AtomicBoolean logging = new AtomicBoolean(true);
    private List<String> warnings = new ArrayList<>();
    private List<String> info = new ArrayList<>();

    /**
     * Create a new deployment configuration instance.
     *
     * @param systemPropertyBaseClass
     *            the class that should be used as a basis when reading system
     *            properties
     * @param initParameters
     *            the init parameters that should make up the foundation for
     *            this configuration
     */
    public DefaultDeploymentConfiguration(Class<?> systemPropertyBaseClass,
            Properties initParameters) {
        super(systemPropertyBaseClass, initParameters);

        boolean log = logging.getAndSet(false);

        checkProductionMode(log);
        checkCompatibilityMode(log);
        checkNewLicenseChecker(log);
        checkRequestTiming();
        checkXsrfProtection(log);
        checkHeartbeatInterval();
        checkMaxMessageSuspendTimeout();
        checkWebComponentDisconnectTimeout();
        checkCloseIdleSessions();
        checkPushMode();
        checkPushURL();
        checkSyncIdCheck();
        checkSendUrlsAsParameters();

        if (log) {
            logMessages();
        }
    }

    private void logMessages() {
        Logger logger = LoggerFactory.getLogger(getClass().getName());

        if (!warnings.isEmpty()) {
            warnings.add(0, DEPLOYMENT_WARNINGS);
            // merging info messages to warnings for now
            warnings.addAll(info);
            if (logger.isWarnEnabled()) {
                logger.warn(String.join("\n", warnings));
            }
        } else if (!info.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info(String.join("\n", info));
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default is false.
     */
    @Override
    public boolean isProductionMode() {
        return productionMode;
    }

    /**
     * {@inheritDoc}
     *
     * The default is false.
     */
    @Override
    public boolean isBowerMode() {
        return compatibilityMode;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default is <code>true</code> when not in production and
     * <code>false</code> when in production mode.
     */
    @Override
    public boolean isRequestTiming() {
        return requestTiming;
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
     * The default max message suspension time is 5000 milliseconds.
     */
    @Override
    public int getMaxMessageSuspendTimeout() {
        return maxMessageSuspendTimeout;
    }

    @Override
    public int getWebComponentDisconnect() {
        return webComponentDisconnect;
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

    /**
     * {@inheritDoc}
     * <p>
     * The default mode is <code>""</code> which uses the servlet URL.
     */
    @Override
    public String getPushURL() {
        return pushURL;
    }

    /**
     * Log a warning if Vaadin is not running in production mode.
     */
    private void checkProductionMode(boolean log) {
        productionMode = getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, false);
        if (log) {
            if (productionMode) {
                info.add("Vaadin is running in production mode.");
            } else {
                if (enableDevServer()) {
                    info.add(NOT_PRODUCTION_MODE_INFO);
                } else {
                    warnings.add(NOT_PRODUCTION_MODE_WARNING);
                }
            }
        }
    }

    /**
     * Log a warning if Vaadin is running in compatibility mode. Throw
     * {@link IllegalStateException} if the mode could not be determined from
     * parameters.
     */
    private void checkCompatibilityMode(boolean logWarning) {
        boolean explicitlySet = false;
        if (getStringProperty(InitParameters.SERVLET_PARAMETER_BOWER_MODE,
                null) != null) {
            compatibilityMode = getBooleanProperty(
                    InitParameters.SERVLET_PARAMETER_BOWER_MODE, false);
            explicitlySet = true;
        } else if (getStringProperty(
                InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                null) != null) {
            compatibilityMode = getBooleanProperty(
                    InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE, false);
            explicitlySet = true;
        }

        @SuppressWarnings("unchecked")
        Consumer<CompatibilityModeStatus> consumer = (Consumer<CompatibilityModeStatus>) getInitParameters()
                .get(DeploymentConfigurationFactory.DEV_MODE_ENABLE_STRATEGY);
        if (consumer != null) {
            if (explicitlySet && !compatibilityMode) {
                consumer.accept(CompatibilityModeStatus.EXPLICITLY_SET_FALSE);
            } else if (!explicitlySet) {
                consumer.accept(CompatibilityModeStatus.UNDEFINED);
            }
        }

        if (compatibilityMode && logWarning) {
            warnings.add(WARNING_COMPATIBILITY_MODE);
        }
    }

    /**
     * Log a warning if new license checker is enabled in compatibility mode or
     * while the live reload is off.
     */
    private void checkNewLicenseChecker(boolean logWarning) {
        boolean enableNewLicenseChecker = !getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_ENABLE_OLD_LICENSE_CHECKER,
                false);
        if (logWarning && !isProductionMode() && !isDevModeLiveReloadEnabled()
                && enableNewLicenseChecker) {
            warnings.add(WARNING_LIVERELOAD_DISABLED_AND_NEW_LICENSE_CHECKER);
        }
    }

    /**
     * Checks if request timing data should be provided to the client.
     */
    private void checkRequestTiming() {
        requestTiming = getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_REQUEST_TIMING,
                !productionMode);
    }

    /**
     * Log a warning if cross-site request forgery protection is disabled.
     */
    private void checkXsrfProtection(boolean logWarning) {
        xsrfProtectionEnabled = !getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                false);
        if (!xsrfProtectionEnabled && logWarning) {
            warnings.add(WARNING_XSRF_PROTECTION_DISABLED);
        }
    }

    private void checkHeartbeatInterval() {
        try {
            heartbeatInterval = getApplicationOrSystemProperty(
                    InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                    DEFAULT_HEARTBEAT_INTERVAL, Integer::parseInt);
        } catch (NumberFormatException e) {
            warnings.add(WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC);
            heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        }
    }

    private void checkMaxMessageSuspendTimeout() {
        try {
            maxMessageSuspendTimeout = getApplicationOrSystemProperty(
                    InitParameters.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT,
                    DEFAULT_MAX_MESSAGE_SUSPEND_TIMEOUT, Integer::parseInt);
        } catch (NumberFormatException e) {
            String warning = "WARNING: maxMessageSuspendInterval has been set to an illegal value."
                    + "The default of " + DEFAULT_MAX_MESSAGE_SUSPEND_TIMEOUT
                    + " ms will be used.";
            warnings.add(warning);
            maxMessageSuspendTimeout = DEFAULT_MAX_MESSAGE_SUSPEND_TIMEOUT;
        }
    }

    private void checkWebComponentDisconnectTimeout() {
        try {
            webComponentDisconnect = getApplicationOrSystemProperty(
                    InitParameters.SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT,
                    DEFAULT_WEB_COMPONENT_DISCONNECT, Integer::parseInt);

        } catch (NumberFormatException e) {
            warnings.add(WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC);
            webComponentDisconnect = DEFAULT_WEB_COMPONENT_DISCONNECT;
        }
    }

    private void checkCloseIdleSessions() {
        closeIdleSessions = getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS,
                DEFAULT_CLOSE_IDLE_SESSIONS);
    }

    private void checkPushMode() {
        try {
            pushMode = getApplicationOrSystemProperty(
                    InitParameters.SERVLET_PARAMETER_PUSH_MODE,
                    PushMode.DISABLED, stringMode -> Enum
                            .valueOf(PushMode.class, stringMode.toUpperCase()));
        } catch (IllegalArgumentException e) {
            warnings.add(WARNING_PUSH_MODE_NOT_RECOGNIZED);
            pushMode = PushMode.DISABLED;
        }
    }

    private void checkPushURL() {
        pushURL = getStringProperty(InitParameters.SERVLET_PARAMETER_PUSH_URL,
                "");
    }

    private void checkSyncIdCheck() {
        syncIdCheck = getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_SYNC_ID_CHECK,
                DEFAULT_SYNC_ID_CHECK);
    }

    private void checkSendUrlsAsParameters() {
        sendUrlsAsParameters = getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                DEFAULT_SEND_URLS_AS_PARAMETERS);
    }
}
