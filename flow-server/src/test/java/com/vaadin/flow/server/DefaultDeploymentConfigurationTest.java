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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link DefaultDeploymentConfiguration}
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
class DefaultDeploymentConfigurationTest {
    @TempDir
    Path tempFolder;
    VaadinContext context;

    @BeforeEach
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
        assertTrue(config.isSendUrlsAsParameters(),
                "Boolean value equal to 'true' ignoring case should be interpreted as 'true'");
    }

    @Test
    public void booleanValueReadIgnoreTheCase_false() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "FaLsE");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertFalse(config.isSendUrlsAsParameters(),
                "Boolean value equal to 'false' ignoring case should be interpreted as 'false'");
    }

    @Test
    public void booleanValueRead_emptyIsTrue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS, "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertTrue(config.isSendUrlsAsParameters(),
                "Empty boolean value should be interpreted as 'true'");
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

    @Test
    public void booleanValueRead_exceptionOnNonBooleanValue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "incorrectValue");

        assertThrows(IllegalArgumentException.class,
                () -> createDeploymentConfig(initParameters));
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
        assertNull(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, null));

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, new Properties());
        assertTrue(config.isProductionMode());
        assertTrue(config.getProperties().isEmpty());
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
        assertTrue(config.isProductionMode());
    }

    @Test
    public void isXsrfProtectionEnabled_valueIsSetViaParentOnly_valueIsTakenFromParent() {
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.isXsrfProtectionEnabled()).thenReturn(true);

        // Note: application configuration doesn't contain production mode
        // parameter !
        assertNull(appConfig.getStringProperty(
                InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                null));

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, new Properties());
        assertTrue(config.isXsrfProtectionEnabled());
        assertTrue(config.getProperties().isEmpty());
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
        assertTrue(config.isXsrfProtectionEnabled());
    }

    @Test
    public void frontendHotdeployParameter_developmentBundle_resetsFrontendHotdeployToFalse() {
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                new Properties());
        assertEquals(Mode.DEVELOPMENT_BUNDLE, config.getMode(),
                "Expected dev server to be disabled by default");

        Properties init = new Properties();
        init.put(InitParameters.FRONTEND_HOTDEPLOY, "true");
        config = createDeploymentConfig(init);
        assertEquals(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD, config.getMode(),
                "Expected dev server to be enabled when set true");
    }

    @Test
    public void frontendHotdeploy_defaultsToParentConfiguration() {
        ApplicationConfiguration appConfig = setupAppConfig();
        Mockito.when(appConfig.getMode())
                .thenReturn(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD);
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                appConfig, new Properties());

        assertEquals(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD, config.getMode(),
                "Expected dev server to be enabled from parent configuration");
    }

    @Test
    public void checkLockStrategy_defaultsToAssert() {
        Properties init = new Properties();
        DefaultDeploymentConfiguration config = createDeploymentConfig(init);

        assertEquals(SessionLockCheckStrategy.ASSERT,
                config.getSessionLockCheckStrategy());
    }

    @Test
    public void checkLockStrategy_configurableViaPropertyParameter() {
        Properties init = new Properties();
        init.put(InitParameters.SERVLET_PARAMETER_SESSION_LOCK_CHECK_STRATEGY,
                "throw");
        DefaultDeploymentConfiguration config = createDeploymentConfig(init);

        assertEquals(SessionLockCheckStrategy.THROW,
                config.getSessionLockCheckStrategy());
    }

    @Test
    public void productionModeTrue_frontendHotdeployTrue_frontendHotdeployReturnsFalse() {
        Properties init = new Properties();
        init.put(InitParameters.FRONTEND_HOTDEPLOY, "true");
        init.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, "true");

        DefaultDeploymentConfiguration config = createDeploymentConfig(init);

        assertTrue(config.isProductionMode(),
                "ProductionMode should be enabled");
    }

    @Test
    public void hillaViewInLegacyFrontendFolderExists_shouldUseLegacyFolderAndHotdeploy()
            throws IOException {
        File projectRoot = tempFolder.toFile();
        File legacyFrontend = Files
                .createDirectories(
                        tempFolder.resolve(FrontendUtils.LEGACY_FRONTEND_DIR))
                .toFile();

        File legacyFrontendViews = new File(legacyFrontend,
                FrontendUtils.HILLA_VIEWS_PATH);
        if (!legacyFrontendViews.mkdir()) {
            fail("Failed to generate legacy frontend views folder");
        }

        File viewFile = new File(legacyFrontendViews, "MyView.tsx");
        org.apache.commons.io.FileUtils.writeStringToFile(viewFile,
                "export default function MyView(){}", "UTF-8");

        try (MockedStatic<EndpointRequestUtil> util = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            util.when(EndpointRequestUtil::isHillaAvailable).thenReturn(true);
            Properties init = new Properties();
            init.put(FrontendUtils.PROJECT_BASEDIR,
                    projectRoot.getAbsolutePath());
            DefaultDeploymentConfiguration config = createDeploymentConfig(
                    init);
            assertEquals(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD, config.getMode(),
                    "Should use the legacy frontend folder");
        }
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
        Mockito.when(appConfig.getFrontendFolder()).thenCallRealMethod();
        Mockito.when(
                appConfig.getStringProperty(FrontendUtils.PARAM_FRONTEND_DIR,
                        FrontendUtils.DEFAULT_FRONTEND_DIR))
                .thenReturn(FrontendUtils.DEFAULT_FRONTEND_DIR);
        Mockito.when(appConfig.getProjectFolder())
                .thenReturn(tempFolder.toFile());
        Mockito.when(appConfig.getContext()).thenReturn(context);
        return appConfig;
    }
}
