/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
