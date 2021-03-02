package com.vaadin.flow.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.communication.PushRequestHandler;

/**
 * Makes sure that a custom vaadin service that is not vaadin servlet service can be used in when desired.
 *
 */
public class CustomVaadinServiceImplementationTest {
    @Test
    public void StaticFileServer_Constructor_uses_VaadinService()
            throws NoSuchMethodException, SecurityException {

        Assert.assertNotNull(
                StaticFileServer.class.getConstructor(VaadinService.class));
    }

    @Test
    public void VaadinServlet_uses_VaadinService_getService()
            throws NoSuchMethodException, SecurityException {

        Assert.assertNotNull(VaadinServlet.class.getDeclaredMethod(
                "createStaticFileHandler", VaadinService.class));

        Method mcreateVaadinResponse = VaadinServlet.class.getDeclaredMethod(
                "createVaadinResponse", HttpServletResponse.class);
        Assert.assertNotNull(mcreateVaadinResponse);
        Assert.assertEquals(VaadinResponse.class,
                mcreateVaadinResponse.getReturnType());

        Method mgetService = VaadinServlet.class
                .getDeclaredMethod("getService");
        Assert.assertNotNull(mgetService);
        Assert.assertEquals(VaadinService.class, mgetService.getReturnType());
    }

    @Test
    public void VaadinServletRequest_uses_VaadinService_getService()
            throws NoSuchMethodException, SecurityException {

        Assert.assertNotNull(VaadinServletRequest.class
                .getConstructor(HttpServletRequest.class, VaadinService.class));

        Method mgetService = VaadinServletRequest.class
                .getDeclaredMethod("getService");
        Assert.assertNotNull(mgetService);
        Assert.assertEquals(VaadinService.class, mgetService.getReturnType());

    }

    @Test
    public void VaadinServletResponse_uses_VaadinService_getService()
            throws NoSuchMethodException, SecurityException {

        Assert.assertNotNull(VaadinServletResponse.class.getConstructor(
                HttpServletResponse.class, VaadinService.class));

        Method mgetService = VaadinServletResponse.class
                .getDeclaredMethod("getService");
        Assert.assertNotNull(mgetService);
        Assert.assertEquals(VaadinService.class, mgetService.getReturnType());
    }

    @Test
    public void PushRequestHandler_uses_VaadinService_createPushHandler()
            throws NoSuchMethodException, SecurityException {

        Method mgetService = PushRequestHandler.class
                .getDeclaredMethod("createPushHandler", VaadinService.class);
        Assert.assertNotNull(mgetService);
    }

    @Test
    public void VaadinResponse_sendError() throws NoSuchMethodException,
            SecurityException, ServiceException, IOException {
        VaadinService vs = new MockVaadinService();

        VaadinHttpServletResponseI response = Mockito
                .mock(VaadinHttpServletResponseI.class);

        Mockito.doThrow(new RuntimeException(
                "Please check that you really nead more than a HttpServletResponse"))
                .when(response)
                .sendError(Mockito.anyInt(), Mockito.anyString());

        vs.handleSessionExpired(Mockito.mock(VaadinRequest.class), response);

    }

    abstract class AbstractMockVaadinService extends VaadinService {

        private static final long serialVersionUID = 1L;
        public static final String TEST_SESSION_EXPIRED_URL = "TestSessionExpiredURL";

        @Override
        protected RouteRegistry getRouteRegistry() {
            // ignore
            return null;
        }

        @Override
        protected PwaRegistry getPwaRegistry() {
            // ignore
            return null;
        }

        @Override
        public String getContextRootRelativePath(VaadinRequest request) {
            // ignore
            return null;
        }

        @Override
        public String getMimeType(String resourceName) {
            // ignore
            return null;
        }

        @Override
        protected boolean requestCanCreateSession(VaadinRequest request) {
            // ignore
            return false;
        }

        @Override
        public String getServiceName() {
            // ignore
            return null;
        }

        @Override
        public String getMainDivId(VaadinSession session,
                VaadinRequest request) {
            // ignore
            return null;
        }

        @Override
        public URL getStaticResource(String url) {
            // ignore
            return null;
        }

        @Override
        public URL getResource(String url) {
            // ignore
            return null;
        }

        @Override
        public InputStream getResourceAsStream(String url) {
            // ignore
            return null;
        }

        @Override
        public String resolveResource(String url) {
            // ignore
            return null;
        }

        @Override
        protected VaadinContext constructVaadinContext() {
            // ignore
            return null;
        }

    }

    class MockVaadinService extends AbstractMockVaadinService {

        private static final long serialVersionUID = 1L;

        @Override
        public Iterable<RequestHandler> getRequestHandlers() {

            return new ArrayList<>();
        }

        @Override
        public SystemMessages getSystemMessages(Locale locale,
                VaadinRequest request) {

            SystemMessages systemMessages = Mockito.mock(SystemMessages.class);
            Mockito.when(systemMessages.getSessionExpiredURL())
                    .thenReturn(TEST_SESSION_EXPIRED_URL);
            return systemMessages;
        }
    }

    interface VaadinHttpServletResponseI
            extends VaadinResponse, HttpServletResponse {

    }
}
