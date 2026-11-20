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
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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
 * The build reaches it by name, through a class loader over this jar: the Maven
 * and Gradle plugins must not depend on the daemon, and the version that has to
 * be provisioned is the one in the jar the project resolves and the CLI will
 * run. So this class name, the {@link #VERSION} field and the signature of
 * {@link #provision(Consumer)} are a contract with
 * {@code DevCliInstaller.provisionHotswapAgent} that no compiler checks -
 * renaming any of them turns the install goal into a no-op that only warns. The
 * same holds for {@link #provision(Consumer, Function)}, which the build
 * prefers when the daemon has it, and for {@link #COORDINATES}, which is what
 * that overload hands the build to resolve.
 * <p>
 * This is the only asset the loop downloads. Everything else it needs - the
 * daemon jar, which is also the javaagent - travels in as a project dependency
 * and is resolved from the local Maven repository. When the build provisions,
 * it resolves the agent the same way, so the mirrors, proxies and local
 * repository of the build apply to it too; only the daemon on its own, and a
 * build that cannot resolve it, download it directly.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class HotswapAgentJar {

    /**
     * Pinned, never "latest": a changing agent would make applies
     * irreproducible.
     * <p>
     * The checksum is of the {@code hotswap-agent-<version>.jar} artifact on
     * Maven Central at {@link #URL}, not of a GitHub release asset of the same
     * version - the two are built separately and there is no promise they are
     * the same bytes. Bumping the version means downloading that artifact and
     * recomputing this.
     */
    public static final String VERSION = "2.0.4";
    /**
     * SHA-256 of the artifact at {@link #URL}, verified after download.
     */
    public static final String SHA256 = "7ed7646da48ee408732a1c3625712a40d1e8381a9a75545caacbd06cbb5bdc95";

    /** Where the pinned {@link #VERSION} of the agent is downloaded from. */
    public static final String URL = "https://repo1.maven.org/maven2/org/hotswapagent/hotswap-agent/"
            + VERSION + "/hotswap-agent-" + VERSION + ".jar";

    /**
     * The Maven coordinates of the artifact at {@link #URL}, as
     * {@code groupId:artifactId:extension:version}, for a build that resolves
     * it from its own repositories instead.
     */
    public static final String COORDINATES = "org.hotswapagent:hotswap-agent:jar:"
            + VERSION;

    /**
     * How many times a download is attempted before giving up.
     */
    private static final int DOWNLOAD_ATTEMPTS = 4;

    /**
     * The longest wait between two download attempts, also when the server asks
     * for a longer one.
     */
    private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(30);

    /**
     * Answers that mean "not now" rather than "not there": a rate limit, which
     * Maven Central hands out to busy shared addresses such as CI runners, and
     * the server errors a proxy in front of it produces.
     */
    private static final Set<Integer> RETRIED_STATUSES = Set.of(429, 500, 502,
            503, 504);

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
     *
     * @return the machine-level dev-loop cache directory
     */
    public static Path cacheDir() {
        return Path.of(System.getProperty("user.home", "."), ".vaadin",
                "devloop");
    }

    /**
     * Where {@link #VERSION} lives once it has been provisioned.
     *
     * @return the path of the cached agent jar, which need not exist yet
     */
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
     * <p>
     * The same as {@link #provision(Consumer, Function)} with nothing to
     * resolve the jar from, so a missing jar is downloaded from {@link #URL}.
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
        return provision(progress, coordinates -> null);
    }

    /**
     * Ensures the HotswapAgent jar is present and matches the pinned checksum,
     * taking it from a repository of the caller's when it has to be fetched,
     * and answers where it is.
     * <p>
     * An explicit override wins, then an already-cached copy whose bytes still
     * verify, exactly as for {@link #provision(Consumer)}. Only when neither is
     * there is {@code resolver} asked for the jar at {@link #COORDINATES},
     * which is how a build provisions it through its own repository setup - the
     * local repository it has already cached, its mirrors and proxies, and the
     * retries it applies - instead of a direct download. What it answers is
     * verified against the pinned checksum and copied into the cache. When it
     * answers {@code null}, throws, or answers bytes that are not the pinned
     * ones, the jar is downloaded from {@link #URL} as before.
     *
     * @param progress
     *            where to report a download, since it is the one step here that
     *            takes long enough to need saying out loud
     * @param resolver
     *            answers a local copy of the artifact with the given
     *            coordinates, or {@code null} when it has none
     * @return the jar to load into the application JVM
     * @throws IOException
     *             if the jar can neither be resolved nor downloaded, or if the
     *             bytes in hand are not the pinned ones
     */
    public static Path provision(Consumer<String> progress,
            Function<String, Path> resolver) throws IOException {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            Path path = Path.of(override);
            if (!Files.isRegularFile(path)) {
                throw new IOException(
                        OVERRIDE_PROPERTY + " does not exist: " + path);
            }
            return path;
        }
        return provision(progress, resolver, cachedJar(), SHA256,
                URI.create(URL));
    }

    /**
     * {@link #provision(Consumer, Function)} past the override, against a given
     * cache location, checksum and download address.
     */
    static Path provision(Consumer<String> progress,
            Function<String, Path> resolver, Path jar, String sha256, URI url)
            throws IOException {
        if (Files.isRegularFile(jar)) {
            String actual = sha256(jar);
            if (!sha256.equalsIgnoreCase(actual)) {
                throw new IOException("cached HotswapAgent checksum mismatch: "
                        + jar + " (expected " + sha256 + ", got " + actual
                        + "). Delete it to re-download.");
            }
            return jar;
        }

        Files.createDirectories(jar.getParent());
        // A unique temp name rather than a fixed sibling: the daemon of one
        // application, the daemon of another and a build running the install
        // goal can all reach this at once, and two of them writing one shared
        // .part file would each verify half of the other download.
        Path temp = Files.createTempFile(jar.getParent(),
                "hotswap-agent-" + VERSION + "-", ".part");
        try {
            Optional<Path> resolved = resolve(progress, resolver);
            if (resolved.isPresent()) {
                Files.copy(resolved.get(), temp,
                        StandardCopyOption.REPLACE_EXISTING);
                String actual = sha256(temp);
                if (!sha256.equalsIgnoreCase(actual)) {
                    progress.accept("resolved HotswapAgent " + resolved.get()
                            + " does not match the pinned checksum (expected "
                            + sha256 + ", got " + actual
                            + "), downloading it instead");
                    resolved = Optional.empty();
                }
            }
            if (resolved.isEmpty()) {
                progress.accept("provisioning HotswapAgent " + VERSION
                        + " from " + url);
                download(url, temp, progress);
                String actual = sha256(temp);
                if (!sha256.equalsIgnoreCase(actual)) {
                    throw new IOException(
                            "downloaded HotswapAgent checksum mismatch (expected "
                                    + sha256 + ", got " + actual + ")");
                }
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

    /**
     * Asks the resolver for the jar, treating a failure of its own as having
     * nothing to offer: the download is still there to fall back on.
     */
    private static Optional<Path> resolve(Consumer<String> progress,
            Function<String, Path> resolver) {
        Path resolved;
        try {
            resolved = resolver.apply(COORDINATES);
        } catch (RuntimeException e) {
            progress.accept("could not resolve " + COORDINATES + ": " + e);
            return Optional.empty();
        }
        if (resolved == null || !Files.isRegularFile(resolved)) {
            return Optional.empty();
        }
        progress.accept(
                "provisioning HotswapAgent " + VERSION + " from " + resolved);
        return Optional.of(resolved);
    }

    /**
     * Downloads {@code url} into {@code target}, retrying the answers that only
     * mean "not now" with a growing delay, or the one the server asks for.
     */
    private static void download(URI url, Path target,
            Consumer<String> progress) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(url).GET().build();
        Duration backoff = Duration.ofSeconds(1);
        try {
            for (int attempt = 1;; attempt++) {
                HttpResponse<InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream in = response.body()) {
                    int status = response.statusCode();
                    if (status == 200) {
                        Files.copy(in, target,
                                StandardCopyOption.REPLACE_EXISTING);
                        return;
                    }
                    if (attempt >= DOWNLOAD_ATTEMPTS
                            || !RETRIED_STATUSES.contains(status)) {
                        throw new IOException("download failed with HTTP "
                                + status + " from " + url);
                    }
                }
                Duration delay = retryAfter(response).orElse(backoff);
                if (delay.compareTo(MAX_RETRY_DELAY) > 0) {
                    delay = MAX_RETRY_DELAY;
                }
                progress.accept("download got HTTP " + response.statusCode()
                        + ", retrying in " + delay.toSeconds() + "s");
                Thread.sleep(delay.toMillis());
                backoff = backoff.multipliedBy(2);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("download interrupted", e);
        }
    }

    /**
     * The delay a {@code Retry-After} header asks for, when it gives one in
     * seconds; the HTTP-date form is left to the regular backoff.
     */
    private static Optional<Duration> retryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After").map(String::trim)
                .filter(value -> value.matches("\\d{1,9}"))
                .map(value -> Duration.ofSeconds(Long.parseLong(value)));
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
