package com.vaadin.flow.server.webjar;

import javax.servlet.ServletContext;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;

public class WebJarServerTest {

    private static final String GRID_DEPENDENCY = "/frontend/bower_components/vaadin-grid/theme/lumo/vaadin-grid-sorter.html";
    private static final String GRID_WEBJAR = "/webjars/vaadin-grid/theme/lumo/vaadin-grid-sorter.html";

    private WebJarServer webJarServer;

    private ServletContext context;

    private String baseUrl = "/.";
    private String upStep = "/..";

    @Before
    public void init() throws Exception {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(configuration.getDevelopmentFrontendPrefix())
                .thenReturn(Constants.FRONTEND_URL_DEV_DEFAULT);

        webJarServer = new WebJarServer(configuration);
        context = Mockito.mock(ServletContext.class);

        Mockito.when(context.getResource(GRID_WEBJAR))
                .thenReturn(new URL("http://localhost:8080" + GRID_DEPENDENCY));
    }

    @Test
    public void test_webjar_resolves_without_any_baseurl() throws Exception {
        String value = GRID_DEPENDENCY;

        boolean foundComponent = webJarServer.hasWebJarResource(value, context);

        Assert.assertTrue(
                "Expected webJarServer to find fixed component for value: "
                        + value,
                foundComponent);
    }

    @Test
    public void test_webjar_resolves_for_baseurl() throws Exception {
        String value = GRID_DEPENDENCY;

        boolean foundComponent = webJarServer.hasWebJarResource(baseUrl + value,
                context);

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
                webJarServer.hasWebJarResource(upStep + value, context));

        Assert.assertTrue("No match found for /./.." + value, webJarServer
                .hasWebJarResource(baseUrl + upStep + value, context));

        Assert.assertTrue("No match found for /./../.." + value, webJarServer
                .hasWebJarResource(baseUrl + upStep + upStep + value, context));
    }

    @Test
    public void no_match_when_frontend_starts_wrong() throws Exception {
        String value = "/wrong" + GRID_DEPENDENCY;

        Assert.assertFalse("Match found for path starting with '/wrong'",
                webJarServer.hasWebJarResource(value, context));

        Assert.assertFalse("Match found for path starting with '/./wrong'",
                webJarServer.hasWebJarResource(baseUrl + value, context));
    }

    @Test
    public void no_match_when_start_prefix_not_at_start() throws Exception {
        String value = "/no/match" + baseUrl + GRID_DEPENDENCY;

        Assert.assertFalse("Match found for path starting with '/no/match/.'",
                webJarServer.hasWebJarResource(value, context));
    }
}
