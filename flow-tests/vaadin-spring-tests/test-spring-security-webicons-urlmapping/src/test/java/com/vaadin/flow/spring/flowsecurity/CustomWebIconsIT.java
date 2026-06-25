/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.spring.test.AbstractSpringTest;

public class CustomWebIconsIT extends AbstractSpringTest {

    private static final int SERVER_PORT = 8888;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    @Override
    protected int getDeploymentPort() {
        return SERVER_PORT;
    }

    @Test
    public void customPWAIcons_allowed() throws Exception {
        assertResourceIsAccessible("hey.png", true);
        assertResourceIsAccessible("hey-16x16.png");
        assertResourceIsAccessible("hey-32x32.png");
        assertResourceIsAccessible("hey-144x144.png");
    }

    @Test
    public void customFavIcon_allowed() throws Exception {
        assertResourceIsAccessible("fav.ico", true);
    }

    protected void assertResourceIsAccessible(String path) throws Exception {
        assertResourceIsAccessible(path, false);
    }

    protected void assertResourceIsAccessible(String path, boolean absolute)
            throws Exception {
        String url = getRootURL() + (absolute ? "/" : "/urlmapping/")
                + Configurator.ICONS_PATH + path;
        String loginUrl = getRootURL() + "/urlmapping/login";
        driver.get(url);
        waitUntil(driver -> {
            String currentUrl = driver.getCurrentUrl();
            Assert.assertNotEquals("Expecting " + url
                    + " to be accessible but browser was redirected to "
                    + currentUrl, loginUrl, currentUrl);
            return url.equals(currentUrl);
        });
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().GET()
                        .uri(URI.create(driver.getCurrentUrl())).build(),
                HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals("Expecting 200 OK", 200, response.statusCode());
        Assert.assertFalse(
                "Expecting " + url + " to reference an image, not an HTML page",
                response.body().matches("<html.*>"));
    }
}
