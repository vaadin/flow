package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.theme.AbstractTheme;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * Test class for testing es5 and es6 resolution by browser capability. This is
 * valid only for bower mode where we need to decide ourselves.
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

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();

        File token = tempFolder.newFile();
        FileUtils.write(token, "{}", StandardCharsets.UTF_8);

        servlet = new VaadinServlet();
        Properties properties = new Properties();
        properties.put(FrontendUtils.PARAM_TOKEN_FILE, token.getPath());
        servlet.init(new MockServletConfig(properties));
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void resolveNullThrows() {
        try {
            service.resolveResource(null, mocks.getBrowser());
            Assert.fail("null should not resolve");
        } catch (NullPointerException e) {
            Assert.assertEquals("Url cannot be null", e.getMessage());
        }
    }

    @Test
    public void resolveResource() {
        Assert.assertEquals("",
                service.resolveResource("", mocks.getBrowser()));
        Assert.assertEquals("foo",
                service.resolveResource("foo", mocks.getBrowser()));
        Assert.assertEquals("/frontend/foo",
                service.resolveResource("frontend://foo", mocks.getBrowser()));
        Assert.assertEquals("/foo",
                service.resolveResource("context://foo", mocks.getBrowser()));
    }

    @Test
    public void resolveResourceNPM_production() {
        mocks.setProductionMode(true);

        Assert.assertEquals("",
                service.resolveResource("", mocks.getBrowser()));
        Assert.assertEquals("foo",
                service.resolveResource("foo", mocks.getBrowser()));
        Assert.assertEquals("/frontend/foo",
                service.resolveResource("frontend://foo", mocks.getBrowser()));
        Assert.assertEquals("/foo",
                service.resolveResource("context://foo", mocks.getBrowser()));
    }

    private void testGetResourceAndGetResourceAsStream(
            String expectedServletContextResource, String untranslatedUrl,
            WebBrowser browser, AbstractTheme theme) throws IOException {

        if (expectedServletContextResource == null) {
            Assert.assertNull(
                    service.getResource(untranslatedUrl, browser, theme));
            Assert.assertNull(service.getResourceAsStream(untranslatedUrl,
                    browser, theme));
        } else {
            URL expectedUrl = new URL(
                    "file://" + expectedServletContextResource);
            Assert.assertEquals(expectedUrl,
                    service.getResource(untranslatedUrl, browser, theme));
            String contents = IOUtils.toString(service.getResourceAsStream(
                    untranslatedUrl, browser, theme), StandardCharsets.UTF_8);
            Assert.assertEquals("This is " + expectedServletContextResource,
                    contents);
        }
    }

    @Test
    public void getResourceNoTheme() throws IOException {
        WebBrowser browser = mocks.getBrowser();
        mocks.getServlet().addServletContextResource("/frontend/foo.txt");
        mocks.getServlet().addWebJarResource("paper-slider/paper-slider.html");

        testGetResourceAndGetResourceAsStream("/frontend/foo.txt",
                "/frontend/foo.txt", browser, null);
        testGetResourceAndGetResourceAsStream("/frontend/foo.txt",
                "frontend://foo.txt", browser, null);
        testGetResourceAndGetResourceAsStream(null, "frontend://bar.txt",
                browser, null);

        testGetResourceAndGetResourceAsStream(
                "/webjars/paper-slider/paper-slider.html",
                "/frontend/bower_components/paper-slider/paper-slider.html",
                browser, null);
        testGetResourceAndGetResourceAsStream(
                "/webjars/paper-slider/paper-slider.html",
                "frontend://bower_components/paper-slider/paper-slider.html",
                browser, null);
    }

    @Test
    public void getResourceTheme() throws IOException {
        WebBrowser browser = mocks.getBrowser();
        TestTheme theme = new TestTheme();

        mocks.getServlet()
                .addServletContextResource("/frontend/raw/raw-only.txt");
        mocks.getServlet().addServletContextResource(
                "/frontend/raw/has-theme-variant.txt");
        mocks.getServlet().addServletContextResource(
                "/frontend/theme/has-theme-variant.txt");
        mocks.getServlet()
                .addServletContextResource("/frontend/theme/theme-only.txt");

        mocks.getServlet().addWebJarResource("vaadin-button/raw/raw-only.txt");
        mocks.getServlet()
                .addWebJarResource("vaadin-button/raw/has-theme-variant.txt");
        mocks.getServlet()
                .addWebJarResource("vaadin-button/theme/has-theme-variant.txt");
        mocks.getServlet()
                .addWebJarResource("vaadin-button/theme/theme-only.txt");

        // Only raw version
        testGetResourceAndGetResourceAsStream("/frontend/raw/raw-only.txt",
                "frontend://raw/raw-only.txt", browser, theme);
        testGetResourceAndGetResourceAsStream(
                "/webjars/vaadin-button/raw/raw-only.txt",
                "frontend://bower_components/vaadin-button/raw/raw-only.txt",
                browser, theme);
        // Only themed version
        testGetResourceAndGetResourceAsStream("/frontend/theme/theme-only.txt",
                "frontend://raw/theme-only.txt", browser, theme);
        testGetResourceAndGetResourceAsStream(
                "/webjars/vaadin-button/theme/theme-only.txt",
                "frontend://bower_components/vaadin-button/raw/theme-only.txt",
                browser, theme);

        // Raw and themed version
        testGetResourceAndGetResourceAsStream(
                "/frontend/theme/has-theme-variant.txt",
                "frontend://raw/has-theme-variant.txt", browser, theme);
        testGetResourceAndGetResourceAsStream(
                "/webjars/vaadin-button/theme/has-theme-variant.txt",
                "frontend://bower_components/vaadin-button/raw/has-theme-variant.txt",
                browser, theme);
        testGetResourceAndGetResourceAsStream(
                "/frontend/theme/has-theme-variant.txt",
                "frontend://theme/has-theme-variant.txt", browser, null);
        testGetResourceAndGetResourceAsStream(
                "/webjars/vaadin-button/theme/has-theme-variant.txt",
                "frontend://bower_components/vaadin-button/theme/has-theme-variant.txt",
                browser, theme);
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
