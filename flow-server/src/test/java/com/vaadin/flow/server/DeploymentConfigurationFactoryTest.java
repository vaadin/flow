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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_TOKEN_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;
import static com.vaadin.flow.server.startup.AbstractConfigurationFactory.DEV_FOLDER_MISSING_MESSAGE;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeploymentConfigurationFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File tokenFile;
    private ServletContext contextMock;

    private ApplicationConfiguration appConfiguration;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Map<String, String> defaultServletParams = new HashMap<>();

    private static String globalUserDirValue;

    private static class NoSettings extends VaadinServlet {
    }

    private static class TestUI extends UI {
        private static class ServletWithEnclosingUi extends VaadinServlet {
        }
    }

    @Before
    public void setup() throws IOException {
        System.setProperty("user.dir",
                temporaryFolder.getRoot().getAbsolutePath());
        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + TOKEN_FILE);
        FileUtils.writeLines(tokenFile, Arrays.asList("{", "}"));
        appConfiguration = mockApplicationConfiguration();
        contextMock = Mockito.mock(ServletContext.class);

        defaultServletParams.put(PARAM_TOKEN_FILE, tokenFile.getPath());
    }

    @After
    public void tearDown() {
        tokenFile.delete();
    }

    @BeforeClass
    public static void setupBeforeClass() {
        globalUserDirValue = System.getProperty("user.dir");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (globalUserDirValue != null) {
            System.setProperty("user.dir", globalUserDirValue);
        }
    }

    @Test
    public void servletWithEnclosingUI_hasItsNameInConfig() throws Exception {
        Class<TestUI.ServletWithEnclosingUi> servlet = TestUI.ServletWithEnclosingUi.class;

        Map<String, String> servletConfigParams = new HashMap<>(
                new HashMap<>(defaultServletParams));

        DeploymentConfiguration config = new DeploymentConfigurationFactory()
                .createDeploymentConfiguration(servlet,
                        createVaadinConfigMock(servletConfigParams,
                                Collections.singletonMap(PARAM_TOKEN_FILE,
                                        tokenFile.getPath())));

        Class<?> customUiClass = servlet.getEnclosingClass();
        assertTrue(String.format(
                "Servlet '%s' should have its enclosing class to be UI subclass, but got: '%s'",
                customUiClass, servlet),
                UI.class.isAssignableFrom(customUiClass));
        assertEquals(String.format(
                "Expected DeploymentConfiguration for servlet '%s' to have its enclosing UI class",
                servlet), customUiClass.getName(), config.getUIClassName());
    }

    @Test
    public void servletWithNoEnclosingUI_hasDefaultUiInConfig()
            throws Exception {
        Class<NoSettings> servlet = NoSettings.class;

        Map<String, String> servletConfigParams = new HashMap<>(
                defaultServletParams);

        DeploymentConfiguration config = new DeploymentConfigurationFactory()
                .createDeploymentConfiguration(servlet, createVaadinConfigMock(
                        servletConfigParams, emptyMap()));

        Class<?> notUiClass = servlet.getEnclosingClass();
        assertFalse(String.format(
                "Servlet '%s' should not have its enclosing class to be UI subclass, but got: '%s'",
                notUiClass, servlet), UI.class.isAssignableFrom(notUiClass));
        assertEquals(String.format(
                "Expected DeploymentConfiguration for servlet '%s' to have its enclosing UI class",
                servlet), UI.class.getName(), config.getUIClassName());
    }

    @Test
    public void servletConfigParametersOverrideServletContextParameters()
            throws Exception {
        Class<NoSettings> servlet = NoSettings.class;

        boolean servletConfigProductionModeValue = true;
        int servletConfigHeartbeatIntervalValue = 333;

        Map<String, String> servletConfigParams = new HashMap<>(
                defaultServletParams);
        servletConfigParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(servletConfigProductionModeValue));
        servletConfigParams.put(
                InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(servletConfigHeartbeatIntervalValue));

        boolean servletContextProductionModeValue = false;
        int servletContextHeartbeatIntervalValue = 444;

        Map<String, String> servletContextParams = new HashMap<>();
        servletContextParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(servletContextProductionModeValue));
        servletContextParams.put(
                InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(servletContextHeartbeatIntervalValue));

        DeploymentConfiguration config = new DeploymentConfigurationFactory()
                .createDeploymentConfiguration(servlet, createVaadinConfigMock(
                        servletConfigParams, servletContextParams));

        assertEquals(
                "Unexpected value for production mode, should be the same as in servlet context parameters",
                servletConfigProductionModeValue, config.isProductionMode());
        assertEquals(
                "Unexpected value for heartbeat interval, should be the same as in servlet context parameters",
                servletConfigHeartbeatIntervalValue,
                config.getHeartbeatInterval());
    }

    @Test
    public void servletConfigParameters_nullValues_ignored() throws Exception {
        Class<NoSettings> servlet = NoSettings.class;

        Map<String, String> servletConfigParams = new HashMap<>(
                defaultServletParams);
        servletConfigParams.put("someKey", null);
        servletConfigParams.put("someNotNullKey", "NOT_NULL");

        Map<String, String> servletContextParams = new HashMap<>();

        DeploymentConfiguration config = new DeploymentConfigurationFactory()
                .createDeploymentConfiguration(servlet, createVaadinConfigMock(
                        servletConfigParams, servletContextParams));

        Assert.assertFalse(
                "Expecting null parameter to be ignored, but was in configuration",
                config.getInitParameters().containsKey("someKey"));
        Assert.assertTrue(
                "Expecting not null parameter to be in configuration, but was not",
                config.getInitParameters().containsKey("someNotNullKey"));
    }

    @Test
    public void should_readConfigurationFromTokenFile() throws Exception {
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"productionMode\": true", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));
        assertTrue(config.isProductionMode());
    }

    @Test
    public void shouldThrow_tokenFileContainsNonExistingNpmFolderInDevMode()
            throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                String.format(DEV_FOLDER_MISSING_MESSAGE, "npm"));
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"productionMode\": false,",
                        "\"npmFolder\": \"npm\",",
                        "\"generatedFolder\": \"generated\",",
                        "\"frontendFolder\": \"frontend\"", "}"));

        createConfig(Collections.singletonMap(PARAM_TOKEN_FILE,
                tokenFile.getPath()));
    }

    @Test
    public void shouldThrow_tokenFileContainsNonExistingFrontendFolderNoNpmFolder()
            throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                String.format(DEV_FOLDER_MISSING_MESSAGE, "frontend"));
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"productionMode\": false,",
                        "\"frontendFolder\": \"frontend\"", "}"));

        createConfig(Collections.singletonMap(PARAM_TOKEN_FILE,
                tokenFile.getPath()));
    }

    @Test
    public void shouldThrow_tokenFileContainsNonExistingFrontendFolderOutsideNpmSubFolder()
            throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                String.format(DEV_FOLDER_MISSING_MESSAGE, "frontend"));
        temporaryFolder.newFolder("npm");
        String tempFolder = temporaryFolder.getRoot().getAbsolutePath()
                .replace("\\", "/");
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"productionMode\": false,",
                        "\"npmFolder\": \"" + tempFolder + "/npm\",",
                        "\"frontendFolder\": \"frontend\"", "}"));

        createConfig(Collections.singletonMap(PARAM_TOKEN_FILE,
                tokenFile.getPath()));
    }

    @Test
    public void shouldNotThrow_tokenFileFrontendFolderInDevMode()
            throws Exception {
        temporaryFolder.newFolder("npm");
        String tempFolder = temporaryFolder.getRoot().getAbsolutePath()
                .replace("\\", "/");
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"productionMode\": false,",
                "\"npmFolder\": \"" + tempFolder + "/npm\",",
                "\"frontendFolder\": \"" + tempFolder + "/npm/frontend\"",
                "}"));

        createConfig(Collections.singletonMap(PARAM_TOKEN_FILE,
                tokenFile.getPath()));
    }

    @Test
    public void shouldNotThrow_tokenFileFoldersExist() throws Exception {
        temporaryFolder.newFolder("npm");
        temporaryFolder.newFolder("frontend");
        String tempFolder = temporaryFolder.getRoot().getAbsolutePath()
                .replace("\\", "/");
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"productionMode\": false,",
                        "\"npmFolder\": \"" + tempFolder + "/npm\",",
                        "\"frontendFolder\": \"" + tempFolder + "/frontend\"",
                        "}"));

        createConfig(Collections.singletonMap(PARAM_TOKEN_FILE,
                tokenFile.getPath()));
    }

    @Test
    public void createInitParameters_valuesFromContextAreIgnored_valuesAreTakenFromservletConfig() {
        DeploymentConfigurationFactory factory = new DeploymentConfigurationFactory();

        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        Mockito.when(config.getVaadinContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Lookup.class))
                .thenReturn(Mockito.mock(Lookup.class));

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(config.getConfigParameterNames()).thenReturn(
                Collections.enumeration(Collections.singleton("foo")));
        Mockito.when(context.getContextParameterNames()).thenReturn(
                Collections.enumeration(Collections.singleton("bar")));

        Mockito.when(config.getConfigParameter("foo")).thenReturn("baz");
        Mockito.when(context.getContextParameter("bar")).thenReturn("foobar");

        Mockito.when(context.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(appConfig);

        Properties parameters = factory.createInitParameters(Object.class,
                config);

        Assert.assertEquals("baz", parameters.get("foo"));
        Assert.assertFalse(parameters.contains("bar"));
    }

    @Test
    public void createInitParameters_valuesAreTakenFromservletConfigAndTokenFile_valuesFromTokenFileOverridenByServletConfig()
            throws Exception {
        DeploymentConfigurationFactory factory = new DeploymentConfigurationFactory();

        Set<String> stringParams = new HashSet<>(Arrays.asList(
                InitParameters.UI_PARAMETER,
                InitParameters.SERVLET_PARAMETER_REQUEST_TIMING,
                InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                InitParameters.SERVLET_PARAMETER_PUSH_SERVLET_MAPPING,
                InitParameters.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING,
                InitParameters.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT,
                InitParameters.SERVLET_PARAMETER_STATISTICS_JSON,
                InitParameters.SERVLET_PARAMETER_DEVMODE_VITE_OPTIONS,
                InitParameters.COMPILED_WEB_COMPONENTS_PATH,
                InitParameters.NODE_VERSION, InitParameters.NODE_DOWNLOAD_ROOT,
                InitParameters.BUILD_FOLDER,
                InitParameters.APPLICATION_IDENTIFIER,
                InitParameters.FRONTEND_EXTRA_EXTENSIONS));
        Field[] initParamFields = InitParameters.class.getDeclaredFields();
        String mockTokenJsonString = generateJsonStringFromFields(
                initParamFields, stringParams);
        VaadinConfig config = mockTokenFileViaContextParam(mockTokenJsonString);
        List<String> allParamsList = mockParamsFromFields(initParamFields,
                config, stringParams);
        allParamsList.add(FrontendUtils.PARAM_TOKEN_FILE);
        // let's not set production mode to see if token setting still
        // works
        allParamsList.removeIf(paramString -> paramString
                .equals(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE));
        Mockito.when(config.getConfigParameterNames())
                .thenReturn(Collections.enumeration(allParamsList));

        VaadinContext context = Mockito.mock(VaadinContext.class);

        Mockito.when(config.getVaadinContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Lookup.class))
                .thenReturn(Mockito.mock(Lookup.class));

        Properties parameters = factory.createInitParameters(Object.class,
                config);

        for (int i = 0; i < initParamFields.length; i++) {
            String paramName = (String) initParamFields[i].get(null);
            mockTokenJsonString += "'" + paramName + "': ";
            if (!stringParams.contains(paramName)) {
                // the one we set from flow-build-info.json
                if (paramName.equals(
                        InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE)) {
                    Assert.assertEquals(
                            InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE
                                    + " (boolean parameter) does not have expected value set from token file",
                            "true", parameters.get(paramName));
                } else {
                    Assert.assertEquals(paramName
                            + " (boolean parameter) does not have expected value set from servlet config",
                            "false", parameters.get(paramName));
                }
            } else {
                Assert.assertEquals(paramName
                        + "(string parameter) does not have expected value set from servlet config",
                        "foo", parameters.get(paramName));
            }
        }
    }

    private List<String> mockParamsFromFields(Field[] fields,
            VaadinConfig config, Set<String> stringParams) {
        List<String> allParamsList = new ArrayList<String>();
        for (int i = 0; i < fields.length; i++) {
            try {
                String paramName = (String) fields[i].get(null);
                if (!stringParams.contains(paramName)) {
                    Mockito.when(config.getConfigParameter(paramName))
                            .thenReturn("false");
                } else {
                    Mockito.when(config.getConfigParameter(paramName))
                            .thenReturn("foo");
                }
                allParamsList.add(paramName);
            } catch (IllegalAccessException illegalAccess) {
                Assert.fail("Illegal access to InitParameters class: "
                        + illegalAccess.getMessage());
            }
        }
        return allParamsList;
    }

    private String generateJsonStringFromFields(Field[] fields,
            Set<String> stringParams) {
        String mockTokenJsonString = "{";
        for (int i = 0; i < fields.length; i++) {
            try {
                String paramName = (String) fields[i].get(null);
                mockTokenJsonString += "\"" + paramName + "\": ";
                if (!stringParams.contains(paramName)) {
                    mockTokenJsonString += "true";
                } else {
                    mockTokenJsonString += " \"bar\"";
                }

            } catch (IllegalAccessException illegalAccess) {
                Assert.fail("Illegal access to InitParameters class: "
                        + illegalAccess.getMessage());
            }
            if (i < fields.length - 1) {
                mockTokenJsonString += ",";
            }
        }
        mockTokenJsonString += " }";
        return mockTokenJsonString;
    }

    @Test
    public void createInitParameters_tokenFileIsSetViaContext_externalStatsUrlIsReadFromTokenFile_predefinedProperties()
            throws Exception {
        DeploymentConfigurationFactory factory = new DeploymentConfigurationFactory();

        VaadinConfig config = mockTokenFileViaContextParam(
                "{ \"externalStatsUrl\": \"http://my.server/static/stats.json\"}");

        VaadinContext context = Mockito.mock(VaadinContext.class);

        Mockito.when(config.getVaadinContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Lookup.class))
                .thenReturn(Mockito.mock(Lookup.class));

        Properties parameters = factory.createInitParameters(Object.class,
                config);

        Assert.assertEquals("http://my.server/static/stats.json",
                parameters.get(Constants.EXTERNAL_STATS_URL));
        Assert.assertEquals(Boolean.TRUE.toString(),
                parameters.get(Constants.EXTERNAL_STATS_FILE));
    }

    @Test
    public void createInitParameters_tokenFileIsSetViaContext_externalStatsFileIsReadFromTokenFile_predefinedProperties()
            throws Exception {
        DeploymentConfigurationFactory factory = new DeploymentConfigurationFactory();

        VaadinConfig config = mockTokenFileViaContextParam(
                "{ \"externalStatsFile\": true}");

        VaadinContext context = Mockito.mock(VaadinContext.class);

        Mockito.when(config.getVaadinContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Lookup.class))
                .thenReturn(Mockito.mock(Lookup.class));

        Properties parameters = factory.createInitParameters(Object.class,
                config);

        Assert.assertEquals(Boolean.TRUE.toString(),
                parameters.get(Constants.EXTERNAL_STATS_FILE));
    }

    @Test
    public void createInitParameters_tokenFileIsSetViaContext_setPropertyFromTokenFile()
            throws Exception {
        DeploymentConfigurationFactory factory = new DeploymentConfigurationFactory();

        VaadinConfig config = mockTokenFileViaContextParam(
                "{ \"" + SERVLET_PARAMETER_PRODUCTION_MODE + "\": true}");

        VaadinContext context = Mockito.mock(VaadinContext.class);

        Mockito.when(config.getVaadinContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Lookup.class))
                .thenReturn(Mockito.mock(Lookup.class));

        Properties parameters = factory.createInitParameters(Object.class,
                config);

        Assert.assertEquals(Boolean.TRUE.toString(),
                parameters.get(SERVLET_PARAMETER_PRODUCTION_MODE));
    }

    private VaadinConfig mockTokenFileViaContextParam(String content)
            throws IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(config.getConfigParameterNames())
                .thenReturn(Collections.enumeration(
                        Collections.singleton(FrontendUtils.PARAM_TOKEN_FILE)));
        Mockito.when(context.getContextParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        Mockito.when(config.getVaadinContext()).thenReturn(context);

        File tmpFile = temporaryFolder.newFile();
        Files.write(tmpFile.toPath(), Collections.singletonList(content));

        Mockito.when(
                context.getContextParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .thenReturn(tmpFile.getPath());

        Mockito.when(config.getConfigParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .thenReturn(tmpFile.toString());

        Mockito.when(context.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(appConfig);

        return config;
    }

    @Test
    public void externalStatsFileTrue_predefinedValuesAreNotOverridden_productionMode()
            throws Exception {
        // note that this situation shouldn't happen that the other
        // settings would be against the external usage.
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"frontend.hotdeploy\": true,",
                        // production mode can be
                        // altered even when external
                        // stats are used
                        "\"productionMode\": true,",
                        "\"externalStatsFile\": true", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertTrue(config.isProductionMode());
        assertTrue(config.isStatsExternal());
        assertEquals(Constants.DEFAULT_EXTERNAL_STATS_URL,
                config.getExternalStatsUrl());
    }

    @Test
    public void createInitParameters_readDevModeProperties() throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"pnpm.enable\": true,", "\"require.home.node\": true", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        Assert.assertEquals(Boolean.TRUE.toString(), config.getInitParameters()
                .getProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
        Assert.assertEquals(Boolean.TRUE.toString(), config.getInitParameters()
                .getProperty(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE));
    }

    @Test
    public void createInitParameters_initParamtersAreSet_tokenDevModePropertiesAreNotSet()
            throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"pnpm.enable\": true,", "\"require.home.node\": true", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        config.getInitParameters().setProperty(
                InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                Boolean.FALSE.toString());
        config.getInitParameters().setProperty(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE,
                Boolean.FALSE.toString());
        config.getInitParameters().setProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                Boolean.FALSE.toString());

        Assert.assertEquals(Boolean.FALSE.toString(), config.getInitParameters()
                .getProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
        Assert.assertEquals(Boolean.FALSE.toString(), config.getInitParameters()
                .getProperty(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE));
        Assert.assertEquals(Boolean.FALSE.toString(),
                config.getInitParameters().getProperty(
                        InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE));
    }

    private DeploymentConfiguration createConfig(Map<String, String> map)
            throws Exception {
        return new DeploymentConfigurationFactory()
                .createDeploymentConfiguration(VaadinServlet.class,
                        createVaadinConfigMock(map, emptyMap()));
    }

    private VaadinConfig createVaadinConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters) throws Exception {
        return new VaadinServletConfig(createServletConfigMock(
                servletConfigParameters, servletContextParameters));
    }

    private ApplicationConfiguration mockApplicationConfiguration() {
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(configuration.getContext()).thenReturn(context);
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString())).thenReturn(null);
        Mockito.when(configuration.isXsrfProtectionEnabled()).thenReturn(false);

        Mockito.when(configuration.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());

        return configuration;

    }

    private ServletConfig createServletConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters) throws Exception {

        URLClassLoader classLoader = new URLClassLoader(
                new URL[] { temporaryFolder.getRoot().toURI().toURL() });

        Mockito.when(contextMock
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfiguration);

        Mockito.when(contextMock.getInitParameterNames()).thenReturn(
                Collections.enumeration(servletContextParameters.keySet()));
        Mockito.when(contextMock.getClassLoader()).thenReturn(classLoader);

        Mockito.when(contextMock.getInitParameter(Mockito.anyString()))
                .thenAnswer(answer -> {
                    String name = answer.getArgument(0);
                    return servletContextParameters.get(name);
                });

        ResourceProvider provider = Mockito.mock(ResourceProvider.class);

        Lookup lookup = new Lookup() {

            @Override
            public <T> Collection<T> lookupAll(Class<T> serviceClass) {
                return null;
            }

            @Override
            public <T> T lookup(Class<T> serviceClass) {
                if (ResourceProvider.class.equals(serviceClass)) {
                    return serviceClass.cast(provider);
                }
                return null;
            }
        };

        Mockito.when(provider
                .getApplicationResources(VAADIN_SERVLET_RESOURCES + TOKEN_FILE))
                .thenReturn(Collections.emptyList());

        Mockito.when(contextMock.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        return new ServletConfig() {
            @Override
            public String getServletName() {
                return "whatever";
            }

            @Override
            public ServletContext getServletContext() {
                return contextMock;
            }

            @Override
            public String getInitParameter(String name) {
                return servletConfigParameters.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections
                        .enumeration(servletConfigParameters.keySet());
            }
        };
    }

}
