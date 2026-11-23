/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.spring.test.AbstractSpringTest;

public class PwaResourcesIT extends AbstractSpringTest {

    private static final int SERVER_PORT = 8888;

    private static final long DEV_SERVER_TIMEOUT_MILLIS = 60000;

    // Redirects are not followed so that an authentication redirect is
    // reported as such, instead of showing up as the login page content
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();

    @Override
    protected int getDeploymentPort() {
        return SERVER_PORT;
    }

    @Test
    public void customManifestPath_allowed() throws Exception {
        assertBodyContains("/" + Configurator.MANIFEST_PATH,
                Configurator.APP_NAME);
    }

    @Test
    public void customOfflinePath_allowed() throws Exception {
        assertBodyContains("/" + Configurator.OFFLINE_PATH,
                "CUSTOM OFFLINE PAGE");
    }

    @Test
    public void offlineResource_allowed() throws Exception {
        assertBodyContains("/" + Configurator.OFFLINE_RESOURCE,
                "CUSTOM OFFLINE RESOURCE");
    }

    @Test
    public void offlineStub_allowed() throws Exception {
        // The stub path is on the static public resource list, so it is
        // permitted regardless of the configured offline path, and Flow serves
        // it with the same contents as the offline page
        assertBodyContains("/" + PwaHandler.DEFAULT_OFFLINE_STUB_PATH,
                "CUSTOM OFFLINE PAGE");
    }

    @Test
    public void pwaResources_precached() throws Exception {
        String precache = get("/sw-runtime-resources-precache.js");
        for (String path : new String[] { Configurator.MANIFEST_PATH,
                Configurator.OFFLINE_PATH, Configurator.OFFLINE_RESOURCE }) {
            Assert.assertTrue("Expecting '" + path
                    + "' to be in the precache manifest, but got: " + precache,
                    precache.contains(path));
        }
    }

    @Test
    public void staticResourceNotConfiguredForOffline_protected()
            throws Exception {
        HttpResponse<String> response = send("/secret.txt");
        Assert.assertNotEquals(
                "Expecting /secret.txt not to be publicly accessible", 200,
                response.statusCode());
    }

    private void assertBodyContains(String path, String expected)
            throws Exception {
        String body = get(path);
        Assert.assertTrue("Expecting " + path + " to contain '" + expected
                + "', but got: " + body, body.contains(expected));
    }

    private String get(String path) throws Exception {
        HttpResponse<String> response = send(path);
        Assert.assertEquals("Expecting 200 OK for " + path, 200,
                response.statusCode());
        return response.body();
    }

    /**
     * Sends a GET request, retrying while the frontend development server is
     * still starting up, since until then every request is answered with a
     * loading page marked by the {@code X-DevModePending} header.
     */
    private HttpResponse<String> send(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(getRootURL() + path)).build();
        long deadline = System.currentTimeMillis() + DEV_SERVER_TIMEOUT_MILLIS;
        HttpResponse<String> response;
        do {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.headers().firstValue("X-DevModePending").isEmpty()) {
                return response;
            }
            Thread.sleep(500);
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError(
                "The frontend development server did not start within "
                        + DEV_SERVER_TIMEOUT_MILLIS + "ms");
    }
}
