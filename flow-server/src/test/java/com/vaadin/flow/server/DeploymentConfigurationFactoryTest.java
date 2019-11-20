package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
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
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FallbackChunk.CssImportData;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.DeploymentConfigurationFactory.DEV_FOLDER_MISSING_MESSAGE;
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

        defaultServletParams.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.FALSE.toString());
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
        servletConfigParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
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
        servletContextParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
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
        servletConfigParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(servletConfigHeartbeatIntervalValue));

        boolean servletContextProductionModeValue = false;
        int servletContextHeartbeatIntervalValue = 444;

        Map<String, String> servletContextParams = new HashMap<>();
        servletContextParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(servletContextProductionModeValue));
        servletContextParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
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
    public void should_throwIfModeNotSet() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Unable to determine mode of operation.");
        createConfig(emptyMap());
    }

    @Test
    public void should_throwIfCompatibilityModeIsFalseButNoTokenFile()
            throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                "The compatibility mode is explicitly set to 'false'");

        URLClassLoader classLoader = new URLClassLoader(new URL[] {});
        expect(contextMock.getClassLoader()).andReturn(classLoader);

        DeploymentConfigurationFactory.createDeploymentConfiguration(
                VaadinServlet.class,
                createVaadinConfigMock(Collections.singletonMap(
                        Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        Boolean.FALSE.toString()), emptyMap()));
    }

    @Test
    public void shouldNotThrowIfCompatibilityModeIsFalse_noTokenFile_correctWebPackConfigExists()
            throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put(FrontendUtils.PROJECT_BASEDIR,
                temporaryFolder.getRoot().getAbsolutePath());
        map.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.FALSE.toString());

        File webPack = new File(temporaryFolder.getRoot().getAbsolutePath(),
                FrontendUtils.WEBPACK_CONFIG);
        FileUtils.writeLines(webPack, Arrays.asList("./webpack.generated.js"));

        DeploymentConfigurationFactory.createDeploymentConfiguration(
                VaadinServlet.class, createVaadinConfigMock(map, emptyMap()));
    }

    @Test
    public void shouldThrowIfCompatibilityModeIsFalse_noTokenFile_incorrectWebPackConfigExists()
            throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                "The compatibility mode is explicitly set to 'false'");

        URLClassLoader classLoader = new URLClassLoader(new URL[] {});
        expect(contextMock.getClassLoader()).andReturn(classLoader);

        Map<String, String> map = new HashMap<>();
        map.put(FrontendUtils.PROJECT_BASEDIR,
                temporaryFolder.getRoot().getAbsolutePath());
        map.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.FALSE.toString());

        File webPack = new File(temporaryFolder.getRoot().getAbsolutePath(),
                FrontendUtils.WEBPACK_CONFIG);
        webPack.createNewFile();

        DeploymentConfigurationFactory.createDeploymentConfiguration(
                VaadinServlet.class, createVaadinConfigMock(map, emptyMap()));
    }

    @Test
    public void should_readConfigurationFromTokenFile() throws Exception {
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{",
                        "\"compatibilityMode\": false,",
                        "\"productionMode\": true",
                        "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));
        assertFalse(config.isCompatibilityMode());
        assertTrue(config.isProductionMode());
    }

    @Test
    public void shouldThrow_tokenFileContainsNonExistingNpmFolderInDevMode()
            throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(
                String.format(DEV_FOLDER_MISSING_MESSAGE, "npm"));
        FileUtils.writeLines(tokenFile,
                Arrays.asList("{",
                        "\"compatibilityMode\": false,",
                        "\"productionMode\": false,",
                        "\"npmFolder\": \"npm\",",
                        "\"generatedFolder\": \"generated\",",
                        "\"frontendFolder\": \"frontend\"",
                        "}"));

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
                Arrays.asList("{",
                        "\"compatibilityMode\": false,",
                        "\"productionMode\": false,",
                        "\"frontendFolder\": \"frontend\"",
                        "}"));

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
                Arrays.asList("{",
                        "\"compatibilityMode\": false,",
                        "\"productionMode\": false,",
                        "\"npmFolder\": \"" + tempFolder + "/npm\",",
                        "\"frontendFolder\": \"frontend\"",
                        "}"));

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
                "\"compatibilityMode\": false,",
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
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"compatibilityMode\": false,",
                "\"productionMode\": false,",
                "\"npmFolder\": \"" + tempFolder + "/npm\",",
                "\"frontendFolder\": \"" + tempFolder + "/frontend\"",
                "}"));

        createConfig(Collections.singletonMap(PARAM_TOKEN_FILE,
                tokenFile.getPath()));
    }

    @Test
    public void externalStatsFileTrue_predefinedContext() throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"externalStatsFile\": true",
                "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertEquals(true, config.isProductionMode());
        assertEquals(false, config.isCompatibilityMode());
        assertEquals(false, config.enableDevServer());
        assertEquals(true, config.isStatsExternal());
        assertEquals(Constants.DEFAULT_EXTERNAL_STATS_URL, config.getExternalStatsUrl());
    }

    @Test
    public void externalStatsUrlGiven_predefinedContext() throws Exception {
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"externalStatsUrl\": \"http://my.server/static/stats.json\"",
                "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertEquals(true, config.isProductionMode());
        assertEquals(false, config.isCompatibilityMode());
        assertEquals(false, config.enableDevServer());
        assertEquals(true, config.isStatsExternal());
        assertEquals("http://my.server/static/stats.json", config.getExternalStatsUrl());
    }

    @Test
    public void externalStatsFileTrue_predefinedValuesAreNotOverridden() throws Exception {
        // note that this situation shouldn't happen that the other settings
        // would be against the external usage.
        FileUtils.writeLines(tokenFile, Arrays.asList("{",
                "\"compatibilityMode\": true,",
                "\"enableDevServer\": true,",
                "\"productionMode\": false,",
                "\"externalStatsFile\": true",
                "}"));

        DeploymentConfiguration config = createConfig(Collections
                .singletonMap(PARAM_TOKEN_FILE, tokenFile.getPath()));

        assertEquals(true, config.isProductionMode());
        assertEquals(false, config.isCompatibilityMode());
        assertEquals(false, config.enableDevServer());
        assertEquals(true, config.isStatsExternal());
        assertEquals(Constants.DEFAULT_EXTERNAL_STATS_URL, config.getExternalStatsUrl());
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
                .createInitParameters(Object.class, new VaadinServletConfig(config));

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

    private DeploymentConfiguration createConfig(Map<String, String> map)
            throws Exception {
        return DeploymentConfigurationFactory.createDeploymentConfiguration(
                VaadinServlet.class, createVaadinConfigMock(map, emptyMap()));
    }

    private VaadinConfig createVaadinConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters) throws Exception {
        return new VaadinServletConfig(createServletConfigMock(servletConfigParameters,servletContextParameters));
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
