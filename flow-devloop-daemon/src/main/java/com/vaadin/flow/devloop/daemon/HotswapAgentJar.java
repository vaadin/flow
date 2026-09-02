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
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.function.Consumer;

/**
 * The HotswapAgent jar the application JVM is launched with: where it is
 * cached, and how it gets there.
 * <p>
 * Separate from {@link Launch} because two very different callers provision it.
 * The daemon does, at the moment it composes the command line for the
 * application. The build does too, from {@code flow:install-dev-cli}, so that a
 * machine prepared while it had network access - a container image, a benchmark
 * sandbox - can run the loop afterwards without any. Both go through this class
 * so there is one pinned version, one checksum and one cache location rather
 * than two that can drift.
 * <p>
 * This is the only asset the loop downloads. Everything else it needs - the
 * daemon jar, which is also the javaagent - travels in as a project dependency
 * and is resolved from the local Maven repository.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class HotswapAgentJar {

    /**
     * Pinned, never "latest": a changing agent would make applies
     * irreproducible.
     * <p>
     * The checksum is of the {@code hotswap-agent-<version>.jar} release asset
     * at {@link #URL}, not of the same version on Maven Central - the two are
     * built separately and there is no promise they are the same bytes. Bumping
     * the version means downloading that asset and recomputing this.
     */
    public static final String VERSION = "2.0.3";
    public static final String SHA256 = "4ef49724b7d8523536d2e2a7310f827f4db9f4fed3489224e05d7bf87f0594f9";
    public static final String URL = "https://github.com/HotswapProjects/HotswapAgent/releases/download/RELEASE-"
            + VERSION + "/hotswap-agent-" + VERSION + ".jar";

    /**
     * Names a jar that is already on the machine, which is how an air-gapped
     * setup that never ran the install goal still gets an agent.
     */
    public static final String OVERRIDE_PROPERTY = "vaadin.dev.hotswapAgentJar";

    private HotswapAgentJar() {
    }

    /**
     * Where machine-level dev-loop assets are cached: the HotswapAgent jar, and
     * nothing else so far.
     * <p>
     * Under the {@code ~/.vaadin} directory Vaadin already owns, so one
     * download serves every application on the machine and nothing is written
     * into a project. It also survives a {@code mvn clean}, which a per-project
     * cache did not.
     */
    public static Path cacheDir() {
        return Path.of(System.getProperty("user.home", "."), ".vaadin",
                "devloop");
    }

    /** Where {@link #VERSION} lives once it has been provisioned. */
    public static Path cachedJar() {
        return cacheDir().resolve("hotswap-agent-" + VERSION + ".jar");
    }

    /**
     * Ensures the HotswapAgent jar is present and matches the pinned checksum,
     * and answers where it is.
     * <p>
     * Downloads only when it has to: an explicit override wins, then an
     * already-cached copy whose bytes still verify. So a machine provisioned
     * once needs no network again - not for the next application, and not after
     * a {@code mvn clean}.
     *
     * @param progress
     *            where to report a download, since it is the one step here that
     *            takes long enough to need saying out loud
     * @return the jar to load into the application JVM
     * @throws IOException
     *             if the jar cannot be downloaded, or if the bytes in hand are
     *             not the pinned ones
     */
    public static Path provision(Consumer<String> progress) throws IOException {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            Path path = Path.of(override);
            if (!Files.isRegularFile(path)) {
                throw new IOException(
                        OVERRIDE_PROPERTY + " does not exist: " + path);
            }
            return path;
        }

        Path jar = cachedJar();
        if (Files.isRegularFile(jar)) {
            String actual = sha256(jar);
            if (!SHA256.equalsIgnoreCase(actual)) {
                throw new IOException("cached HotswapAgent checksum mismatch: "
                        + jar + " (expected " + SHA256 + ", got " + actual
                        + "). Delete it to re-download.");
            }
            return jar;
        }

        Files.createDirectories(jar.getParent());
        progress.accept(
                "provisioning HotswapAgent " + VERSION + " from " + URL);
        // A unique temp name rather than a fixed sibling: the daemon of one
        // application, the daemon of another and a build running the install
        // goal can all reach this at once, and two of them writing one shared
        // .part file would each verify half of the other download.
        Path temp = Files.createTempFile(jar.getParent(),
                "hotswap-agent-" + VERSION + "-", ".part");
        try {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
                        .GET().build();
                HttpResponse<InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new IOException("download failed with HTTP "
                            + response.statusCode() + " from " + URL);
                }
                try (InputStream in = response.body()) {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("download interrupted", e);
            }

            String actual = sha256(temp);
            if (!SHA256.equalsIgnoreCase(actual)) {
                throw new IOException(
                        "downloaded HotswapAgent checksum mismatch (expected "
                                + SHA256 + ", got " + actual + ")");
            }
            // Not ATOMIC_MOVE: a file system is free to refuse that, and the
            // bytes are verified either way, so a concurrent provision can only
            // move the same content over the same name.
            Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        progress.accept("provisioned " + jar + " (verified sha256)");
        return jar;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }
}
