package com.vaadin.flow.server;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.tests.util.MockDeploymentConfiguration;

public class UnsupportedBrowserHandlerTest {

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private UnsupportedBrowserHandler handler = new UnsupportedBrowserHandler();
    private ArgumentCaptor<String> pageCapture = ArgumentCaptor
            .forClass(String.class);
    private PrintWriter writer;
    private MockDeploymentConfiguration configuration;

    private void initMocks(boolean forceReloadCookie,
            boolean isTooOldToFunctionProperly) throws IOException {

        configuration = new MockDeploymentConfiguration();

        request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getHeader("Cookie"))
                .thenReturn(forceReloadCookie
                        ? UnsupportedBrowserHandler.FORCE_LOAD_COOKIE
                        : null);

        writer = Mockito.mock(PrintWriter.class);

        response = Mockito.mock(VaadinServletResponse.class);
        Mockito.when(response.getWriter()).thenReturn(writer);

        WebBrowser webBrowser = Mockito.mock(WebBrowser.class);
        Mockito.when(webBrowser.isTooOldToFunctionProperly())
                .thenReturn(isTooOldToFunctionProperly);

        session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.getBrowser()).thenReturn(webBrowser);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);

        VaadinSession.setCurrent(session);
    }

    @Test
    public void testUnsupportedBrowserHandler_validBrowser_doesntHandleRequest()
            throws IOException {
        initMocks(false, false);

        Assert.assertFalse("Should not handle the request",
                handler.synchronizedHandleRequest(session, request, response));
        Mockito.verify(writer, Mockito.never()).write(Mockito.anyString());
    }

    @Test
    public void testUnsupportedBrowserHandler_tooOldBrowser_returnsUnsupportedBrowserPage()
            throws IOException {
        initMocks(false, true);

        Assert.assertTrue("Request should have been handled",
                handler.synchronizedHandleRequest(session, request, response));

        Mockito.verify(writer).write(pageCapture.capture());

        Assert.assertTrue("Unsupported browser page not used",
                pageCapture.getValue().contains(
                        "I'm sorry, but your browser is not supported"));
    }

    @Test
    public void testUnsupportedBrowserHandler_tooOldBrowserWithForceReloadCookie_doesntHandleRequest()
            throws IOException {
        initMocks(true, true);

        Assert.assertFalse("Should not handle the request",
                handler.synchronizedHandleRequest(session, request, response));
        Mockito.verify(writer, Mockito.never()).write(Mockito.anyString());
    }

    @Test
    public void testUnsupportedBrowserHandler_validBrowserWithForceReloadCookie_doesntHandleRequest()
            throws IOException {
        initMocks(true, false);

        Assert.assertFalse("Should not handle the request",
                handler.synchronizedHandleRequest(session, request, response));
        Mockito.verify(writer, Mockito.never()).write(Mockito.anyString());
    }

    @After
    public void tearDown() {
        VaadinSession.setCurrent(null);
    }
}
