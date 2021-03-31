package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.HandlerHelper.RequestType;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class HandlerHelperTest {
    @Test
    public void isFrameworkInternalRequest_validType_nullPathInfo() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(null);
        Mockito.when(request.getParameter("v-r"))
                .thenReturn(RequestType.INIT.getIdentifier());
        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_emptyPathinfo() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("");
        Mockito.when(request.getParameter("v-r"))
                .thenReturn(RequestType.INIT.getIdentifier());
        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_validType_slashPathinfo() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/");
        Mockito.when(request.getParameter("v-r"))
                .thenReturn(RequestType.INIT.getIdentifier());
        Assert.assertTrue(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_invalidType() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("v-r")).thenReturn("unknown");
        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_noType() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("v-r")).thenReturn(null);
        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_wrongPath() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("hello");
        Mockito.when(request.getParameter("v-r"))
                .thenReturn(RequestType.INIT.getIdentifier());
        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

    @Test
    public void isFrameworkInternalRequest_staticResourceRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/VAADIN/foo.css");
        Assert.assertFalse(HandlerHelper.isFrameworkInternalRequest(request));
    }

}
