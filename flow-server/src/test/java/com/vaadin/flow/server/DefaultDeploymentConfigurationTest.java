/*
 * Copyright 2000-2021 Vaadin Ltd.
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

    @Test
    public void testGetSystemPropertyForDefaultPackage()
            throws ClassNotFoundException {
        Class<?> clazz = Class.forName("ClassInDefaultPackage");
        String value = "value";
        String prop = "prop";
        System.setProperty(prop, value);
        Properties initParameters = new Properties();
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                appConfig, clazz, initParameters);
        assertEquals(value, config.getSystemProperty(prop));
    }

    @Test
    public void testGetSystemProperty() throws ClassNotFoundException {
        String value = "value";
        String prop = "prop";
        System.setProperty(
                DefaultDeploymentConfigurationTest.class.getPackage().getName()
                        + '.' + prop,
                value);
        Properties initParameters = new Properties();
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
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

    @Test(expected = IllegalArgumentException.class)
    public void booleanValueRead_exceptionOnNonBooleanValue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "incorrectValue");

        createDeploymentConfig(initParameters);
    }

    @Test
    public void defaultPushUrl() {
        Properties initParameters = new Properties();
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertThat(config.getPushURL(), is(""));
    }

    @Test
    public void pushUrl() {
        Properties initParameters = new Properties();
        initParameters.setProperty(InitParameters.SERVLET_PARAMETER_PUSH_URL,
                "foo");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertThat(config.getPushURL(), is("foo"));
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
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
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
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
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
    public void useV14Bootstrap_v14ModeIsSetViaParentOnly_v14ModeIsTakenFromParent() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.useV14Bootstrap()).thenReturn(true);

        // Note: application configuration doesn't contain production mode
        // parameter !
        Assert.assertNull(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP, null));

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, new Properties());
        Assert.assertTrue(config.useV14Bootstrap());
        Assert.assertTrue(config.getProperties().isEmpty());
    }

    @Test
    public void useV14Bootstrap_v14ModeIsSetViaParentOnlyAndViaParent_v14ModeIsTakenFromParent() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.useV14Bootstrap()).thenReturn(true);

        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP,
                Boolean.TRUE.toString());

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, initParameters);
        // the deployment configuration parameter takes precedence over parent
        // config
        Assert.assertTrue(config.useV14Bootstrap());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsSetViaParentOnly_valueIsTakenFromParent() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
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
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
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

    private DefaultDeploymentConfiguration createDeploymentConfig(
            Properties initParameters) {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        return createDeploymentConfig(appConfig, initParameters);
    }

    private DefaultDeploymentConfiguration createDeploymentConfig(
            ApplicationConfiguration appConfig, Properties initParameters) {
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        return new DefaultDeploymentConfiguration(appConfig,
                DefaultDeploymentConfigurationTest.class, initParameters);
    }
}
