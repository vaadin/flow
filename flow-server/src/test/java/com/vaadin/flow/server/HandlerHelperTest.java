package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.HandlerHelper.RequestType;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class HandlerHelperTest {

    private HttpServletRequest createRequest(String pathInfo,
            RequestType type) {
        return createRequest(pathInfo,
                type == null ? null : type.getIdentifier());
    }

    private HttpServletRequest createRequest(String pathInfo,
            String typeString) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        if ("".equals(pathInfo)) {
            pathInfo = null;
        }

        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getParameter("v-r")).thenReturn(typeString);
        return request;
    }

    @Test
    public void isFrameworkInternalRequest_validType_nullPathInfo() {
        HttpServletRequest request = createRequest(null, RequestType.INIT);

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_emptyPathinfo() {
        HttpServletRequest request = createRequest("", RequestType.INIT);

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_slashPathinfo() {
        // This is how requests to /vaadinServlet/ are interpreted
        HttpServletRequest request = createRequest("/", RequestType.INIT);

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
    }

    @Test
    public void isFrameworkInternalRequest_unknownType() {
        HttpServletRequest request = createRequest(null, "unknown");

        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));

    }

    @Test
    public void isFrameworkInternalRequest_noType() {
        HttpServletRequest request = createRequest(null, (RequestType) null);

        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));

    }

    @Test
    public void isFrameworkInternalRequest_validType_withPath() {
        HttpServletRequest request = createRequest("/hello", RequestType.INIT);

        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/*", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo", request));
        Assert.assertFalse(
                HandlerHelper.isFrameworkInternalRequest("/foo/*", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/hello", request));
        Assert.assertTrue(
                HandlerHelper.isFrameworkInternalRequest("/hello/*", request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_servletRoot() {
        VaadinRequest request = createVaadinRequest("", "/*", RequestType.INIT);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_servletRoot_noType() {
        VaadinRequest request = createVaadinRequest("", "/*", null);

        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_pathInsideServlet() {
        VaadinRequest request = createVaadinRequest("/foo", "/*",
                RequestType.INIT);

        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_pathInsideServlet_noType() {
        VaadinRequest request = createVaadinRequest("/foo", "/*", null);

        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_nonRootServlet() {
        VaadinRequest request = createVaadinRequest("", "/myservlet/",
                RequestType.INIT);

        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_vaadinRequest_nonRootServlet_pathInsideServlet() {
        VaadinRequest request = createVaadinRequest("/hello", "/myservlet",
                null);

        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    private VaadinRequest createVaadinRequest(String requestPath,
            String servletPath, RequestType type) {
        HttpServletRequest servletRequest = createRequest(requestPath, type);
        if (servletPath.equals("/*")) {
            // This is what the spec says HttpServletRequest#getServletPath
            servletPath = "";
        }
        Mockito.when(servletRequest.getServletPath()).thenReturn(servletPath);
        return new VaadinServletRequest(servletRequest,
                Mockito.mock(VaadinServletService.class));
    }
}
