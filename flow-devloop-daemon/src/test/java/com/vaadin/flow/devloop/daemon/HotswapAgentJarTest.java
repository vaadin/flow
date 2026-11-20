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
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provisioning has to be offline-clean: the build installs the jar once, while
 * the machine still has network access, and every later {@code start} - of this
 * application or any other on the machine - has to be satisfied by what is
 * already there rather than reaching for the network again.
 */
class HotswapAgentJarTest {

    private final List<String> progress = new ArrayList<>();

    @AfterEach
    void clearOverride() {
        System.clearProperty(HotswapAgentJar.OVERRIDE_PROPERTY);
    }

    @Test
    void cacheDir_isMachineLevelRatherThanPerProject() {
        // One download per machine, and nothing written into a project - which
        // also means mvn clean does not throw it away.
        assertEquals(
                Path.of(System.getProperty("user.home"), ".vaadin", "devloop"),
                HotswapAgentJar.cacheDir());
        assertEquals(
                HotswapAgentJar.cacheDir().resolve(
                        "hotswap-agent-" + HotswapAgentJar.VERSION + ".jar"),
                HotswapAgentJar.cachedJar());
    }

    @Test
    void provision_honoursAnOverrideWithoutTouchingTheNetwork(@TempDir Path dir)
            throws IOException {
        Path own = Files.writeString(dir.resolve("ha.jar"), "not really a jar");
        System.setProperty(HotswapAgentJar.OVERRIDE_PROPERTY, own.toString());

        // No checksum on an override: the point of it is a jar the developer
        // vouches for, which is not required to be the pinned artifact.
        assertEquals(own, HotswapAgentJar.provision(progress::add));
        assertTrue(progress.isEmpty(),
                "nothing was downloaded, so nothing should be reported");
    }

    @Test
    void provision_failsLoudlyWhenTheOverrideIsMissing(@TempDir Path dir) {
        Path absent = dir.resolve("absent.jar");
        System.setProperty(HotswapAgentJar.OVERRIDE_PROPERTY,
                absent.toString());

        IOException e = assertThrows(IOException.class,
                () -> HotswapAgentJar.provision(progress::add));
        assertTrue(e.getMessage().contains(absent.toString()), e.getMessage());
    }

    @Test
    void provision_resolvedCopy_isVerifiedAndCachedWithoutDownloading(
            @TempDir Path dir) throws IOException {
        Path resolved = Files.writeString(dir.resolve("from-m2.jar"),
                "agent bytes 7f3a");
        Path cached = dir.resolve("cache").resolve("ha.jar");
        List<String> asked = new ArrayList<>();

        // Port 1 refuses connections, so reaching the download fails the case.
        Path jar = HotswapAgentJar.provision(progress::add, coordinates -> {
            asked.add(coordinates);
            return resolved;
        }, cached, sha256("agent bytes 7f3a"),
                URI.create("http://127.0.0.1:1/ha.jar"));

        assertEquals(cached, jar);
        assertEquals("agent bytes 7f3a", Files.readString(cached));
        assertEquals(List.of("org.hotswapagent:hotswap-agent:jar:"
                + HotswapAgentJar.VERSION), asked);
    }

    @ParameterizedTest
    @ValueSource(strings = { "tampered", "missing", "throws" })
    void provision_resolverHasNoUsableCopy_downloadsRetryingARateLimit(
            String resolverAnswer, @TempDir Path dir) throws IOException {
        Path tampered = Files.writeString(dir.resolve("from-m2.jar"),
                "tampered");
        Function<String, Path> resolver = coordinates -> switch (resolverAnswer) {
        case "tampered" -> tampered;
        case "missing" -> dir.resolve("absent.jar");
        default -> throw new IllegalStateException("no repository");
        };
        Path cached = dir.resolve("cache").resolve("ha.jar");
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer
                .create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ha.jar", exchange -> {
            if (requests.incrementAndGet() == 1) {
                exchange.getResponseHeaders().add("Retry-After", "0");
                exchange.sendResponseHeaders(429, -1);
            } else {
                byte[] body = "agent bytes 7f3a"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
            exchange.close();
        });
        server.start();
        try {
            HotswapAgentJar.provision(progress::add, resolver, cached,
                    sha256("agent bytes 7f3a"), URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/ha.jar"));
        } finally {
            server.stop(0);
        }

        assertEquals("agent bytes 7f3a", Files.readString(cached));
        assertEquals(2, requests.get());
        // The wait the server asked for, not the default backoff.
        assertTrue(progress.contains("download got HTTP 429, retrying in 0s"),
                progress.toString());
    }

    @ParameterizedTest
    @CsvSource({ "404, 1", "503, 4" })
    void provision_downloadKeepsFailing_givesUpWithTheStatus(int status,
            int expectedRequests, @TempDir Path dir) throws IOException {
        Path cached = dir.resolve("cache").resolve("ha.jar");
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer
                .create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ha.jar", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Retry-After", "0");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        IOException e;
        try {
            e = assertThrows(IOException.class,
                    () -> HotswapAgentJar.provision(progress::add,
                            coordinates -> null, cached, sha256("unused"),
                            URI.create("http://127.0.0.1:"
                                    + server.getAddress().getPort()
                                    + "/ha.jar")));
        } finally {
            server.stop(0);
        }

        // A missing artifact is not worth asking for again; a busy server is,
        // but only a few times.
        assertTrue(e.getMessage().contains("HTTP " + status), e.getMessage());
        assertEquals(expectedRequests, requests.get());
        assertFalse(Files.exists(cached));
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
