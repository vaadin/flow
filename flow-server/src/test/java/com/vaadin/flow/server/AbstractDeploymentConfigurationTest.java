/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test for {@link AbstractDeploymentConfiguration}
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
class AbstractDeploymentConfigurationTest {

    @Test
    void getUIClass_returnsUIParameterPropertyValue() {
        String ui = UUID.randomUUID().toString();
        DeploymentConfiguration config = getConfig(InitParameters.UI_PARAMETER,
                ui);
        assertEquals(ui, config.getUIClassName(),
                "Unexpected UI class configuration option value");
    }

    @Test
    void getClassLoader_returnsClassloaderPropertyValue() {
        String classLoader = UUID.randomUUID().toString();
        DeploymentConfiguration config = getConfig("ClassLoader", classLoader);
        assertEquals(classLoader, config.getClassLoaderName(),
                "Unexpected classLoader configuration option value");
    }

    @Test
    void getUrlSafeSchemes_propertyNotSet_returnsDefault() {
        DeploymentConfiguration config = getConfig(null, null);
        assertEquals(Constants.DEFAULT_URL_SAFE_SCHEMES,
                config.getUrlSafeSchemes());
    }

    @Test
    void getUrlSafeSchemes_commaSeparated_isTrimmedAndLowerCased() {
        DeploymentConfiguration config = getConfig(
                InitParameters.URL_SAFE_SCHEMES, " HTTPS , MyApp ");
        assertEquals(Set.of("https", "myapp"), config.getUrlSafeSchemes());
    }

    @Test
    void getUrlSafeSchemes_memoizedAcrossCalls() {
        DeploymentConfiguration config = getConfig(
                InitParameters.URL_SAFE_SCHEMES, "custom");
        Set<String> first = config.getUrlSafeSchemes();
        Set<String> second = config.getUrlSafeSchemes();
        assertEquals(Set.of("custom"), first);
        assertSame(first, second,
                "Subsequent calls should return the cached instance");
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

        @Override
        public SessionLockCheckStrategy getSessionLockCheckStrategy() {
            return SessionLockCheckStrategy.ASSERT;
        }
    }
}
