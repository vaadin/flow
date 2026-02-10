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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyDeploymentConfigurationTest {
    @TempDir
    Path tempFolder;

    @Test
    public void isProductionMode_modeIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        assertTrue(config.isProductionMode());
        // there is no any property
        assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void isProductionMode_modeIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        assertTrue(config.isProductionMode());
        assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void reuseDevServer_valueIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.reuseDevServer()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        assertTrue(config.reuseDevServer());
        assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void reuseDevServer_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.reuseDevServer()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        assertTrue(config.reuseDevServer());
        // there is no any property
        assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void isPnpmEnabled_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        assertTrue(config.isPnpmEnabled());
        // there is no any property
        assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void isPnpmEnabled_valueIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        assertTrue(config.isPnpmEnabled());
        assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        assertTrue(config.isXsrfProtectionEnabled());
        // there is no any property
        assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsProvidedViaPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                Boolean.FALSE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        assertTrue(config.isXsrfProtectionEnabled());
        assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void getApplicationProperty_propertyIsDefinedInParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();

        Mockito.when(appConfig.getStringProperty("foo", null))
                .thenReturn("bar");

        PropertyDeploymentConfiguration configuration = createConfiguration(
                appConfig, new Properties());

        assertEquals("bar",
                configuration.getApplicationProperty("foo"));
        // there is no any property
        assertTrue(configuration.getInitParameters().isEmpty());
    }

    @Test
    public void getApplicationProperty_propertyIsDefinedInPropertiesAndParent_valueFromPropertiesIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();

        Mockito.when(appConfig.getStringProperty("foo", null))
                .thenReturn("bar");

        Properties properties = new Properties();
        properties.put("foo", "baz");

        PropertyDeploymentConfiguration configuration = createConfiguration(
                appConfig, properties);

        assertEquals("baz",
                configuration.getApplicationProperty("foo"));
        assertEquals(properties, configuration.getInitParameters());
    }

    @Test
    public void isProductionMode_modeIsProvidedViaParentOnly_propertyIsSetToAnotherValue_valueFromParentIsReturnedViaAPI() {
        ApplicationConfiguration appConfig = mockAppConfig();

        // The property value is provided via API
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);

        // The property whose value is overridden above via API is different
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections.singleton(
                        InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE)));

        Mockito.when(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, null))
                .thenReturn(Boolean.FALSE.toString());

        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        // Several things are checked: the value from parent is used via API and
        // deployment configuration doesn't read the property directly even
        // though its "getInitParameters" method returns the property. Also
        // "getApplicationProperty" method checks the parent properties which
        // should not be taken into account here
        assertTrue(config.isProductionMode());
        assertTrue(config.getInitParameters()
                .containsKey(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE));
    }

    @Test
    public void isPnpmEnabled_valueIsProvidedViaParentOnly_propertyIsSetToAnotherValue_valueFromParentIsReturnedViaAPI() {
        ApplicationConfiguration appConfig = mockAppConfig();

        // The property value is provided via API
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(true);

        // The property whose value is overridden above via API is different
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections.singleton(
                        InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)));

        Mockito.when(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_ENABLE_PNPM, null))
                .thenReturn(Boolean.FALSE.toString());

        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        // Several things are checked: the value from parent is used via API and
        // deployment configuration doesn't read the property directly even
        // though its "getInitParameters" method returns the property. Also
        // "getApplicationProperty" method checks the parent properties which
        // should not be taken into account here
        assertTrue(config.isPnpmEnabled());
        assertTrue(config.getInitParameters()
                .containsKey(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
    }

    @Test
    public void reuseDevServer_valueIsProvidedViaParentOnly_propertyIsSetToAnotherValue_valueFromParentIsReturnedViaAPI() {
        ApplicationConfiguration appConfig = mockAppConfig();

        // The property value is provided via API
        Mockito.when(appConfig.reuseDevServer()).thenReturn(true);

        // The property whose value is overridden above via API is different
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections.singleton(
                        InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER)));

        Mockito.when(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER, null))
                .thenReturn(Boolean.FALSE.toString());

        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        // Several things are checked: the value from parent is used via API and
        // deployment configuration doesn't read the property directly even
        // though its "getInitParameters" method returns the property. Also
        // "getApplicationProperty" method checks the parent properties which
        // should not be taken into account here
        assertTrue(config.reuseDevServer());
        assertTrue(config.getInitParameters().containsKey(
                InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER));
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsProvidedViaParentOnly_propertyIsSetToAnotherValue_valueFromParentIsReturnedViaAPI() {
        ApplicationConfiguration appConfig = mockAppConfig();

        // The property value is provided via API
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(true);

        // The property whose value is overridden above via API is different
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections.singleton(
                        InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION)));

        Mockito.when(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION, null))
                .thenReturn(Boolean.TRUE.toString());

        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        // Several things are checked: the value from parent is used via API and
        // deployment configuration doesn't read the property directly even
        // though its "getInitParameters" method returns the property. Also
        // "getApplicationProperty" method checks the parent properties which
        // should not be taken into account here
        assertTrue(config.isXsrfProtectionEnabled());
        assertTrue(config.getInitParameters().containsKey(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION));
    }

    @Test
    public void getInitParameters_prorprtiesAreMergedFromParentAndDeploymentConfig() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames()).thenReturn(
                Collections.enumeration(Collections.singleton("foo")));

        Mockito.when(appConfig.getStringProperty("foo", null))
                .thenReturn("foobar");

        Properties properties = new Properties();
        properties.put("bar", "baz");
        PropertyDeploymentConfiguration configuration = createConfiguration(
                appConfig, properties);
        Properties initParameters = configuration.getInitParameters();

        assertEquals("foobar", initParameters.get("foo"));
        assertEquals("baz", initParameters.get("bar"));
    }

    @Test
    public void allDefaultAbstractConfigurationMethodsAreOverridden() {
        Method[] methods = PropertyDeploymentConfiguration.class.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.equals("getProjectFolder")
                    || methodName.equals("getMode")) {
                // You cannot override these
                continue;
            }
            assertNotEquals(AbstractConfiguration.class,
                    method.getDeclaringClass(),
                    "There is a method '" + methodName
                            + "' which is declared in  "
                            + AbstractConfiguration.class
                            + " interface but it's not overriden in the "
                            + PropertyDeploymentConfiguration.class
                            + ". That's most likely a mistake because every method implementation in "
                            + PropertyDeploymentConfiguration.class
                            + " must take into account parent "
                            + ApplicationConfiguration.class
                            + " API which shares the same interface "
                            + AbstractConfiguration.class + " with "
                            + PropertyDeploymentConfiguration.class
                            + ", so every API method should call parent config and may not use just default implementation of "
                            + AbstractConfiguration.class);
        }
    }

    private ApplicationConfiguration mockAppConfig() {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());

        return appConfig;
    }

    private PropertyDeploymentConfiguration createConfiguration(
            ApplicationConfiguration appConfig, Properties properties) {
        return new PropertyDeploymentConfiguration(appConfig, Object.class,
                properties);
    }
}
