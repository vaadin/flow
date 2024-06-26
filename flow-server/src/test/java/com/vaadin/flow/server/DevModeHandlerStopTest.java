/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpServer;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@NotThreadSafe
@SuppressWarnings("restriction")
@Ignore("This test may cause freeze of a build. "
        + "It happens all the time for Java 11 validation and it happens sometimes on PR validation")
public class DevModeHandlerStopTest {

    private MockDeploymentConfiguration configuration;

    private HttpServer httpServer;
    private File npmFolder;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {

        npmFolder = temporaryFolder.getRoot();
        configuration = new MockDeploymentConfiguration();
    }

    @After
    public void teardown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        if (handler != null) {
            handler.stop();
        }
    }

    @Test
    public void testServer_should_HandleStopRequest() throws Exception {
        int port = DevModeHandler.getFreePort();

        // Server is not started
        assertNull(requestWebpackServer(port, "/foo"));

        // Running server should handle any request
        startTestServer(port, HTTP_OK, "OK");
        assertNotNull(requestWebpackServer(port, "/foo"));
        assertNotNull(requestWebpackServer(port, "/bar"));

        // Stop server
        assertNotNull(requestWebpackServer(port, "/stop"));
        assertNull(requestWebpackServer(port, "/foo"));
    }

    @Test
    public void devModeHandler_should_StopWebPack() throws Exception {

        int port = DevModeHandler.getFreePort();

        startTestServer(port, HTTP_OK, "OK");

        DevModeHandler.start(port, configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();
        assertEquals(port, DevModeHandler.getDevModeHandler().getPort());
        assertNotNull(requestWebpackServer(port, "/bar"));

        DevModeHandler.getDevModeHandler().stop();
        assertNull(DevModeHandler.getDevModeHandler());
        assertNull(requestWebpackServer(port, "/bar"));
    }

    @Test
    public void devModeHandler_should_Keep_WebPackOnRestart() throws Exception {
        int port = DevModeHandler.getFreePort();

        startTestServer(port, HTTP_OK, "OK");

        DevModeHandler.start(port, configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();

        // Simulate a server restart by removing the handler, and starting a new
        // one
        DevModeHandlerTest.removeDevModeHandlerInstance();
        assertNull(DevModeHandler.getDevModeHandler());
        DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();

        // Webpack server should continue working
        assertNotNull(requestWebpackServer(port, "/bar"));

        DevModeHandler.getDevModeHandler().stop();
        assertNull(DevModeHandler.getDevModeHandler());
        assertNull(requestWebpackServer(port, "/bar"));
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
            port = DevModeHandler.getFreePort();
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
