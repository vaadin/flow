package com.vaadin.flow.server;

import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.theme.AbstractTheme;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Test class for testing es5 and es6 resolution by browser capability.
 * This is valid only for bower mode where we need to decide ourselves.
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
    private final String[] es5es6 = new String[] { "es5", "es6" };

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
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
    public void resolveResource_production() {
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);
        mocks.setProductionMode(true);

        Assert.assertEquals("",
                service.resolveResource("", mocks.getBrowser()));
        Assert.assertEquals("foo",
                service.resolveResource("foo", mocks.getBrowser()));
        Assert.assertEquals("/frontend-es6/foo",
                service.resolveResource("frontend://foo", mocks.getBrowser()));
        Assert.assertEquals("/foo",
                service.resolveResource("context://foo", mocks.getBrowser()));

        mocks.setBrowserEs6(false);

        Assert.assertEquals("",
                service.resolveResource("", mocks.getBrowser()));
        Assert.assertEquals("foo",
                service.resolveResource("foo", mocks.getBrowser()));
        Assert.assertEquals("/frontend-es5/foo",
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

        mocks.setBrowserEs6(false);

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

    // Theme resource is not handled from servlet in NPM
    @Test
    public void getResourceNoTheme_production() throws IOException {
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);

        mocks.getServlet().addServletContextResource("/frontend-es6/foo.txt");
        mocks.getServlet().addServletContextResource("/frontend-es5/foo.txt");

        mocks.setProductionMode(true);
        WebBrowser browser = mocks.getBrowser();

        testGetResourceAndGetResourceAsStream(null, "/frontend/foo.txt",
                browser, null);
        testGetResourceAndGetResourceAsStream("/frontend-es6/foo.txt",
                "frontend://foo.txt", browser, null);
        testGetResourceAndGetResourceAsStream(null, "/frontend/bar.txt",
                browser, null);

        mocks.setBrowserEs6(false);
        testGetResourceAndGetResourceAsStream(null, "/frontend/foo.txt",
                browser, null);
        testGetResourceAndGetResourceAsStream("/frontend-es5/foo.txt",
                "frontend://foo.txt", browser, null);
        testGetResourceAndGetResourceAsStream(null, "/frontend/bar.txt",
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

    // NPM theme is not handled in servlet service.
    @Test
    public void getResourceTheme_production() throws IOException {
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);

        mocks.setProductionMode(true);
        WebBrowser browser = mocks.getBrowser();
        TestTheme theme = new TestTheme();
        for (String es : es5es6) {
            String frontendFolder = "/frontend-" + es;
            mocks.getServlet().addServletContextResource(
                    frontendFolder + "/raw/raw-only.txt");
            mocks.getServlet().addServletContextResource(
                    frontendFolder + "/raw/has-theme-variant.txt");
            mocks.getServlet().addServletContextResource(
                    frontendFolder + "/theme/has-theme-variant.txt");
            mocks.getServlet().addServletContextResource(
                    frontendFolder + "/theme/theme-only.txt");
        }

        for (String es : es5es6) {
            mocks.setBrowserEs6("es6".equals(es));
            String expectedFrontend = "file:///frontend-" + es;
            // Only raw version
            Assert.assertEquals(new URL(expectedFrontend + "/raw/raw-only.txt"),
                    service.getResource("frontend://raw/raw-only.txt", browser,
                            theme));

            // Only themed version
            Assert.assertEquals(
                    new URL(expectedFrontend + "/theme/theme-only.txt"),
                    service.getResource("frontend://raw/theme-only.txt",
                            browser, theme));

            // Raw and themed version
            Assert.assertEquals(
                    new URL(expectedFrontend + "/theme/has-theme-variant.txt"),
                    service.getResource("frontend://raw/has-theme-variant.txt",
                            browser, theme));
            Assert.assertEquals(
                    new URL(expectedFrontend + "/theme/has-theme-variant.txt"),
                    service.getResource(
                            "frontend://theme/has-theme-variant.txt", browser,
                            null)); // No theme -> raw version
        }
    }


}
