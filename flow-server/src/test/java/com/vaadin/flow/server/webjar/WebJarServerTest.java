package com.vaadin.flow.server.webjar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.tests.util.MockDeploymentConfiguration;

public class WebJarServerTest {

    private static final String GRID_DEPENDENCY = "/frontend/bower_components/vaadin-grid/theme/lumo/vaadin-grid-sorter.html";
    private static final String GRID_WEBJAR_RESOURCE = "/webjars/vaadin-grid/theme/lumo/vaadin-grid-sorter.html";

    private MockDeploymentConfiguration deploymentConfiguration;
    private WebJarServer webJarServer;

    private String baseUrl = "/.";

    @Before
    public void init() throws Exception {
        deploymentConfiguration = new MockDeploymentConfiguration();
        webJarServer = new WebJarServer(deploymentConfiguration);
    }

    @Test
    public void finds_resources() {
        Assert.assertEquals(GRID_WEBJAR_RESOURCE,
                webJarServer.getWebJarResourcePath(GRID_DEPENDENCY).get());
    }

    @Test
    public void no_match_when_frontend_starts_wrong() throws Exception {
        String value = "/wrong" + GRID_DEPENDENCY;

        Assert.assertFalse("Match found for path starting with '/wrong'",
                webJarServer.getWebJarResourcePath(value).isPresent());

        Assert.assertFalse("Match found for path starting with '/./wrong'",
                webJarServer.getWebJarResourcePath(baseUrl + value)
                        .isPresent());
    }

    @Test
    public void no_match_when_start_prefix_not_at_start() throws Exception {
        String value = "/no/match" + baseUrl + GRID_DEPENDENCY;

        Assert.assertFalse("Match found for path starting with '/no/match/.'",
                webJarServer.getWebJarResourcePath(value).isPresent());
    }
}
