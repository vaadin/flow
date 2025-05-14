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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class PropertyDeploymentConfigurationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void isProductionMode_modeIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.isProductionMode());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
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
        Assert.assertTrue(config.isProductionMode());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void frontendHotdeploy_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.frontendHotdeploy()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.frontendHotdeploy());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
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
        Assert.assertTrue(config.reuseDevServer());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void reuseDevServer_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.reuseDevServer()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.reuseDevServer());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
    }

    @Test
    public void frontendHotdeploy_valueIsProvidedViaPropertiesAndParent_valueIsAlwaysTrueIfExpressBuildIsOFF() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.frontendHotdeploy()).thenReturn(false);

        Properties properties = new Properties();
        properties.put(InitParameters.FRONTEND_HOTDEPLOY,
                Boolean.TRUE.toString());
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                properties);
        Assert.assertTrue(config.frontendHotdeploy());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void isPnpmEnabled_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.isPnpmEnabled());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
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
        Assert.assertTrue(config.isPnpmEnabled());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsProvidedViaParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(true);
        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        Assert.assertTrue(config.isXsrfProtectionEnabled());
        // there is no any property
        Assert.assertTrue(config.getInitParameters().isEmpty());
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
        Assert.assertTrue(config.isXsrfProtectionEnabled());
        Assert.assertEquals(properties, config.getInitParameters());
    }

    @Test
    public void getApplicationProperty_propertyIsDefinedInParentOnly_valueFromParentIsReturned() {
        ApplicationConfiguration appConfig = mockAppConfig();

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
        ApplicationConfiguration appConfig = mockAppConfig();

        Mockito.when(appConfig.getStringProperty("foo", null))
                .thenReturn("bar");

        Properties properties = new Properties();
        properties.put("foo", "baz");

        PropertyDeploymentConfiguration configuration = createConfiguration(
                appConfig, properties);

        Assert.assertEquals("baz", configuration.getApplicationProperty("foo"));
        Assert.assertEquals(properties, configuration.getInitParameters());
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
        Assert.assertTrue(config.isProductionMode());
        Assert.assertTrue(config.getInitParameters()
                .containsKey(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE));
    }

    @Test
    public void frontendHotdeploy_valueIsProvidedViaParentOnly_propertyIsSetToAnotherValue_valueFromParentIsReturnedViaAPI() {
        ApplicationConfiguration appConfig = mockAppConfig();

        // The property value is provided via API
        Mockito.when(appConfig.frontendHotdeploy()).thenReturn(true);

        // The property whose value is overridden above via API is different
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections
                        .singleton(InitParameters.FRONTEND_HOTDEPLOY)));

        Mockito.when(appConfig
                .getStringProperty(InitParameters.FRONTEND_HOTDEPLOY, null))
                .thenReturn(Boolean.FALSE.toString());

        PropertyDeploymentConfiguration config = createConfiguration(appConfig,
                new Properties());
        // Several things are checked: the value from parent is used via API and
        // deployment configuration doesn't read the property directly even
        // though its "getInitParameters" method returns the property. Also
        // "getApplicationProperty" method checks the parent properties which
        // should not be taken into account here
        Assert.assertTrue(config.frontendHotdeploy());
        Assert.assertTrue(config.getInitParameters()
                .containsKey(InitParameters.FRONTEND_HOTDEPLOY));
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
        Assert.assertTrue(config.isPnpmEnabled());
        Assert.assertTrue(config.getInitParameters()
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
        Assert.assertTrue(config.reuseDevServer());
        Assert.assertTrue(config.getInitParameters().containsKey(
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
        Assert.assertTrue(config.isXsrfProtectionEnabled());
        Assert.assertTrue(config.getInitParameters().containsKey(
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

        Assert.assertEquals("foobar", initParameters.get("foo"));
        Assert.assertEquals("baz", initParameters.get("bar"));
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
            Assert.assertNotEquals("There is a method '" + methodName
                    + "' which is declared in  " + AbstractConfiguration.class
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
                    + AbstractConfiguration.class, AbstractConfiguration.class,
                    method.getDeclaringClass());
        }
    }

    @Test
    public void frontendHotDeploy_hillaInLegacyFrontendFolderExists_usesLegacyAndHotdeploy()
            throws IOException {
        File projectRoot = tempFolder.getRoot();
        File legacyFrontend = tempFolder
                .newFolder(FrontendUtils.LEGACY_FRONTEND_DIR);

        File legacyFrontendViews = new File(legacyFrontend,
                FrontendUtils.HILLA_VIEWS_PATH);
        if (!legacyFrontendViews.mkdir()) {
            Assert.fail("Failed to generate legacy frontend views folder");
        }

        File viewFile = new File(legacyFrontendViews, "MyView.tsx");
        org.apache.commons.io.FileUtils.writeStringToFile(viewFile,
                "export default function MyView(){}", "UTF-8");

        ApplicationConfiguration appConfig = new ApplicationConfiguration() {

            @Override
            public File getProjectFolder() {
                return projectRoot;
            }

            @Override
            public Enumeration<String> getPropertyNames() {
                return Collections.emptyEnumeration();
            }

            @Override
            public VaadinContext getContext() {
                return null;
            }

            @Override
            public boolean isDevModeSessionSerializationEnabled() {
                return false;
            }

            @Override
            public boolean isProductionMode() {
                return false;
            }

            @Override
            public String getStringProperty(String name, String defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean getBooleanProperty(String name,
                    boolean defaultValue) {
                return defaultValue;
            }
        };

        try (MockedStatic<EndpointRequestUtil> util = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            util.when(EndpointRequestUtil::isHillaAvailable).thenReturn(true);
            boolean hotdeploy = appConfig.frontendHotdeploy();
            Assert.assertTrue("Should use the legacy frontend folder",
                    hotdeploy);
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
