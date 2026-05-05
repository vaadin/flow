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
package com.vaadin.base.devserver;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.junit.AssumptionViolatedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.base.devserver.startup.AbstractDevModeTest;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.server.frontend.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AbstractDevServerRunnerTest extends AbstractDevModeTest {

    private class DummyRunner extends AbstractDevServerRunner {

        protected DummyRunner() {
            super(lookup, 0, npmFolder,
                    CompletableFuture.completedFuture(null));
        }

        @Override
        protected File getServerBinary() {
            return new File("dummy.bin");
        }

        @Override
        protected File getServerConfig() {
            return new File("dummy.config");
        }

        @Override
        protected String getServerName() {
            return "Dummy server";
        }

        @Override
        void doStartDevModeServer() throws ExecutionFailedException {
        }

        @Override
        protected List<String> getServerStartupCommand(FrontendTools tools) {
            List<String> commands = new ArrayList<>();
            commands.add("echo");
            return commands;
        }

        @Override
        protected Pattern getServerSuccessPattern() {
            return Pattern.compile("Dummy success");
        }

        @Override
        protected Pattern getServerFailurePattern() {
            return Pattern.compile("Dummy fail");
        }

        @Override
        protected boolean checkConnection() {
            return true;
        }

        @Override
        public HttpURLConnection prepareConnection(String path, String method)
                throws IOException {
            return Mockito.mock(HttpURLConnection.class);
        }

        // Expose for testing
        @Override
        protected void updateServerStartupEnvironment(
                FrontendTools frontendTools, Map<String, String> environment) {
            super.updateServerStartupEnvironment(frontendTools, environment);
        }
    }

    @Test
    void shouldPassEncodedUrlToDevServer() throws Exception {
        handler = new DummyRunner();
        waitForDevServer();
        DevModeHandler devServer = Mockito.spy(handler);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getOutputStream())
                .thenReturn(Mockito.mock(ServletOutputStream.class));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/foo%20bar");
        Mockito.when(request.getPathInfo()).thenReturn("foo bar");
        Mockito.when(request.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());

        AtomicReference<String> requestedPath = new AtomicReference<>();
        Mockito.when(devServer.prepareConnection(Mockito.any(), Mockito.any()))
                .then(invocation -> {
                    requestedPath.set((String) invocation.getArguments()[0]);
                    return Mockito.mock(HttpURLConnection.class);
                });
        assertTrue(devServer.serveDevModeRequest(request, response),
                "Dev server should have served the resource");
        assertEquals("foo%20bar", requestedPath.get());

    }

    @Test
    void updateServerStartupEnvironment_preferIpv4_LocalhostIpAddressAddedToProcessEnvironment() {
        assertOnDevProcessEnvironment(Inet4Address.class, environment -> {
            assertNotNull(environment.get("watchDogPort"),
                    "Expecting watchDogPort to be added to environment, but was not");

            String watchDogHost = environment.get("watchDogHost");
            assertNotNull(watchDogHost,
                    "Expecting watchDogHost to be added to environment, but was not");
            // From InetAddress javadocs:
            // The IPv4 loopback address returned is only one of many in the
            // form 127.*.*.*
            assertTrue(watchDogHost.matches("127\\.\\d+\\.\\d+\\.\\d+"),
                    "Expecting watchDogHost to be an ipv4 address, but was "
                            + watchDogHost);
        });
    }

    @Test
    void updateServerStartupEnvironment_preferIpv6_LocalhostIpAddressAddedToProcessEnvironment() {
        assertOnDevProcessEnvironment(Inet6Address.class, environment -> {
            assertNotNull(environment.get("watchDogPort"),
                    "Expecting watchDogPort to be added to environment, but was not");

            String watchDogHost = environment.get("watchDogHost");
            assertNotNull(watchDogHost,
                    "Expecting watchDogHost to be added to environment, but was not");
            assertTrue(
                    "0:0:0:0:0:0:0:1".equals(watchDogHost)
                            || "::1".equals(watchDogHost),
                    "Expecting watchDogHost to be an ipv6 address, but was "
                            + watchDogHost);
        });
    }

    private InetAddress findLocalhostAddress(
            Class<? extends InetAddress> type) {
        try {
            return Arrays.stream(InetAddress.getAllByName("localhost"))
                    .filter(type::isInstance).findFirst()
                    .orElseThrow(() -> new AssumptionViolatedException(
                            "localhost address not found for "
                                    + type.getName()));
        } catch (UnknownHostException e) {
            // should never happen for localhost
            throw new AssertionError("Cannot detect addresses for localhost",
                    e);
        }
    }

    @Test
    void hopByHopRequestHeadersAreNotForwardedToDevServer() throws Exception {
        handler = new DummyRunner();
        waitForDevServer();
        DevModeHandler devServer = Mockito.spy(handler);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getOutputStream())
                .thenReturn(Mockito.mock(ServletOutputStream.class));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/VAADIN/test.js");
        Mockito.when(request.getRequestURI()).thenReturn("/VAADIN/test.js");

        // Simulate browser headers including hop-by-hop
        List<String> headerNames = List.of("Connection", "Keep-Alive", "Accept",
                "Host", "Accept-Encoding");
        Mockito.when(request.getHeaderNames())
                .thenReturn(Collections.enumeration(headerNames));
        Mockito.when(request.getHeader("Connection")).thenReturn("keep-alive");
        Mockito.when(request.getHeader("Keep-Alive")).thenReturn("timeout=5");
        Mockito.when(request.getHeader("Accept")).thenReturn("*/*");
        Mockito.when(request.getHeader("Host")).thenReturn("localhost:8080");
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("gzip");

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getResponseCode())
                .thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(connection.getHeaderFields()).thenReturn(Map.of());
        Mockito.when(connection.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        Mockito.when(devServer.prepareConnection(Mockito.any(), Mockito.any()))
                .thenReturn(connection);

        devServer.serveDevModeRequest(request, response);

        // Hop-by-hop headers must not be forwarded
        verify(connection, never()).setRequestProperty(Mockito.eq("Connection"),
                Mockito.eq("keep-alive"));
        verify(connection, never()).setRequestProperty(Mockito.eq("Keep-Alive"),
                Mockito.anyString());

        // Connection must be set to close since it's not reused
        verify(connection).setRequestProperty("Connection", "close");

        // Non-hop-by-hop headers must be forwarded
        verify(connection).setRequestProperty("Accept", "*/*");
        verify(connection).setRequestProperty("Host", "localhost:8080");
        verify(connection).setRequestProperty("Accept-Encoding", "gzip");
    }

    @Test
    void hopByHopAndContentLengthResponseHeadersAreNotForwardedToBrowser()
            throws Exception {
        handler = new DummyRunner();
        waitForDevServer();
        DevModeHandler devServer = Mockito.spy(handler);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/VAADIN/test.js");
        Mockito.when(request.getRequestURI()).thenReturn("/VAADIN/test.js");
        Mockito.when(request.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());

        // Dev server response with hop-by-hop headers, Content-Length, and
        // a legitimate end-to-end header
        Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        responseHeaders.put(null, List.of("HTTP/1.1 200 OK"));
        responseHeaders.put("Content-Type", List.of("application/javascript"));
        responseHeaders.put("Content-Length", List.of("42"));
        responseHeaders.put("Connection", List.of("keep-alive"));
        responseHeaders.put("Keep-Alive", List.of("timeout=5"));
        responseHeaders.put("Transfer-Encoding", List.of("chunked"));

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getResponseCode())
                .thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(connection.getHeaderFields()).thenReturn(responseHeaders);
        Mockito.when(connection.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        Mockito.when(devServer.prepareConnection(Mockito.any(), Mockito.any()))
                .thenReturn(connection);

        devServer.serveDevModeRequest(request, response);

        // Only end-to-end headers should be forwarded
        verify(response).setHeader("Content-Type", "application/javascript");

        // Hop-by-hop headers must not be forwarded (RFC 9110 Section 7.6.1)
        verify(response, never()).setHeader(Mockito.eq("Connection"),
                Mockito.anyString());
        verify(response, never()).addHeader(Mockito.eq("Connection"),
                Mockito.anyString());
        verify(response, never()).setHeader(Mockito.eq("Keep-Alive"),
                Mockito.anyString());
        verify(response, never()).addHeader(Mockito.eq("Keep-Alive"),
                Mockito.anyString());
        verify(response, never()).setHeader(Mockito.eq("Transfer-Encoding"),
                Mockito.anyString());
        verify(response, never()).addHeader(Mockito.eq("Transfer-Encoding"),
                Mockito.anyString());

        // Content-Length must not be forwarded (RFC 9110 Section 8.6:
        // may not match actual bytes after HttpURLConnection decoding)
        verify(response, never()).setHeader(Mockito.eq("Content-Length"),
                Mockito.anyString());
        verify(response, never()).addHeader(Mockito.eq("Content-Length"),
                Mockito.anyString());
    }

    @Test
    void outputStreamNotClosedAfterSendError() throws Exception {
        handler = new DummyRunner();
        waitForDevServer();
        DevModeHandler devServer = Mockito.spy(handler);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/VAADIN/test.js");
        Mockito.when(request.getRequestURI()).thenReturn("/VAADIN/test.js");
        Mockito.when(request.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getResponseCode())
                .thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        Mockito.when(connection.getHeaderFields()).thenReturn(Map.of());

        Mockito.when(devServer.prepareConnection(Mockito.any(), Mockito.any()))
                .thenReturn(connection);

        devServer.serveDevModeRequest(request, response);

        // sendError() commits the response; closing the output stream
        // after that can throw in some servlet containers
        verify(response).sendError(HttpURLConnection.HTTP_INTERNAL_ERROR);
        verify(outputStream, never()).close();
    }

    private void assertOnDevProcessEnvironment(
            Class<? extends InetAddress> loopbackAddressType,
            Consumer<Map<String, String>> op) {
        final DevServerWatchDog watchDog = new DevServerWatchDog();
        watchDog.start();
        final InetAddress loopbackAddress = findLocalhostAddress(
                loopbackAddressType);
        try {
            handler = new DummyRunner() {
                @Override
                protected DevServerWatchDog getWatchDog() {
                    return watchDog;
                }

                @Override
                InetAddress getLoopbackAddress() {
                    return loopbackAddress;
                }
            };

            FrontendTools frontendTools = new FrontendTools(
                    new FrontendToolsSettings(
                            System.getProperty("java.io.tmpdir"), null));
            Map<String, String> environment = new HashMap<>();
            ((AbstractDevServerRunner) handler)
                    .updateServerStartupEnvironment(frontendTools, environment);
            op.accept(environment);
        } finally {
            watchDog.stop();
        }
    }

}
