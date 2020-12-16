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

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class PropertyDeploymentConfigurationTest {

    @Test
    public void isProductionMode_modeIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.isProductionMode());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void isProductionMode_modeIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        Assert.assertTrue(config.isProductionMode());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void enableDevServer_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.enableDevServer()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.enableDevServer());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void enableDevServer_valueIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.enableDevServer()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        Assert.assertTrue(config.enableDevServer());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void useV14Bootstrap_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.useV14Bootstrap()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.useV14Bootstrap());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void useV14Bootstrap_valueIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.useV14Bootstrap()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        Assert.assertTrue(config.useV14Bootstrap());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void isPnpmEnabled_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.isPnpmEnabled());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void isPnpmEnabled_valueIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        Assert.assertTrue(config.isPnpmEnabled());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void getApplicationProperty_propertyIsDefinedInParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(appConfig.getStringProperty("foo", null))
                .thenReturn("bar");

        PropertyDeploymentConfiguration configuration = createConfiguration(
                appConfig, new Properties());

        Assert.assertEquals("bar", configuration.getApplicationProperty("foo"));
        // there is no any property
        Assert.assertTrue(configuration.getInitParameters().isEmpty());
    }

    @Test
    public void getApplicationProperty_propertyIsDefinedInPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(appConfig.getStringProperty("foo", null))
                .thenReturn("bar");

        Properties properties = new Properties();
        properties.put("foo", "baz");

        PropertyDeploymentConfiguration configuration = createConfiguration(
                appConfig, properties);

        Assert.assertEquals("baz", configuration.getApplicationProperty("foo"));
        Assert.assertEquals(properties, configuration.getInitParameters());
    }

    private PropertyDeploymentConfiguration createConfiguration(
            ApplicationConfiguration appConfig, Properties properties) {
        return new PropertyDeploymentConfiguration(appConfig, Object.class,
                properties);
    }
}
