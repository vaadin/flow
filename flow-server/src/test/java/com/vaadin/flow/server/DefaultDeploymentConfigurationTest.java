/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DefaultDeploymentConfiguration}
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultDeploymentConfigurationTest {

    VaadinContext context;

    @Before
    public void setup() {
        context = new MockVaadinContext();
    }

    @Test
    public void testGetSystemPropertyForDefaultPackage()
            throws ClassNotFoundException {
        Class<?> clazz = Class.forName("ClassInDefaultPackage");
        String value = "value";
        String prop = "prop";
        System.setProperty(prop, value);
        Properties initParameters = new Properties();
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                appConfig, clazz, initParameters);
        assertEquals(value, config.getSystemProperty(prop));
    }

    @Test
    public void testGetSystemProperty() {
        String value = "value";
        String prop = "prop";
        System.setProperty(
                DefaultDeploymentConfigurationTest.class.getPackage().getName()
                        + '.' + prop,
                value);
        Properties initParameters = new Properties();
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                appConfig, DefaultDeploymentConfigurationTest.class,
                initParameters);
        assertEquals(value, config.getSystemProperty(prop));
    }

    @Test
    public void booleanValueReadIgnoreTheCase_true() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "tRUe");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertTrue(
                "Boolean value equal to 'true' ignoring case should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void booleanValueReadIgnoreTheCase_false() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "FaLsE");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertFalse(
                "Boolean value equal to 'false' ignoring case should be interpreted as 'false'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void booleanValueRead_emptyIsTrue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS, "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertTrue("Empty boolean value should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void defaultPushServletMapping() {
        Properties initParameters = new Properties();
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertThat(config.getPushServletMapping(), is(""));
    }

    @Test
    public void pushUrl() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_PUSH_SERVLET_MAPPING,
                "/foo/*");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertThat(config.getPushServletMapping(), is("/foo/*"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void booleanValueRead_exceptionOnNonBooleanValue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "incorrectValue");

        createDeploymentConfig(initParameters);
    }

    @Test
    public void maxMessageSuspendTimeout_validValue_accepted() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT,
                "2700");
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertEquals(2700, config.getMaxMessageSuspendTimeout());
    }

    @Test
    public void maxMessageSuspendTimeout_invalidValue_defaultValue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT,
                "kk");
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertEquals(5000, config.getMaxMessageSuspendTimeout());
    }

    @Test
    public void isProductionMode_productionModeIsSetViaParentOnly_productionModeIsTakenFromParent() {
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);

        // Note: application configuration doesn't contain production mode
        // parameter !
        Assert.assertNull(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, null));

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, new Properties());
        Assert.assertTrue(config.isProductionMode());
        Assert.assertTrue(config.getProperties().isEmpty());
    }

    @Test
    public void isProductionMode_productionModeIsSetViaPropertiesAndViaParent_productionModeIsTakenFromProperties() {
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);

        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.TRUE.toString());
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, initParameters);
        // the deployment configuration parameter takes precedence over parent
        // config
        Assert.assertTrue(config.isProductionMode());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsSetViaParentOnly_valueIsTakenFromParent() {
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(true);

        // Note: application configuration doesn't contain production mode
        // parameter !
        Assert.assertNull(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                null));

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, new Properties());
        Assert.assertTrue(config.isXsrfProtectionEnabled());
        Assert.assertTrue(config.getProperties().isEmpty());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsSetViaParentOnlyAndViaParent_valueIsTakenFromParent() {
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(false);

        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                Boolean.FALSE.toString());

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, initParameters);
        // the deployment configuration parameter takes precedence over parent
        // config
        Assert.assertTrue(config.isXsrfProtectionEnabled());
    }

    @Test
    public void frontendHotdeployParameter_expressBuildFeatureFlagIsON_resetsFrontendHotdeployToFalse() {
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                new Properties());
        Assert.assertFalse("Expected dev server to be disabled by default",
                config.frontendHotdeploy());

        Properties init = new Properties();
        init.put(InitParameters.FRONTEND_HOTDEPLOY, "true");
        config = createDeploymentConfig(init);
        Assert.assertTrue("Expected dev server to be enabled when set true",
                config.frontendHotdeploy());
    }

    @Test
    public void productionModeTrue_frontendHotdeployTrue_frontendHotdeployReturnsFalse() {
        Properties init = new Properties();
        init.put(InitParameters.FRONTEND_HOTDEPLOY, "true");
        init.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, "true");

        DefaultDeploymentConfiguration config = createDeploymentConfig(init);

        Assert.assertTrue("ProductionMode should be enabled",
                config.isProductionMode());
        Assert.assertFalse(
                "Frontend hotdeploy should return false in production mode",
                config.frontendHotdeploy());
    }

    private DefaultDeploymentConfiguration createDeploymentConfig(
            Properties initParameters) {
        ApplicationConfiguration appConfig = setupAppConfig();
        return createDeploymentConfig(appConfig, initParameters);
    }

    private DefaultDeploymentConfiguration createDeploymentConfig(
            ApplicationConfiguration appConfig, Properties initParameters) {
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(appConfig.getBuildFolder()).thenReturn(".");
        Mockito.when(appConfig.getContext()).thenReturn(context);
        return new DefaultDeploymentConfiguration(appConfig,
                DefaultDeploymentConfigurationTest.class, initParameters);
    }

    private ApplicationConfiguration setupAppConfig() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getContext()).thenReturn(context);
        return appConfig;
    }
}
