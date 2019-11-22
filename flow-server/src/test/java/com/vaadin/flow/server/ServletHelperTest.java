package com.vaadin.flow.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class ServletHelperTest {
    VaadinServlet servlet;

    @Before
    public void setup() throws ServletException {
        servlet = new VaadinServlet();
        servlet.init(new MockServletConfig());
    }

    @Test
    public void testServletToContextRootRelativePath() throws Exception {
        String location;

        /* SERVLETS */
        // http://dummy.host:8080/contextpath/servlet
        // should return . (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "");
        Assert.assertEquals("./..", location);

        // http://dummy.host:8080/contextpath/servlet/
        // should return ./.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "/");
        Assert.assertEquals("./..", location);

        // http://dummy.host:8080/servlet
        // should return "."
        location = testLocation("http://dummy.host:8080", "", "/servlet", "");
        Assert.assertEquals("./..", location);

        // http://dummy.host/contextpath/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host", "/contextpath", "/servlet",
                "/extra/stuff");
        Assert.assertEquals("./..", location);

        // http://dummy.host/context/path/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /context/path)
        location = testLocation("http://dummy.host", "/context/path",
                "/servlet", "/extra/stuff");
        Assert.assertEquals("./..", location);

    }

    private String testLocation(String base, String contextPath,
            String servletPath, String pathInfo) throws Exception {

        HttpServletRequest request = createNonIncludeRequest(base, contextPath,
                servletPath, pathInfo);
        // Set request into replay mode
        replay(request);

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
        expect(request.getAttribute("javax.servlet.include.context_path"))
                .andReturn(null).anyTimes();
        expect(request.getAttribute("javax.servlet.include.servlet_path"))
                .andReturn(null).anyTimes();

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
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.isSecure())
                .andReturn(url.getProtocol().equalsIgnoreCase("https"))
                .anyTimes();
        expect(request.getServerName()).andReturn(url.getHost()).anyTimes();
        expect(request.getServerPort()).andReturn(url.getPort()).anyTimes();
        expect(request.getRequestURI()).andReturn(url.getPath()).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getPathInfo()).andReturn(pathInfo).anyTimes();
        expect(request.getServletPath()).andReturn(servletPath).anyTimes();

        return request;
    }

}
