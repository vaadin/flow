/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletResponse;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class VaadinServletTest {

    private MockedStatic<LicenseChecker> licenseChecker;
    private final DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @Before
    public void setup() {
        licenseChecker = Mockito.mockStatic(LicenseChecker.class);
        Mockito.when(configuration.getInitParameters())
                .thenReturn(new Properties());
    }

    @After
    public void cleanup() {
        licenseChecker.close();
    }

    @Test
    public void testGetLastPathParameter() {
        Assert.assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com"));
        Assert.assertEquals(";a",
                VaadinServlet.getLastPathParameter("http://myhost.com;a"));
        Assert.assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com/hello"));
        Assert.assertEquals(";b=c", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;b=c"));
        Assert.assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com/hello/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;a/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;a=1/"));
        Assert.assertEquals(";b", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b"));
        Assert.assertEquals(";b=1", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1"));
        Assert.assertEquals(";b=1,c=2", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1,c=2"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1,c=2/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;a/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;a=1/"));
        Assert.assertEquals(";b", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b"));
        Assert.assertEquals(";b=1", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1"));
        Assert.assertEquals(";b=1,c=2", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1,c=2"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1,c=2/"));
    }

    @Test
    public void nonexistingFrontendFileReturns404() throws Exception {
        MockServletServiceSessionSetup mocks = new MockServletServiceSessionSetup();
        TestVaadinServletResponse response = mocks.createResponse();
        Assert.assertTrue(mocks.getServlet().serveStaticOrWebJarRequest(
                createRequest(mocks, "/frontend/bower_components/foo/foo.html"),
                response));
        Assert.assertEquals(404, response.getErrorCode());
        mocks.cleanup();
    }

    @Test
    public void existingFrontendFileFound() throws Exception {
        MockServletServiceSessionSetup mocks = new MockServletServiceSessionSetup();
        TestVaadinServletResponse response = mocks.createResponse();
        mocks.getServlet().addServletContextResource("/webjars/foo/foo.html");
        Assert.assertTrue(mocks.getServlet().serveStaticOrWebJarRequest(
                createRequest(mocks, "/frontend/bower_components/foo/foo.html"),
                response));
        // A real server would return 200, the mock does not change the status
        // code
        Assert.assertEquals(0, response.getErrorCode());
        mocks.cleanup();
    }

    @Test
    public void checkLicense_devMode_licenseIsChecked()
            throws ServletException {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        triggerLicenseChecking();
        licenseChecker.verify(() -> LicenseChecker.checkLicense("flow",
                Version.getFullVersion(), BuildType.DEVELOPMENT));
    }

    @Test
    public void checkLicense_prodMode_licenseIsNotChecked()
            throws ServletException {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        triggerLicenseChecking();
        licenseChecker.verifyNoInteractions();
    }

    private HttpServletRequest createRequest(
            MockServletServiceSessionSetup mocks, String path) {
        HttpServletRequest httpServletRequest = Mockito
                .mock(HttpServletRequest.class);
        return new VaadinServletRequest(httpServletRequest,
                mocks.getService()) {
            @Override
            public String getPathInfo() {
                return path;
            }

            @Override
            public String getServletPath() {
                return "";
            }

            @Override
            public ServletContext getServletContext() {
                return mocks.getServletContext();
            }
        };
    }

    private void triggerLicenseChecking() throws ServletException {
        VaadinServlet vaadinServlet = new VaadinServletWithConfiguration() {
            @Override
            protected VaadinServletService createServletService() {
                VaadinServletService service = Mockito
                        .mock(VaadinServletService.class);
                Mockito.when(service.getDeploymentConfiguration())
                        .thenReturn(configuration);
                return service;
            }
        };

        ServletConfig config = mockConfig();

        vaadinServlet.init(config);
    }

    private ServletConfig mockConfig() {
        ServletConfig config = Mockito.mock(ServletConfig.class);
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(config.getServletContext()).thenReturn(context);
        return config;
    }

    private class VaadinServletWithConfiguration extends VaadinServlet {
        @Override
        protected DeploymentConfiguration createDeploymentConfiguration() {
            return configuration;
        }
    }
}