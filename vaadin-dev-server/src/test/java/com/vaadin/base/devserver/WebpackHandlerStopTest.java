/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpServer;
import com.vaadin.base.devserver.startup.AbstractDevModeTest;
import com.vaadin.flow.server.frontend.FrontendUtils;

import org.junit.Assert;
import org.junit.Test;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
@SuppressWarnings("restriction")
public class WebpackHandlerStopTest extends AbstractDevModeTest {

    private HttpServer httpServer;

    @Override
    public void teardown() {
        super.teardown();
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    public void testServer_should_HandleStopRequest() throws Exception {
        int port = WebpackHandler.getFreePort();

        // Server is not started
        assertNull(requestWebpackServer(port, "/foo"));

        // Running server should handle any request
        startTestServer(port, HTTP_OK, "{}");
        assertNotNull(requestWebpackServer(port, "/foo"));
        assertNotNull(requestWebpackServer(port, "/bar"));

        // Stop server
        assertNotNull(requestWebpackServer(port, "/stop"));
        assertNull(requestWebpackServer(port, "/foo"));
    }

    @Test
    public void devModeHandler_should_StopWebPack() throws Exception {

        int port = WebpackHandler.getFreePort();

        startTestServer(port, HTTP_OK, "{}");

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertEquals(port, ((WebpackHandler) handler).getPort());
        assertNotNull(requestWebpackServer(port, "/bar"));
        Assert.assertTrue(((WebpackHandler) handler).isRunning());

        handler.stop();
        Assert.assertFalse(((WebpackHandler) handler).isRunning());
        assertNull(requestWebpackServer(port, "/bar"));
    }

    @Test
    public void devModeHandler_should_Keep_WebPackOnRestart() throws Exception {
        int port = WebpackHandler.getFreePort();

        startTestServer(port, HTTP_OK, "{}");

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        simulateServerRestart();
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        Assert.assertTrue(((WebpackHandler) handler).isRunning());

        // Webpack server should continue working on the same port
        assertNotNull(requestWebpackServer(port, "/bar"));

        handler.stop();
        Assert.assertFalse(((WebpackHandler) handler).isRunning());
        assertNull(requestWebpackServer(port, "/bar"));
    }

    private void simulateServerRestart() throws Exception {
        // On a server restart/redeploy all reference to the running process are
        // lost so we simulate it by removing the handler reference
        WebpackHandlerTest.removeDevModeHandlerInstance(lookup);
    }

    private String requestWebpackServer(int port, String path) {
        try {
            URL url = new URL("http://localhost:" + port + path);
            return FrontendUtils.streamToString(url.openStream());
        } catch (IOException e) {
        }
        return null;
    }

    private int startTestServer(int port, int status, String response)
            throws Exception {
        if (port == 0) {
            port = WebpackHandler.getFreePort();
        }
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            String uri = exchange.getRequestURI().toString();
            if ("/stop".equals(uri)) {
                httpServer.stop(0);
            }
        });
        httpServer.start();
        return port;
    }

}
