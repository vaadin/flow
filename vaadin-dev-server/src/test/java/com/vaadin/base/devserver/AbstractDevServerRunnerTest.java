/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.base.devserver.startup.AbstractDevModeTest;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;

import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.mockito.Mockito;

public class AbstractDevServerRunnerTest extends AbstractDevModeTest {

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
        public void updateServerStartupEnvironment(FrontendTools frontendTools,
                Map<String, String> environment) {
            super.updateServerStartupEnvironment(frontendTools, environment);
        }
    }

    @Test
    public void shouldPassEncodedUrlToDevServer() throws Exception {
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
        Assert.assertTrue("Dev server should have served the resource",
                devServer.serveDevModeRequest(request, response));
        Assert.assertEquals("foo%20bar", requestedPath.get());

    }

    @Test
    public void updateServerStartupEnvironment_preferIpv4_LocalhostIpAddressAddedToProcessEnvironment() {
        assertOnDevProcessEnvironment(Inet4Address.class, environment -> {
            Assert.assertNotNull(
                    "Expecting watchDogPort to be added to environment, but was not",
                    environment.get("watchDogPort"));

            String watchDogHost = environment.get("watchDogHost");
            Assert.assertNotNull(
                    "Expecting watchDogHost to be added to environment, but was not",
                    watchDogHost);
            // From InetAddress javadocs:
            // The IPv4 loopback address returned is only one of many in the
            // form 127.*.*.*
            Assert.assertTrue(
                    "Expecting watchDogHost to be an ipv4 address, but was "
                            + watchDogHost,
                    watchDogHost.matches("127\\.\\d+\\.\\d+\\.\\d+"));
        });
    }

    @Test
    public void updateServerStartupEnvironment_preferIpv6_LocalhostIpAddressAddedToProcessEnvironment() {
        assertOnDevProcessEnvironment(Inet6Address.class, environment -> {
            Assert.assertNotNull(
                    "Expecting watchDogPort to be added to environment, but was not",
                    environment.get("watchDogPort"));

            String watchDogHost = environment.get("watchDogHost");
            Assert.assertNotNull(
                    "Expecting watchDogHost to be added to environment, but was not",
                    watchDogHost);
            Assert.assertTrue(
                    "Expecting watchDogHost to be an ipv6 address, but was "
                            + watchDogHost,
                    "0:0:0:0:0:0:0:1".equals(watchDogHost)
                            || "::1".equals(watchDogHost));
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

    private void assertOnDevProcessEnvironment(
            Class<? extends InetAddress> loopbackAddressType,
            Consumer<Map<String, String>> op) {
        final DevServerWatchDog watchDog = new DevServerWatchDog();
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
