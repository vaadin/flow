/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * Test class for testing es6 resolution by browser capability. This is valid
 * only for bower mode where we need to decide ourselves.
 */
public class VaadinServletServiceTest {

    private final class TestTheme implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "/raw/";
        }

        @Override
        public String getThemeUrl() {
            return "/theme/";
        }
    }

    private MockServletServiceSessionSetup mocks;
    private TestVaadinServletService service;
    private VaadinServlet servlet;

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();

        servlet = mocks.getServlet();
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void resolveNullThrows() {
        try {
            service.resolveResource(null);
            Assert.fail("null should not resolve");
        } catch (NullPointerException e) {
            Assert.assertEquals("Url cannot be null", e.getMessage());
        }
    }

    @Test
    public void resolveResource() {
        Assert.assertEquals("", service.resolveResource(""));
        Assert.assertEquals("foo", service.resolveResource("foo"));
        Assert.assertEquals("/foo", service.resolveResource("context://foo"));
    }

    @Test
    public void resolveResourceNPM_production() {
        mocks.setProductionMode(true);

        Assert.assertEquals("", service.resolveResource(""));
        Assert.assertEquals("foo", service.resolveResource("foo"));
        Assert.assertEquals("/foo", service.resolveResource("context://foo"));
    }

    @Test
    public void getContextRootRelativePath_useVariousContextPathAndServletPathsAndPathInfo()
            throws Exception {
        String location;

        /* SERVLETS */
        // http://dummy.host:8080/contextpath/servlet
        // should return . (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "");
        Assert.assertEquals("./../", location);

        // http://dummy.host:8080/contextpath/servlet/
        // should return ./.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "/");
        Assert.assertEquals("./../", location);

        // http://dummy.host:8080/servlet
        // should return "."
        location = testLocation("http://dummy.host:8080", "", "/servlet", "");
        Assert.assertEquals("./../", location);

        // http://dummy.host/contextpath/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host", "/contextpath", "/servlet",
                "/extra/stuff");
        Assert.assertEquals("./../", location);

        // http://dummy.host/context/path/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /context/path)
        location = testLocation("http://dummy.host", "/context/path",
                "/servlet", "/extra/stuff");
        Assert.assertEquals("./../", location);

    }

    @Test
    public void should_report_flow_bootstrapHandler() {
        mocks.getDeploymentConfiguration().useDeprecatedV14Bootstrapping(true);

        Assert.assertTrue(UsageStatistics.getEntries()
                .anyMatch(e -> Constants.STATISTIC_FLOW_BOOTSTRAPHANDLER
                        .equals(e.getName())));
    }

    @Test
    public void init_classLoaderIsSetUsingServletContext()
            throws ServiceException {
        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(servlet.getServletContext()).thenReturn(context);

        ClassLoader loader = Mockito.mock(ClassLoader.class);
        Mockito.when(context.getClassLoader()).thenReturn(loader);

        VaadinServletService service = new VaadinServletService(servlet,
                mocks.getDeploymentConfiguration()) {
            @Override
            protected Instantiator createInstantiator()
                    throws ServiceException {
                return Mockito.mock(Instantiator.class);
            }

            @Override
            protected List<RequestHandler> createRequestHandlers()
                    throws ServiceException {
                return Collections.emptyList();
            }
        };

        service.init();

        Assert.assertSame(loader, service.getClassLoader());
    }

    private String testLocation(String base, String contextPath,
            String servletPath, String pathInfo) throws Exception {

        HttpServletRequest request = createNonIncludeRequest(base, contextPath,
                servletPath, pathInfo);

        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        Mockito.doCallRealMethod().when(service)
                .getContextRootRelativePath(Mockito.any());
        String location = service.getContextRootRelativePath(
                servlet.createVaadinRequest(request));
        return location;
    }

    private HttpServletRequest createNonIncludeRequest(String base,
            String realContextPath, String realServletPath, String pathInfo)
            throws Exception {
        HttpServletRequest request = createRequest(base, realContextPath,
                realServletPath, pathInfo);
        Mockito.when(request.getAttribute("javax.servlet.include.context_path"))
                .thenReturn(null);
        Mockito.when(request.getAttribute("javax.servlet.include.servlet_path"))
                .thenReturn(null);

        return request;
    }

    /**
     * Creates a HttpServletRequest mock using the supplied parameters.
     *
     * @param base
     *            The base url, e.g. http://localhost:8080
     * @param contextPath
     *            The context path where the application is deployed, e.g.
     *            /mycontext
     * @param servletPath
     *            The servlet path to the servlet we are testing, e.g. /myapp
     * @param pathInfo
     *            Any text following the servlet path in the request, not
     *            including query parameters, e.g. /UIDL/
     * @return A mock HttpServletRequest object useful for testing
     * @throws MalformedURLException
     */
    private HttpServletRequest createRequest(String base, String contextPath,
            String servletPath, String pathInfo) throws MalformedURLException {
        URL url = new URL(base + contextPath + pathInfo);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.isSecure())
                .thenReturn(url.getProtocol().equalsIgnoreCase("https"));
        Mockito.when(request.getServerName()).thenReturn(url.getHost());
        Mockito.when(request.getServerPort()).thenReturn(url.getPort());
        Mockito.when(request.getRequestURI()).thenReturn(url.getPath());
        Mockito.when(request.getContextPath()).thenReturn(contextPath);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getServletPath()).thenReturn(servletPath);

        return request;
    }

}
