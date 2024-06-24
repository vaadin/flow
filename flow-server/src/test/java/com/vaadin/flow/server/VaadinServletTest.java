/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletResponse;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class VaadinServletTest {

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
}
