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
package com.vaadin.flow.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

/**
 * Runs the browser tests through a reverse proxy that terminates HTTPS, like
 * the proxies applications are deployed behind.
 * <p>
 * The proxy is selected with the {@value #PROXY_PROPERTY} system property and
 * its configuration with the {@value #CONFIG_PROPERTY} system property. The
 * proxy runs in a Docker container started with Testcontainers once per JVM,
 * and forwards to the server the tests are deployed to. Without the
 * {@value #PROXY_PROPERTY} system property the tests connect to the server
 * directly.
 */
public final class ReverseProxy {

    /**
     * The system property that selects the proxy, one of {@code nginx} or
     * {@code caddy}.
     */
    public static final String PROXY_PROPERTY = "reverse.proxy";

    /**
     * The system property that selects the configuration of the proxy:
     * {@code recommended} for a configuration tuned for Vaadin, or
     * {@code default} for a minimal one that leaves the defaults of the proxy
     * in place. Defaults to {@code recommended}.
     */
    public static final String CONFIG_PROPERTY = "reverse.proxy.config";

    private static String rootUrl;

    private ReverseProxy() {
    }

    /**
     * Checks whether the tests run through a reverse proxy.
     *
     * @return {@code true} if a proxy is selected with the
     *         {@value #PROXY_PROPERTY} system property
     */
    public static boolean isEnabled() {
        String proxy = System.getProperty(PROXY_PROPERTY);
        return proxy != null && !proxy.isBlank();
    }

    /**
     * Gets the URL the browser opens the server through, starting the proxy the
     * first time it is called.
     *
     * @param backendPort
     *            the port of the server the proxy forwards to, on the host
     *            running the tests
     * @return the HTTPS root URL of the proxy, e.g.
     *         {@code https://localhost:32768}
     */
    public static synchronized String getRootUrl(int backendPort) {
        if (rootUrl == null) {
            rootUrl = ProxyContainer.start(System.getProperty(PROXY_PROPERTY),
                    System.getProperty(CONFIG_PROPERTY, "recommended"),
                    backendPort);
        }
        return rootUrl;
    }

    /**
     * Keeps the Testcontainers classes out of {@link ReverseProxy}, so that the
     * tests that do not use a proxy run without Testcontainers on the
     * classpath.
     */
    private static final class ProxyContainer {

        private static final int HTTPS_PORT = 443;

        private enum Proxy {
            NGINX("nginx:1.30.6", "/etc/nginx/conf.d/default.conf"),
            CADDY("caddy:2.11.7", "/etc/caddy/Caddyfile");

            private final String image;
            private final String configPath;

            Proxy(String image, String configPath) {
                this.image = image;
                this.configPath = configPath;
            }
        }

        private static String start(String proxyName, String configName,
                int backendPort) {
            Proxy proxy = Arrays.stream(Proxy.values())
                    .filter(p -> p.name().equalsIgnoreCase(proxyName.trim()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown reverse proxy '" + proxyName + "' in "
                                    + PROXY_PROPERTY + ", use one of "
                                    + Arrays.toString(Proxy.values())));
            String proxyId = proxy.name().toLowerCase(Locale.ENGLISH);

            // Makes the server reachable from the proxy container as
            // host.testcontainers.internal
            Testcontainers.exposeHostPorts(backendPort);
            String backend = "host.testcontainers.internal:" + backendPort;

            GenericContainer<?> container = new GenericContainer<>(proxy.image)
                    .withExposedPorts(HTTPS_PORT)
                    .waitingFor(Wait.forListeningPort());
            // The host name the browser uses is known before the container
            // starts, which lets the proxy issue its certificate for it
            String host = container.getHost();
            container.withCopyToContainer(Transferable.of(readConfig(
                    proxyId + "-" + configName + ".conf", host, backend)),
                    proxy.configPath);
            if (proxy == Proxy.NGINX) {
                // Scripts in this directory run before nginx starts
                container.withCopyToContainer(
                        Transferable.of(readConfig("nginx-certificate.sh", host,
                                backend), 0755),
                        "/docker-entrypoint.d/10-certificate.sh");
            }
            container.start();

            String url = "https://" + host + ":"
                    + container.getMappedPort(HTTPS_PORT);
            LoggerFactory.getLogger(ReverseProxy.class).info(
                    "Running the tests through {} with the {} configuration at {}",
                    proxyId, configName, url);
            return url;
        }

        private static String readConfig(String name, String host,
                String backend) {
            try (InputStream stream = ReverseProxy.class
                    .getResourceAsStream("reverseproxy/" + name)) {
                if (stream == null) {
                    throw new IllegalArgumentException(
                            "No reverse proxy configuration " + name + ", set "
                                    + CONFIG_PROPERTY
                                    + " to recommended or default");
                }
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("@HOST@", host).replace("@BACKEND@", backend);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
