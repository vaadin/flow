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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.TARGET;

/**
 * The default implementation of {@link DeploymentConfiguration} based on a base
 * class for resolving system properties and a set of init parameters.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultDeploymentConfiguration
        extends PropertyDeploymentConfiguration {

    private static final String SEPARATOR = "\n=======================================================================";
    private static final String HEADER = "\n=================== Vaadin DeploymentConfiguration ====================\n";

    public static final String NOT_PRODUCTION_MODE_INFO = " Vaadin is running in DEBUG MODE.\n"
            + " When deploying application for production, remember to disable debug features. See more from https://vaadin.com/docs/";

    public static final String NOT_PRODUCTION_MODE_WARNING = " WARNING: Vaadin is running in DEBUG MODE with debug features enabled, but with a prebuild frontend bundle (production ready).\n"
            + " When deploying application for production, disable debug features by enabling production mode!\n"
            + " See more from https://vaadin.com/docs/v14/flow/production/tutorial-production-mode-basic.html";

    public static final String WARNING_V14_BOOTSTRAP = " Using deprecated Vaadin 14 bootstrap mode.\n"
            + " Client-side views written in TypeScript are not supported. Vaadin 15+ enables client-side and server-side views.\n"
            + " See https://vaadin.com/docs/v15/flow/typescript/starting-the-app.html for more information.";

    // not a warning anymore, but keeping variable name to avoid breaking anything
    public static final String WARNING_V15_BOOTSTRAP = "%n Using Vaadin 15+ bootstrap mode.%n %s%n %s";

    private static final String DEPLOYMENT_WARNINGS = " Following issues were discovered with deployment configuration:";

    public static final String WARNING_XSRF_PROTECTION_DISABLED = " WARNING: Cross-site request forgery protection is disabled!";

    public static final String WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC = " WARNING: heartbeatInterval has been set to a non integer value."
            + "\n The default of 5min will be used.";

    public static final String WARNING_PUSH_MODE_NOT_RECOGNIZED = " WARNING: pushMode has been set to an unrecognized value.\n"
            + " The permitted values are \"disabled\", \"manual\",\n"
            + " and \"automatic\". The default of \"disabled\" will be used.";

    private static final String INDEX_NOT_FOUND = " '%s' is not found from '%s'.%n"
            + " Generating a default one in '%s%s'. "
            + " Move it to the '%s' folder if you want to customize it.";

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
    private boolean useDeprecatedV14Bootstrapping;
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
        checkV14Bootsrapping(log);
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
            warnings.add(0, HEADER);
            warnings.add(1, DEPLOYMENT_WARNINGS);
            warnings.add("\n");
            // merging info messages to warnings for now
            warnings.addAll(info);
            warnings.add(SEPARATOR);
            if (logger.isWarnEnabled()) {
                logger.warn(String.join("\n", warnings));
            }
        } else if (!info.isEmpty()) {
            info.add(0, HEADER);
            info.add(SEPARATOR);
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
     * {@inheritDoc} The default is true.
     */
    @Override
    public boolean useV14Bootstrap() {
        return useDeprecatedV14Bootstrapping;
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
                Constants.SERVLET_PARAMETER_PRODUCTION_MODE, false);
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
     * Log a message about the bootstrapping being used.
     */
    private void checkV14Bootsrapping(boolean log) {
        useDeprecatedV14Bootstrapping = getBooleanProperty(
                Constants.SERVLET_PARAMETER_USE_V14_BOOTSTRAP, false);
        if (log) {
            if (useDeprecatedV14Bootstrapping) {
                warnings.add(WARNING_V14_BOOTSTRAP);
            } else if (!productionMode) {
                String frontendDir = getStringProperty(PARAM_FRONTEND_DIR,
                        System.getProperty(PARAM_FRONTEND_DIR,
                                DEFAULT_FRONTEND_DIR));
                String indexHTMLMessage = getIndexHTMLMessage(frontendDir);
                String entryPointMessage = getEntryPointMessage(frontendDir);
                info.add(String.format(WARNING_V15_BOOTSTRAP, indexHTMLMessage,
                        entryPointMessage));
            }
        }
    }

    private String getEntryPointMessage(String frontendDir) {
        File indexEntry = new File(frontendDir, INDEX_JS);
        File indexEntryTs = new File(frontendDir, INDEX_TS);
        String entryPointMessage;
        if (!indexEntry.exists() && !indexEntryTs.exists()) {
            entryPointMessage = String.format(INDEX_NOT_FOUND,
                    indexEntryTs.getName(), indexEntryTs.getPath(), TARGET,
                    indexEntryTs.getName(),
                    indexEntryTs.getParentFile().getPath());
        } else {
            String fileName = indexEntry.exists() ? "index.js" : "index.ts";
            String filePath = indexEntry.exists() ? indexEntry.getPath()
                    : indexEntryTs.getPath();
            entryPointMessage = String.format("Using '%s' from '%s'", fileName,
                    filePath);
        }
        return entryPointMessage;
    }

    private String getIndexHTMLMessage(String frontendDir) {
        File indexHTML = new File(frontendDir, INDEX_HTML);

        String indexHTMLMessage;
        if (!indexHTML.exists()) {
            indexHTMLMessage = String.format(INDEX_NOT_FOUND,
                    indexHTML.getName(), indexHTML.getPath(), TARGET,
                    indexHTML.getName(), indexHTML.getParentFile().getPath());
        } else {
            indexHTMLMessage = String.format("Using 'index.html' from '%s'",
                    indexHTML.getPath());
        }
        return indexHTMLMessage;
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
            warnings.add(WARNING_XSRF_PROTECTION_DISABLED);
        }
    }

    private void checkHeartbeatInterval() {
        try {
            heartbeatInterval = getApplicationOrSystemProperty(
                    Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                    DEFAULT_HEARTBEAT_INTERVAL, Integer::parseInt);
        } catch (NumberFormatException e) {
            warnings.add(WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC);
            heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        }
    }

    private void checkMaxMessageSuspendTimeout() {
        try {
            maxMessageSuspendTimeout = getApplicationOrSystemProperty(
                    Constants.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT,
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
                    Constants.SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT,
                    DEFAULT_WEB_COMPONENT_DISCONNECT, Integer::parseInt);

        } catch (NumberFormatException e) {
            warnings.add(WARNING_HEARTBEAT_INTERVAL_NOT_NUMERIC);
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
            warnings.add(WARNING_PUSH_MODE_NOT_RECOGNIZED);
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
}
