/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import java.util.Properties;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockUIContainingServlet.ServletInUI;

public class VaadinServletConfigurationTest {

    @Test
    public void testEnclosingUIClass() throws Exception {
        ServletInUI servlet = new MockUIContainingServlet.ServletInUI();
        servlet.init(new MockServletConfig());

        Class<? extends UI> uiClass = BootstrapHandler
                .getUIClass(new VaadinServletRequest(
                        EasyMock.createMock(HttpServletRequest.class),
                        servlet.getService()));
        Assert.assertEquals(MockUIContainingServlet.class, uiClass);
    }

    @Test
    public void testValuesFromAnnotation() throws ServletException {
        Properties servletInitParams = new Properties();
        servletInitParams.setProperty(Constants.USE_ORIGINAL_FRONTEND_RESOURCES,
                Boolean.TRUE.toString());
        servletInitParams.setProperty(
                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());

        TestServlet servlet = new TestServlet();
        servlet.init(new MockServletConfig(servletInitParams));

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
                Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                Boolean.toString(expectedBoolean));
        servletInitParams.setProperty(
                Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
                Integer.toString(expectedInt));
        servletInitParams.setProperty(Constants.USE_ORIGINAL_FRONTEND_RESOURCES,
                Boolean.TRUE.toString());
        servletInitParams.setProperty(
                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());

        TestServlet servlet = new TestServlet();
        servlet.init(new MockServletConfig(servletInitParams));

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
