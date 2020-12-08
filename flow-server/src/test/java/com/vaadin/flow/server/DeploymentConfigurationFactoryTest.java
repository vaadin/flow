package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FallbackChunk.CssImportData;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.DeploymentConfigurationFactory.DEV_FOLDER_MISSING_MESSAGE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_TOKEN_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;
import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeploymentConfigurationFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File tokenFile;
    private ServletContext contextMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Map<String, String> defaultServletParams = new HashMap<>();

    private static class NoSettings extends VaadinServlet {
    }

    private static class TestUI extends UI {
        private static class ServletWithEnclosingUi extends VaadinServlet {
        }
    }

    @VaadinServletConfiguration(productionMode = true, heartbeatInterval = 222)
    private static class VaadinSettings extends VaadinServlet {
    }

    @Before
    public void setup() throws IOException {
        System.setProperty("user.dir",
                temporaryFolder.getRoot().getAbsolutePath());
        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + TOKEN_FILE);
        FileUtils.writeLines(tokenFile, Arrays.asList("{", "}"));
        contextMock = mock(ServletContext.class);

        defaultServletParams.put(PARAM_TOKEN_FILE, tokenFile.getPath());
    }

    public void tearDown() {
        tokenFile.delete();
    }

    @Test
    public void servletWithEnclosingUI_hasItsNameInConfig() throws Exception {
        Class<TestUI.ServletWithEnclosingUi> servlet = TestUI.ServletWithEnclosingUi.class;

        Map<String, String> servletConfigParams = new HashMap<>(
                new HashMap<>(defaultServletParams));

        DeploymentConfiguration config = DeploymentConfigurationFactory
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

        DeploymentConfiguration config = DeploymentConfigurationFactory
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
    public void vaadinServletConfigurationRead() throws Exception {
        Class<VaadinSettings> servlet = VaadinSettings.class;

        Map<String, String> servletConfigParams = new HashMap<>(
                defaultServletParams);

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createVaadinConfigMock(
                        servletConfigParams, emptyMap()));

        assertTrue(String.format(
                "Unexpected value for production mode, check '%s' class annotation",
                servlet), config.isProductionMode());
        assertEquals(String.format(
                "Unexpected value for heartbeat interval, check '%s' class annotation",
                servlet), 222, config.getHeartbeatInterval());
    }

    @Test
    public void servletConfigParametersOverrideVaadinParameters()
            throws Exception {
        Class<VaadinSettings> servlet = VaadinSettings.class;

        boolean overridingProductionModeValue = false;
        int overridingHeartbeatIntervalValue = 444;

        Map<String, String> servletConfigParams = new HashMap<>(
                defaultServletParams);
        servletConfigParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(overridingProductionModeValue));
        servletConfigParams.put(
                InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(overridingHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createVaadinConfigMock(
                        servletConfigParams, emptyMap()));

        assertEquals(
                "Unexpected value for production mode, should be the same as in servlet config parameters",
                overridingProductionModeValue, config.isProductionMode());
        assertEquals(
                "Unexpected value for heartbeat interval, should be the same as in servlet config parameters",
                overridingHeartbeatIntervalValue,
                config.getHeartbeatInterval());
    }

    @Test
    public void servletContextParametersOverrideVaadinParameters()
            throws Exception {
        Class<VaadinSettings> servlet = VaadinSettings.class;

        boolean overridingProductionModeValue = false;
        int overridingHeartbeatIntervalValue = 444;

        Map<String, String> servletContextParams = new HashMap<>(
                defaultServletParams);
        servletContextParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(overridingProductionModeValue));
        servletContextParams.put(
                InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(overridingHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createVaadinConfigMock(
                        emptyMap(), servletContextParams));

        assertEquals(
                "Unexpected value for production mode, should be the same as in servlet context parameters",
                overridingProductionModeValue, config.isProductionMode());
        assertEquals(
                "Unexpected value for heartbeat interval, should be the same as in servlet context parameters",
                overridingHeartbeatIntervalValue,
                config.getHeartbeatInterval());
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

        DeploymentConfiguration config = DeploymentConfigurationFactory
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
    public void shouldNotThrow_noTokenFile_correctWebPackConfigExists()
            throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put(FrontendUtils.PROJECT_BASEDIR,
                temporaryFolder.getRoot().getAbsolutePath());

        File webPack = new File(temporaryFolder.getRoot().getAbsolutePath(),
                FrontendUtils.WEBPACK_CONFIG);
        FileUtils.writeLines(webPack, Arrays.asList("./webpack.generated.js"));

        DeploymentConfigurationFactory.createDeploymentConfiguration(
                VaadinServlet.class, createVaadinConfigMock(map, emptyMap()));
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
    public void externalStatsFileTrue_predefinedContext() throws Exception {
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"externalStatsFile\": true", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertEquals(false, config.isProductionMode());
        assertEquals(false, config.enableDevServer());
        assertEquals(true, config.isStatsExternal());
        assertEquals(Constants.DEFAULT_EXTERNAL_STATS_URL,
                config.getExternalStatsUrl());
    }

    @Test
    public void externalStatsUrlGiven_predefinedContext() throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"externalStatsUrl\": \"http://my.server/static/stats.json\"",
                "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertEquals(false, config.isProductionMode());
        assertEquals(false, config.enableDevServer());
        assertEquals(true, config.isStatsExternal());
        assertEquals("http://my.server/static/stats.json",
                config.getExternalStatsUrl());
    }

    @Test
    public void externalStatsFileTrue_predefinedValuesAreNotOverridden()
            throws Exception {
        // note that this situation shouldn't happen that the other settings
        // would be against the external usage.
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{", "\"enableDevServer\": true,",
                        // production mode can be altered even when external
                        // stats
                        // are used
                        "\"productionMode\": true,",
                        "\"externalStatsFile\": true", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertEquals(true, config.isProductionMode());
        assertEquals(false, config.enableDevServer());
        assertEquals(true, config.isStatsExternal());
        assertEquals(Constants.DEFAULT_EXTERNAL_STATS_URL,
                config.getExternalStatsUrl());
    }

    @Test
    public void createInitParameters_fallbackChunkObjectIsInInitParams()
            throws VaadinConfigurationException, IOException {
        ServletContext context = Mockito.mock(ServletContext.class);
        ServletConfig config = Mockito.mock(ServletConfig.class);
        Mockito.when(config.getServletContext()).thenReturn(context);

        Hashtable<String, String> table = new Hashtable<>(
                Collections.singletonMap(FrontendUtils.PARAM_TOKEN_FILE, ""));
        Mockito.when(context.getInitParameterNames()).thenReturn(table.keys());

        Mockito.when(config.getInitParameterNames())
                .thenReturn(new Hashtable<String, String>().keys());

        File tokenFile = temporaryFolder.newFile();

        Files.write(tokenFile.toPath(),
                Collections.singletonList("{ 'chunks': { " + "'fallback': {"
                        + "            'jsModules': ['foo', 'bar'],"
                        + "           'cssImports': [ { 'value' :'foo-value' , 'id': 'bar-id'}]"
                        + "}}" + "}"));

        Mockito.when(context.getInitParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .thenReturn(tokenFile.getPath());

        Properties properties = DeploymentConfigurationFactory
                .createInitParameters(Object.class,
                        new VaadinServletConfig(config));

        Object object = properties
                .get(DeploymentConfigurationFactory.FALLBACK_CHUNK);

        Assert.assertTrue(object instanceof FallbackChunk);

        FallbackChunk chunk = (FallbackChunk) object;
        Set<String> modules = chunk.getModules();
        Assert.assertEquals(2, modules.size());
        Assert.assertTrue(modules.contains("foo"));
        Assert.assertTrue(modules.contains("bar"));

        Set<CssImportData> cssImports = chunk.getCssImports();
        Assert.assertEquals(1, cssImports.size());
        CssImportData data = cssImports.iterator().next();
        Assert.assertEquals("foo-value", data.getValue());
        Assert.assertEquals("bar-id", data.getId());
    }

    @Test
    public void createInitParameters_readDevModeProperties() throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"pnpm.enable\": true,", "\"require.home.node\": true,", "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        Assert.assertEquals(Boolean.TRUE.toString(), config.getInitParameters()
                .getProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
        Assert.assertEquals(Boolean.TRUE.toString(), config.getInitParameters()
                .getProperty(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE));
    }

    @Test
    public void createInitParameters_readTokenFileFromContext()
            throws VaadinConfigurationException, IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        DeploymentConfigurationFactory.createInitParameters(
                DeploymentConfigurationFactoryTest.class, config);

        Mockito.verify(resourceProvider)
                .getApplicationResources(VAADIN_SERVLET_RESOURCES + TOKEN_FILE);
    }

    @Test
    public void createInitParameters_checkWebpackGeneratedFromContext()
            throws VaadinConfigurationException, IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        String path = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;

        File tmpFile = temporaryFolder.newFile();
        Files.write(tmpFile.toPath(), Collections.singletonList("{}"));

        URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return tmpFile.toURI().toURL().openConnection();
            }
        };
        URL url = new URL("file", "", -1, "foo.jar!/" + path, handler);

        Mockito.when(resourceProvider.getApplicationResources(path))
                .thenReturn(Collections.singletonList(url));

        Mockito.when(resourceProvider
                .getApplicationResource(FrontendUtils.WEBPACK_GENERATED))
                .thenReturn(tmpFile.toURI().toURL());

        DeploymentConfigurationFactory.createInitParameters(
                DeploymentConfigurationFactoryTest.class, config);

        Mockito.verify(resourceProvider)
                .getApplicationResource(FrontendUtils.WEBPACK_GENERATED);

    }

    @Test
    public void createInitParameters_initParamtersAreSet_tokenDevModePropertiesAreNotSet()
            throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"pnpm.enable\": true,", "\"require.home.node\": true,", "}"));

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
        return DeploymentConfigurationFactory.createDeploymentConfiguration(
                VaadinServlet.class, createVaadinConfigMock(map, emptyMap()));
    }

    private VaadinConfig createVaadinConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters) throws Exception {
        return new VaadinServletConfig(createServletConfigMock(
                servletConfigParameters, servletContextParameters));
    }

    private ResourceProvider mockResourceProvider(VaadinConfig config,
            VaadinContext context) throws VaadinConfigurationException {
        Mockito.when(config.getVaadinContext()).thenReturn(context);

        Mockito.when(context.getContextParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(config.getConfigParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        Lookup lookup = Mockito.mock(Lookup.class);
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        return resourceProvider;
    }

    private ServletConfig createServletConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters) throws Exception {

        URLClassLoader classLoader = new URLClassLoader(
                new URL[] { temporaryFolder.getRoot().toURI().toURL() });

        expect(contextMock.getInitParameterNames())
                .andAnswer(() -> Collections
                        .enumeration(servletContextParameters.keySet()))
                .anyTimes();
        expect(contextMock.getClassLoader()).andReturn(classLoader).anyTimes();
        Capture<String> initParameterNameCapture = EasyMock.newCapture();
        expect(contextMock.getInitParameter(capture(initParameterNameCapture)))
                .andAnswer(() -> servletContextParameters
                        .get(initParameterNameCapture.getValue()))
                .anyTimes();

        ResourceProvider provider = EasyMock.mock(ResourceProvider.class);

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

        expect(provider
                .getApplicationResources(VAADIN_SERVLET_RESOURCES + TOKEN_FILE))
                        .andAnswer(() -> Collections.emptyList()).anyTimes();

        replay(provider);

        expect(contextMock.getAttribute(Lookup.class.getName()))
                .andAnswer(() -> lookup).anyTimes();

        Capture<String> resourceCapture = EasyMock.newCapture();
        expect(contextMock.getResource(capture(resourceCapture)))
                .andReturn(null).anyTimes();
        replay(contextMock);

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
