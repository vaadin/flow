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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

import static com.vaadin.flow.server.InitParameters.BUILD_FOLDER;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_ENABLE_DEV_TOOLS;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_REQUEST_TIMING;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_SYNC_ID_CHECK;

/**
 * The property handling implementation of {@link DeploymentConfiguration} based
 * on a base class for resolving system properties and a set of init parameters.
 *
 * @since 1.2
 */
public class PropertyDeploymentConfiguration
        extends AbstractDeploymentConfiguration {

    private final Class<?> systemPropertyBaseClass;

    /**
     * Contains properties from both: parent config and provided properties.
     */
    private final Properties allProperties;

    private final ApplicationConfiguration parentConfig;

    /**
     * Create a new property deployment configuration instance.
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
    public PropertyDeploymentConfiguration(
            ApplicationConfiguration parentConfig,
            Class<?> systemPropertyBaseClass, Properties initParameters) {
        super(filterStringProperties(initParameters));
        this.parentConfig = parentConfig;
        allProperties = mergeProperties(parentConfig, initParameters);
        this.systemPropertyBaseClass = systemPropertyBaseClass;
    }

    /**
     * Gets an system property value.
     *
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    @Override
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
        } else if (!pkgName.isEmpty()) {
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

        return super.getSystemProperty(parameterName);
    }

    /**
     * Gets an application property value.
     *
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    @Override
    public String getApplicationProperty(String parameterName) {
        String val = getApplicationProperty(getProperties()::get,
                parameterName);
        if (val == null) {
            val = getApplicationProperty(
                    prop -> parentConfig.getStringProperty(prop, null),
                    parameterName);
        }
        return val;
    }

    @Override
    public boolean isProductionMode() {
        if (isOwnProperty(SERVLET_PARAMETER_PRODUCTION_MODE)) {
            return getBooleanProperty(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        }
        return parentConfig.isProductionMode();
    }

    @Override
    public File getFrontendFolder() {
        return parentConfig.getFrontendFolder();
    }

    @Override
    public boolean isPnpmEnabled() {
        if (isOwnProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)) {
            return super.isPnpmEnabled();
        }
        return parentConfig.isPnpmEnabled();
    }

    @Override
    public boolean isBunEnabled() {
        if (isOwnProperty(InitParameters.SERVLET_PARAMETER_ENABLE_BUN)) {
            return super.isBunEnabled();
        }
        return parentConfig.isBunEnabled();
    }

    @Override
    public boolean isUsageStatisticsEnabled() {
        return !isProductionMode() && getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_STATISTICS, true);
    }

    @Override
    public boolean isGlobalPnpm() {
        if (isOwnProperty(InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM)) {
            return super.isGlobalPnpm();
        }
        return parentConfig.isGlobalPnpm();
    }

    @Override
    public boolean reuseDevServer() {
        if (isOwnProperty(InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER)) {
            return super.reuseDevServer();
        }
        return parentConfig.reuseDevServer();
    }

    @Override
    public boolean isRequestTiming() {
        return getBooleanProperty(SERVLET_PARAMETER_REQUEST_TIMING,
                !isProductionMode());
    }

    @Override
    public boolean isXsrfProtectionEnabled() {
        if (isOwnProperty(SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION)) {
            return super.isXsrfProtectionEnabled();
        }
        return parentConfig.isXsrfProtectionEnabled();
    }

    @Override
    public String getBuildFolder() {
        if (isOwnProperty(BUILD_FOLDER)) {
            return super.getBuildFolder();
        }
        return parentConfig.getBuildFolder();
    }

    @Override
    public File getJavaResourceFolder() {
        return super.getJavaResourceFolder();
    }

    @Override
    public File getJavaSourceFolder() {
        return super.getJavaSourceFolder();
    }

    @Override
    public boolean isSyncIdCheckEnabled() {
        return getBooleanProperty(SERVLET_PARAMETER_SYNC_ID_CHECK, true);
    }

    @Override
    public int getHeartbeatInterval() {
        return DefaultDeploymentConfiguration.DEFAULT_HEARTBEAT_INTERVAL;
    }

    @Override
    public int getMaxMessageSuspendTimeout() {
        return DefaultDeploymentConfiguration.DEFAULT_MAX_MESSAGE_SUSPEND_TIMEOUT;
    }

    @Override
    public int getWebComponentDisconnect() {
        return DefaultDeploymentConfiguration.DEFAULT_WEB_COMPONENT_DISCONNECT;
    }

    @Override
    public boolean isSendUrlsAsParameters() {
        return getBooleanProperty(SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                true);
    }

    @Override
    public boolean isCloseIdleSessions() {
        return getBooleanProperty(SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, false);
    }

    @Override
    public PushMode getPushMode() {
        return PushMode.DISABLED;
    }

    @Override
    public Properties getInitParameters() {
        return allProperties;
    }

    @Override
    public boolean isDevModeLiveReloadEnabled() {
        return isDevToolsEnabled() && getBooleanProperty(
                SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD, true);
    }

    @Override
    public boolean isDevToolsEnabled() {
        return !isProductionMode() && getBooleanProperty(
                SERVLET_PARAMETER_DEVMODE_ENABLE_DEV_TOOLS, true);
    }

    @Override
    public SessionLockCheckStrategy getSessionLockCheckStrategy() {
        return SessionLockCheckStrategy.ASSERT;
    }

    /**
     * Checks whether the given {@code property} is the property explicitly set
     * in this deployment configuration (not in it's parent config).
     * <p>
     * The deployment configuration consists of properties defined in the
     * configuration itself and properties which are coming from the application
     * configuration. The properties which are defined in the deployment
     * configuration itself (own properties) should take precedence: their
     * values should override the parent config properties values.
     *
     * @param property
     *            a property name
     * @return whether the {@code property} is explicitly set in the
     *         configuration
     */
    protected boolean isOwnProperty(String property) {
        return getApplicationProperty(getProperties()::get, property) != null;
    }

    /**
     * Returns parent application configuration.
     *
     * @return the parent config
     */
    protected ApplicationConfiguration getParentConfiguration() {
        return parentConfig;
    }

    private Properties mergeProperties(ApplicationConfiguration config,
            Properties properties) {
        Properties result = new Properties();
        Enumeration<String> propertyNames = config.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String property = propertyNames.nextElement();
            result.put(property, config.getStringProperty(property, null));
        }
        result.putAll(properties);
        return result;
    }

    private static Map<String, String> filterStringProperties(
            Properties properties) {
        Map<String, String> result = new HashMap<>();
        for (Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            // Hashtable doesn't allow null for key and value
            if (key instanceof String && value instanceof String) {
                result.put(key.toString(), value.toString());
            }
        }
        return result;
    }

}
