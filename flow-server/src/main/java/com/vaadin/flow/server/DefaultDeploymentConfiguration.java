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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String SEPARATOR = "\n===========================================================";

    public static final String NOT_PRODUCTION_MODE_INFO = SEPARATOR
            + "\nVaadin is running in DEBUG MODE.\nAdd productionMode=true to web.xml "
            + "to disable debug features." + SEPARATOR;

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
     * Default value for {@link #getWebComponentDisconnect()} = {@value}.
     */
    public static final int DEFAULT_WEB_COMPONENT_DISCONNECT = 300;

    /**
     * Default value for {@link #isCloseIdleSessions()} = {@value} .
     */
    public static final boolean DEFAULT_CLOSE_IDLE_SESSIONS = false;

    /**
     * Default value for {@link #isSyncIdCheckEnabled()} = {@value} .
     *
     */
    public static final boolean DEFAULT_SYNC_ID_CHECK = true;

    public static final boolean DEFAULT_SEND_URLS_AS_PARAMETERS = true;

    private boolean productionMode;
    private boolean xsrfProtectionEnabled;
    private int heartbeatInterval;
    private int webComponentDisconnect;
    private boolean closeIdleSessions;
    private PushMode pushMode;
    private String pushURL;
    private boolean syncIdCheck;
    private boolean sendUrlsAsParameters;
    private boolean requestTiming;
    private static AtomicBoolean loggWarning = new AtomicBoolean(true);

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

        boolean log = loggWarning.getAndSet(false);

        checkProductionMode(log);
        checkRequestTiming();
        checkXsrfProtection(log);
        checkHeartbeatInterval();
        checkWebComponentDisconnectTimeout();
        checkCloseIdleSessions();
        checkPushMode();
        checkPushURL();
        checkSyncIdCheck();
        checkSendUrlsAsParameters();
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
     *
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
    private void checkProductionMode(boolean loggWarning) {
        productionMode = getBooleanProperty(
                Constants.SERVLET_PARAMETER_PRODUCTION_MODE, false);
        if (!productionMode && loggWarning) {
            getLogger().warn(NOT_PRODUCTION_MODE_INFO);
        }
    }

    /**
     * Checks if request timing data should be provided to the client.
     */
    private void checkRequestTiming() {
        requestTiming = getBooleanProperty(
                Constants.SERVLET_PARAMETER_REQUEST_TIMING, !productionMode);
    }

    /**
     * Log a warning if cross-site request forgery protection is disabled.
     */
    private void checkXsrfProtection(boolean loggWarning) {
        xsrfProtectionEnabled = !getBooleanProperty(
                Constants.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION, false);
        if (!xsrfProtectionEnabled && loggWarning) {
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

    private void checkWebComponentDisconnectTimeout() {
        try {
            webComponentDisconnect = getApplicationOrSystemProperty(
                    Constants.SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT,
                    DEFAULT_WEB_COMPONENT_DISCONNECT, Integer::parseInt);

        } catch (NumberFormatException e) {
            getLogger().warn(WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC);
            webComponentDisconnect = DEFAULT_WEB_COMPONENT_DISCONNECT;
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

    private void checkPushURL() {
        pushURL = getStringProperty(Constants.SERVLET_PARAMETER_PUSH_URL, "");
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

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getName());
    }

}
