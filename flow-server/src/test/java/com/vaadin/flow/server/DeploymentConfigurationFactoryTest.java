package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeploymentConfigurationFactoryTest {

    private static class NoSettings extends VaadinServlet {
    }

    private static class TestUI extends UI {
        private static class ServletWithEnclosingUi extends VaadinServlet {
        }
    }

    @VaadinServletConfiguration(productionMode = true, heartbeatInterval = 222)
    private static class VaadinSettings extends VaadinServlet {

    }

    @Test
    public void servletWithEnclosingUI_hasItsNameInConfig()
            throws ServletException {
        Class<TestUI.ServletWithEnclosingUi> servlet = TestUI.ServletWithEnclosingUi.class;

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet,
                        createServletConfigMock(emptyMap(), emptyMap()));

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
            throws ServletException {
        Class<NoSettings> servlet = NoSettings.class;

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet,
                        createServletConfigMock(emptyMap(), emptyMap()));

        Class<?> notUiClass = servlet.getEnclosingClass();
        assertFalse(String.format(
                "Servlet '%s' should not have its enclosing class to be UI subclass, but got: '%s'",
                notUiClass, servlet), UI.class.isAssignableFrom(notUiClass));
        assertEquals(String.format(
                "Expected DeploymentConfiguration for servlet '%s' to have its enclosing UI class",
                servlet), UI.class.getName(), config.getUIClassName());
    }

    @Test
    public void vaadinServletConfigurationRead() throws ServletException {
        Class<VaadinSettings> servlet = VaadinSettings.class;

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet,
                        createServletConfigMock(emptyMap(), emptyMap()));

        assertTrue(String.format(
                "Unexpected value for production mode, check '%s' class annotation",
                servlet), config.isProductionMode());
        assertEquals(String.format(
                "Unexpected value for heartbeat interval, check '%s' class annotation",
                servlet), 222, config.getHeartbeatInterval());
    }

    @Test
    public void servletConfigParametersOverrideVaadinParameters()
            throws ServletException {
        Class<VaadinSettings> servlet = VaadinSettings.class;

        boolean overridingProductionModeValue = false;
        int overridingHeartbeatIntervalValue = 444;

        Map<String, String> servletConfigParams = new HashMap<>();
        servletConfigParams.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(overridingProductionModeValue));
        servletConfigParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(overridingHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createServletConfigMock(
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
            throws ServletException {
        Class<VaadinSettings> servlet = VaadinSettings.class;

        boolean overridingProductionModeValue = false;
        int overridingHeartbeatIntervalValue = 444;

        Map<String, String> servletContextParams = new HashMap<>();
        servletContextParams.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(overridingProductionModeValue));
        servletContextParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(overridingHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createServletConfigMock(
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
            throws ServletException {
        Class<NoSettings> servlet = NoSettings.class;

        boolean servletConfigProductionModeValue = true;
        int servletConfigHeartbeatIntervalValue = 333;

        Map<String, String> servletConfigParams = new HashMap<>();
        servletConfigParams.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(servletConfigProductionModeValue));
        servletConfigParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(servletConfigHeartbeatIntervalValue));

        boolean servletContextProductionModeValue = false;
        int servletContextHeartbeatIntervalValue = 444;

        Map<String, String> servletContextParams = new HashMap<>();
        servletContextParams.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.toString(servletContextProductionModeValue));
        servletContextParams.put(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(servletContextHeartbeatIntervalValue));

        DeploymentConfiguration config = DeploymentConfigurationFactory
                .createDeploymentConfiguration(servlet, createServletConfigMock(
                        servletConfigParams, servletContextParams));

        assertEquals(
                "Unexpected value for production mode, should be the same as in servlet context parameters",
                servletConfigProductionModeValue, config.isProductionMode());
        assertEquals(
                "Unexpected value for heartbeat interval, should be the same as in servlet context parameters",
                servletConfigHeartbeatIntervalValue,
                config.getHeartbeatInterval());
    }

    private ServletConfig createServletConfigMock(
            Map<String, String> servletConfigParameters,
            Map<String, String> servletContextParameters) {
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
