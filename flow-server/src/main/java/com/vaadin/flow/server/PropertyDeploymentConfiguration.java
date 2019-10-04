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

import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_BOWER_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REQUEST_TIMING;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_SYNC_ID_CHECK;
import static com.vaadin.flow.server.Constants.VAADIN_PREFIX;

/**
 * The property handling implementation of {@link DeploymentConfiguration} based
 * on a base class for resolving system properties and a set of init parameters.
 *
 * @since 1.2
 */
public class PropertyDeploymentConfiguration
        extends AbstractDeploymentConfiguration {

    private final Properties initParameters;
    private final Class<?> systemPropertyBaseClass;

    /**
     * Create a new property deployment configuration instance.
     *
     * @param systemPropertyBaseClass
     *            the class that should be used as a basis when reading system
     *            properties
     * @param initParameters
     *            the init parameters that should make up the foundation for
     *            this configuration
     */
    public PropertyDeploymentConfiguration(Class<?> systemPropertyBaseClass,
            Properties initParameters) {
        this.initParameters = initParameters;
        this.systemPropertyBaseClass = systemPropertyBaseClass;
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

        // version prefixed with just "vaadin."
        val = System.getProperty(VAADIN_PREFIX + parameterName);

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

    @Override
    public boolean isProductionMode() {
        return getBooleanProperty(SERVLET_PARAMETER_PRODUCTION_MODE, false);
    }

    @Override
    public boolean isBowerMode() {
        return getBooleanProperty(SERVLET_PARAMETER_BOWER_MODE, false);
    }

    @Override
    public boolean isCompatibilityMode() {
        String bower = getStringProperty(SERVLET_PARAMETER_BOWER_MODE, null);
        if (bower == null) {
            return getBooleanProperty(
                    Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE, false);
        }
        return isBowerMode();
    }

    @Override
    public boolean isRequestTiming() {
        return getBooleanProperty(SERVLET_PARAMETER_REQUEST_TIMING,
                !isProductionMode());
    }

    @Override
    public boolean isXsrfProtectionEnabled() {
        return !getBooleanProperty(SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                false);
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
    public String getPushURL() {
        return "";
    }

    @Override
    public Properties getInitParameters() {
        return initParameters;
    }

}
