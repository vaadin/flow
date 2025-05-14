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

package com.vaadin.flow.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
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

    public static final String NOT_PRODUCTION_MODE_INFO = "\nVaadin is running in DEVELOPMENT mode - do not use for production deployments.";

    private static final String DEPLOYMENT_WARNINGS = "Following issues were discovered with deployment configuration:";

    public static final String WARNING_XSRF_PROTECTION_DISABLED = "WARNING: Cross-site request forgery protection is disabled!";

    public static final String WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC = "WARNING: heartbeatInterval has been set to a non integer value."
            + "\n The default of 5min will be used.";

    public static final String WARNING_PUSH_MODE_NOT_RECOGNIZED = "WARNING: pushMode has been set to an unrecognized value.\n"
            + "The permitted values are \"disabled\", \"manual\",\n"
            + "and \"automatic\". The default of \"disabled\" will be used.";

    public static final String WARNING_SESSION_LOCK_CHECK_STRATEGY_NOT_RECOGNIZED = "WARNING: "
            + InitParameters.SERVLET_PARAMETER_SESSION_LOCK_CHECK_STRATEGY
            + " has been set to an unrecognized value.\n"
            + "The permitted values are "
            + Arrays.stream(SessionLockCheckStrategy.values())
                    .map(it -> "\"" + it.name().toLowerCase() + "\"")
                    .collect(Collectors.joining(", "))
            + ".\nThe default of \""
            + SessionLockCheckStrategy.ASSERT.name().toLowerCase()
            + "\" will be used.";

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
    private boolean xsrfProtectionEnabled;
    private int heartbeatInterval;
    private int maxMessageSuspendTimeout;
    private int webComponentDisconnect;
    private boolean closeIdleSessions;
    private PushMode pushMode;
    private String pushServletMapping;
    private boolean syncIdCheck;
    private boolean sendUrlsAsParameters;
    private boolean requestTiming;
    private boolean frontendHotdeploy;
    private SessionLockCheckStrategy sessionLockCheckStrategy;

    private static AtomicBoolean logging = new AtomicBoolean(true);
    private List<String> warnings = new ArrayList<>();
    private List<String> info = new ArrayList<>();

    /**
     * Create a new deployment configuration instance.
     *
     * @param parentConfig
     *            a parent application configuration
     * @param systemPropertyBaseClass
     *            the class that should be used as a basis when reading system
     *            properties
     * @param initParameters
     *            the init parameters that should make up the foundation for
     *            this configuration
     */
    public DefaultDeploymentConfiguration(ApplicationConfiguration parentConfig,
            Class<?> systemPropertyBaseClass, Properties initParameters) {
        super(parentConfig, systemPropertyBaseClass, initParameters);

        boolean log = logging.getAndSet(false);

        checkProductionMode(log);
        checkFeatureFlags();
        checkRequestTiming();
        checkXsrfProtection(log);
        checkHeartbeatInterval();
        checkMaxMessageSuspendTimeout();
        checkWebComponentDisconnectTimeout();
        checkCloseIdleSessions();
        checkPushMode();
        checkPushServletMapping();
        checkSyncIdCheck();
        checkSendUrlsAsParameters();
        checkFrontendHotdeploy();
        checkSessionLockCheckStrategy();

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
        } else if (!info.isEmpty() && logger.isInfoEnabled()) {
            logger.info(String.join("\n", info));
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
     * The default mode is <code>""</code> which uses the service mapping.
     */
    @Override
    public String getPushServletMapping() {
        return pushServletMapping;
    }

    @Override
    public boolean frontendHotdeploy() {
        return frontendHotdeploy;
    }

    @Override
    public SessionLockCheckStrategy getSessionLockCheckStrategy() {
        return sessionLockCheckStrategy;
    }

    /**
     * Log a warning if Vaadin is not running in production mode.
     */
    private void checkProductionMode(boolean log) {
        if (isOwnProperty(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE)) {
            productionMode = getBooleanProperty(
                    InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, false);
        } else {
            productionMode = getParentConfiguration().isProductionMode();
        }
        if (log) {
            if (productionMode) {
                info.add("Vaadin is running in production mode.");
            } else {
                info.add(NOT_PRODUCTION_MODE_INFO);
            }
        }
    }

    /**
     * Log information about enabled feature flags.
     */
    private void checkFeatureFlags() {
        List<Feature> enabledFeatures = FeatureFlags
                .get(getParentConfiguration().getContext()).getFeatures()
                .stream().filter(f -> f.isEnabled())
                .collect(Collectors.toList());
        if (!enabledFeatures.isEmpty()) {
            info.add("\nThe following feature previews are enabled:");
            enabledFeatures.forEach(feature -> {
                info.add("- " + feature.getTitle());
            });

            info.add("\n");
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
    private void checkXsrfProtection(boolean loggWarning) {
        if (isOwnProperty(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION)) {
            xsrfProtectionEnabled = !getBooleanProperty(
                    InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                    false);
        } else {
            xsrfProtectionEnabled = getParentConfiguration()
                    .isXsrfProtectionEnabled();
        }
        if (!xsrfProtectionEnabled && loggWarning) {
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

    private void checkSessionLockCheckStrategy() {
        try {
            sessionLockCheckStrategy = getApplicationOrSystemProperty(
                    InitParameters.SERVLET_PARAMETER_SESSION_LOCK_CHECK_STRATEGY,
                    SessionLockCheckStrategy.ASSERT,
                    stringStrategy -> Enum.valueOf(
                            SessionLockCheckStrategy.class,
                            stringStrategy.toUpperCase()));
        } catch (IllegalArgumentException e) {
            warnings.add(WARNING_SESSION_LOCK_CHECK_STRATEGY_NOT_RECOGNIZED);
            sessionLockCheckStrategy = SessionLockCheckStrategy.ASSERT;
        }
    }

    private void checkPushServletMapping() {
        pushServletMapping = getStringProperty(
                InitParameters.SERVLET_PARAMETER_PUSH_SERVLET_MAPPING, "");
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

    private void checkFrontendHotdeploy() {
        if (isProductionMode()) {
            frontendHotdeploy = false;
        } else {
            frontendHotdeploy = getBooleanProperty(
                    InitParameters.FRONTEND_HOTDEPLOY,
                    automaticHotdeployDefault());
        }
    }

    private boolean automaticHotdeployDefault() {
        return FrontendUtils.isHillaUsed(getFrontendFolder());
    }

}
