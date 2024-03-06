/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Test for {@link AbstractDeploymentConfiguration}
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AbstractDeploymentConfigurationTest {

    @Test
    public void getUIClass_returnsUIParameterPropertyValue() {
        String ui = UUID.randomUUID().toString();
        DeploymentConfiguration config = getConfig(InitParameters.UI_PARAMETER,
                ui);
        Assert.assertEquals("Unexpected UI class configuration option value",
                ui, config.getUIClassName());
    }

    @Test
    public void getClassLoader_returnsClassloaderPropertyValue() {
        String classLoader = UUID.randomUUID().toString();
        DeploymentConfiguration config = getConfig("ClassLoader", classLoader);
        Assert.assertEquals("Unexpected classLoader configuration option value",
                classLoader, config.getClassLoaderName());
    }

    private DeploymentConfiguration getConfig(String property, String value) {
        Properties props = new Properties();
        if (property != null) {
            props.put(property, value);
        }
        return new DeploymentConfigImpl(props);
    }

    private static class DeploymentConfigImpl
            extends AbstractDeploymentConfiguration {

        private Properties properties;

        DeploymentConfigImpl(Properties props) {
            super(Collections.emptyMap());
            properties = props;
        }

        @Override
        public boolean isProductionMode() {
            return false;
        }

        @Override
        public boolean isRequestTiming() {
            return !isProductionMode();
        }

        @Override
        public boolean isXsrfProtectionEnabled() {
            return false;
        }

        @Override
        public boolean isSyncIdCheckEnabled() {
            return false;
        }

        @Override
        public int getHeartbeatInterval() {
            return 0;
        }

        @Override
        public int getMaxMessageSuspendTimeout() {
            return 0;
        }

        @Override
        public int getWebComponentDisconnect() {
            return 0;
        }

        @Override
        public boolean isCloseIdleSessions() {
            return false;
        }

        @Override
        public PushMode getPushMode() {
            return null;
        }

        @Override
        public Properties getInitParameters() {
            return null;
        }

        @Override
        public <T> T getApplicationOrSystemProperty(String propertyName,
                T defaultValue, Function<String, T> converter) {
            return Optional.ofNullable(properties.getProperty(propertyName))
                    .map(converter).orElse(defaultValue);
        }

        @Override
        public boolean isDevModeLiveReloadEnabled() {
            return false;
        }

        @Override
        public boolean isDevToolsEnabled() {
            return false;
        }

        @Override
        public boolean isSendUrlsAsParameters() {
            return DefaultDeploymentConfiguration.DEFAULT_SEND_URLS_AS_PARAMETERS;
        }

    }
}
