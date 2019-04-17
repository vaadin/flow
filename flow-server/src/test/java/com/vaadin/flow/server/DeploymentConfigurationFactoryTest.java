package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;
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
        System.setProperty("user.dir", temporaryFolder.getRoot().getAbsolutePath());
    }

    @Test
    public void servletWithEnclosingUI_hasItsNameInConfig()
            throws Exception {
        Class<TestUI.ServletWithEnclosingUi> servlet = TestUI.ServletWithEnclosingUi.class;

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet,
                        createServletConfigMock(emptyMap(), emptyMap(), null));

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

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet,
                        createServletConfigMock(emptyMap(), emptyMap(), null));

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

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet,
                        createServletConfigMock(emptyMap(), emptyMap(), null));

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

        Map<String, String> servletConfigParams = new HashMap<>();
        servletConfigParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(overridingProductionModeValue));
        servletConfigParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(overridingHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createServletConfigMock(
                        servletConfigParams, emptyMap(), null));

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

        Map<String, String> servletContextParams = new HashMap<>();
        servletContextParams.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(overridingProductionModeValue));
        servletContextParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(overridingHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createServletConfigMock(
                        emptyMap(), servletContextParams, null));

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

        Map<String, String> servletConfigParams = new HashMap<>();
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
                .createDeploymentConfiguration(servlet, createServletConfigMock(
                        servletConfigParams, servletContextParams, null));

        assertEquals(
                "Unexpected value for production mode, should be the same as in servlet context parameters",
                servletConfigProductionModeValue, config.isProductionMode());
        assertEquals(
                "Unexpected value for heartbeat interval, should be the same as in servlet context parameters",
                servletConfigHeartbeatIntervalValue,
                config.getHeartbeatInterval());
    }

    @Test
    public void should_not_SetBowerMode_when_NoBowerFrontendFolder() throws Exception {
        DeploymentConfiguration config = createConfig(emptyMap(), null);
        assertFalse(config.isBowerMode());
    }

    @Test
    public void should_SetBowerMode_when_BowerFrontendFolder() throws Exception {
        FileUtils.forceMkdir(new File(getBaseDir(), "src/main/webapp/frontend"));
        DeploymentConfiguration config = createConfig(emptyMap(), null);
        assertTrue(config.isBowerMode());
    }

    @Test
    public void should_not_SetBowerMode_when_BowerFrontendFolderAndNpmFrontendFolder() throws Exception {
        FileUtils.forceMkdir(new File(getBaseDir(), "src/main/webapp/frontend"));
        FileUtils.forceMkdir(new File(getBaseDir(), "frontend"));
        DeploymentConfiguration config = createConfig(emptyMap(), null);
        assertFalse(config.isBowerMode());
    }

    @Test
    public void should_not_SetBowerMode_when_BowerFrontendFolderAndPackageAndWebpack() throws Exception {
        FileUtils.forceMkdir(new File(getBaseDir(), "src/main/webapp/frontend"));
        new File(getBaseDir(), PACKAGE_JSON).createNewFile();
        new File(getBaseDir(), FrontendUtils.WEBPACK_CONFIG).createNewFile();
        DeploymentConfiguration config = createConfig(emptyMap(), null);
        assertFalse(config.isBowerMode());
    }

    @Test
    public void should_not_SetBowerMode_when_ProdModeAndNoBundleEs6() throws Exception {
        Map<String, String> pars = new HashMap<>();
        pars.put(SERVLET_PARAMETER_PRODUCTION_MODE, Boolean.TRUE.toString());
        DeploymentConfiguration config = createConfig(pars, null);
        assertFalse(config.isBowerMode());
    }

    @Test
    public void should_SetBowerMode_when_ProdModeAndBundleEs6() throws Exception {
        Map<String, String> pars = new HashMap<>();
        pars.put(SERVLET_PARAMETER_PRODUCTION_MODE, Boolean.TRUE.toString());
        DeploymentConfiguration config = createConfig(pars, new File(".").toURI().toURL());
        assertTrue(config.isBowerMode());
    }

    private DeploymentConfiguration createConfig(Map<String, String> map, URL url) throws Exception {
        return DeploymentConfigurationFactory
                .createDeploymentConfiguration(VaadinServlet.class, createServletConfigMock(
                        map, emptyMap(), url));
    }

    private ServletConfig createServletConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters, URL url) throws Exception {
        ServletContext contextMock = mock(ServletContext.class);
        expect(contextMock.getInitParameterNames())
                .andAnswer(() -> Collections
                        .enumeration(servletContextParameters.keySet()))
                .anyTimes();
        Capture<String> initParameterNameCapture = EasyMock.newCapture();
        expect(contextMock.getInitParameter(capture(initParameterNameCapture)))
                .andAnswer(() -> servletContextParameters
                        .get(initParameterNameCapture.getValue()))
                .anyTimes();

        Capture<String> resourceCapture = EasyMock.newCapture();
        expect(contextMock.getResource(capture(resourceCapture)))
                .andReturn(url).anyTimes();


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
