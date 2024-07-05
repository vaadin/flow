/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import java.util.Properties;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockUIContainingServlet.ServletInUI;

public class VaadinServletConfigurationTest {

    @Test
    public void testEnclosingUIClass() throws Exception {
        Properties servletInitParams = new Properties();
        servletInitParams.setProperty(
                InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());

        ServletInUI servlet = new MockUIContainingServlet.ServletInUI();
        servlet.init(createServletConfig(servletInitParams));

        Class<? extends UI> uiClass = BootstrapHandler
                .getUIClass(new VaadinServletRequest(
                        EasyMock.createMock(HttpServletRequest.class),
                        servlet.getService()));
        Assert.assertEquals(MockUIContainingServlet.class, uiClass);
    }

    @Test
    public void testValuesFromAnnotation() throws ServletException {
        Properties servletInitParams = new Properties();
        servletInitParams.setProperty(
                InitParameters.USE_ORIGINAL_FRONTEND_RESOURCES,
                Boolean.TRUE.toString());
        servletInitParams.setProperty(
                InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());

        TestServlet servlet = new TestServlet();
        servlet.init(createServletConfig(servletInitParams));

        DeploymentConfiguration configuration = servlet.getService()
                .getDeploymentConfiguration();

        Assert.assertEquals(true, configuration.isProductionMode());
        Assert.assertEquals(true, configuration.isCloseIdleSessions());
        Assert.assertEquals(1234, configuration.getHeartbeatInterval());

        Class<? extends UI> uiClass = BootstrapHandler
                .getUIClass(new VaadinServletRequest(
                        EasyMock.createMock(HttpServletRequest.class),
                        servlet.getService()));
        Assert.assertEquals(MockUIContainingServlet.class, uiClass);
    }

    @Test
    public void testValuesOverriddenForServlet() throws ServletException {
        final boolean expectedBoolean = false;
        final int expectedInt = 1111;

        Properties servletInitParams = new Properties();
        servletInitParams.setProperty(
                InitParameters.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                Boolean.toString(expectedBoolean));
        servletInitParams.setProperty(
                InitParameters.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(expectedInt));
        servletInitParams.setProperty(
                InitParameters.USE_ORIGINAL_FRONTEND_RESOURCES,
                Boolean.TRUE.toString());
        servletInitParams.setProperty(
                InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());

        TestServlet servlet = new TestServlet();
        servlet.init(createServletConfig(servletInitParams));

        DeploymentConfiguration configuration = servlet.getService()
                .getDeploymentConfiguration();

        // Values from servlet init params take precedence
        Assert.assertEquals(expectedBoolean,
                configuration.isSendUrlsAsParameters());
        Assert.assertEquals(expectedInt, configuration.getHeartbeatInterval());

        // Other params are as defined in the annotation
        Assert.assertEquals(true, configuration.isCloseIdleSessions());

        Class<? extends UI> uiClass = BootstrapHandler
                .getUIClass(new VaadinServletRequest(
                        EasyMock.createMock(HttpServletRequest.class),
                        servlet.getService()));
        Assert.assertEquals(MockUIContainingServlet.class, uiClass);
    }

    private MockServletConfig createServletConfig(Properties properties) {
        MockServletConfig config = new MockServletConfig(properties);
        ServletContext servletContext = config.getServletContext();
        Lookup lookup = Mockito.mock(Lookup.class);
        servletContext.setAttribute(Lookup.class.getName(), lookup);

        ResourceProvider provider = Mockito.mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(provider);
        return config;
    }

}

@VaadinServletConfiguration(productionMode = true, ui = MockUIContainingServlet.class, closeIdleSessions = true, heartbeatInterval = 1234)
class TestServlet extends VaadinServlet {

}

@VaadinServletConfiguration(productionMode = true, ui = MockUIContainingServlet.class)
class LegacyPropertyWarningTestServlet extends VaadinServlet {

}

@VaadinServletConfiguration(productionMode = true, ui = MockUIContainingServlet.class)
class LegacyPropertyEnabledTestServlet extends VaadinServlet {

}
