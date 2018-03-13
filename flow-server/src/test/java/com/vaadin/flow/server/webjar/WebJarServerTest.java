package com.vaadin.flow.server.webjar;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.lang.reflect.Field;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

public class WebJarServerTest {

    private static final String GRID_DEPENDENCY = "/frontend/bower_components/vaadin-grid/theme/lumo/vaadin-grid-sorter.html";
    private static final String GRID_WEBJAR = "/webjars/vaadin-grid/theme/lumo/vaadin-grid-sorter.html";

    private WebJarServer webJarServer;

//    private ServletContext context;
    private VaadinServlet servlet;

    private String baseUrl = "/.";
    private String upStep = "/..";

    @Before
    public void init() throws Exception {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(configuration.getDevelopmentFrontendPrefix())
                .thenReturn(Constants.FRONTEND_URL_DEV_DEFAULT);

        VaadinServletService servletService = Mockito.mock(VaadinServletService.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        servlet = new VaadinServlet() {
            @Override
            protected VaadinServletService createServletService()
                    throws ServletException, ServiceException {
                return servletService;
            }

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }
        };
        Mockito.when(servletService.getDeploymentConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.areWebJarsEnabled()).thenReturn(true);
        servlet.init(Mockito.mock(ServletConfig.class));
        Field webJarServerField = VaadinServlet.class
                .getDeclaredField("webJarServer");
        webJarServerField.setAccessible(true);
        webJarServer = (WebJarServer) webJarServerField.get(servlet);
        //        webJarServer = new WebJarServer(configuration);
//        context = Mockito.mock(ServletContext.class);

        Mockito.when(servletContext.getResource(GRID_WEBJAR))
                .thenReturn(new URL("http://localhost:8080" + GRID_DEPENDENCY));
    }

    @Test
    public void test_webjar_resolves_without_any_baseurl() throws Exception {
        String value = GRID_DEPENDENCY;

        boolean foundComponent = servlet.getResource(webJarServer.getWebJarResourcePath(value).get()) != null;

        Assert.assertTrue(
                "Expected webJarServer to find fixed component for value: "
                        + value,
                foundComponent);
    }

    @Test
    public void test_webjar_resolves_for_baseurl() throws Exception {
        String value = GRID_DEPENDENCY;

        boolean foundComponent = servlet.getResource(webJarServer.getWebJarResourcePath(baseUrl + value).get()) != null;

        Assert.assertTrue(
                "Expected webJarServer to find fixed component for value: "
                        + value,
                foundComponent);
    }

    @Test
    public void test_webjar_resolves_for_baseurl_and_up_step_combinations()
            throws Exception {
        String value = GRID_DEPENDENCY;

        Assert.assertTrue("No match found for /.." + value,
                servlet.getResource(webJarServer.getWebJarResourcePath(upStep + value).get()) != null);

        Assert.assertTrue("No match found for /./.." + value, servlet.getResource(webJarServer
                .getWebJarResourcePath(baseUrl + upStep + value).get()) != null);

        Assert.assertTrue("No match found for /./../.." + value, servlet.getResource(webJarServer
                .getWebJarResourcePath(baseUrl + upStep + upStep + value).get()) != null);
    }

    @Test
    public void no_match_when_frontend_starts_wrong() throws Exception {
        String value = "/wrong" + GRID_DEPENDENCY;

        Assert.assertFalse("Match found for path starting with '/wrong'",
                webJarServer.getWebJarResourcePath(value).isPresent());

        Assert.assertFalse("Match found for path starting with '/./wrong'",
                webJarServer.getWebJarResourcePath(baseUrl + value).isPresent());
    }

    @Test
    public void no_match_when_start_prefix_not_at_start() throws Exception {
        String value = "/no/match" + baseUrl + GRID_DEPENDENCY;

        Assert.assertFalse("Match found for path starting with '/no/match/.'",
                webJarServer.getWebJarResourcePath(value).isPresent());
    }
}
