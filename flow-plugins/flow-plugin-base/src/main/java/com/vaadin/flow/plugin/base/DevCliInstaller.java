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
package com.vaadin.flow.plugin.base;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;

/**
 * Installs the {@code vaadin-dev} CLI and its agent skills into a project.
 * <p>
 * The payload ships as classpath resources of this module - the one that
 * already holds logic shared by the Maven and Gradle plugins - so a future
 * Gradle task reads the same bytes from the same jar. Nothing generated and no
 * jars are copied: the CLI resolves the dev-loop daemon from the project's own
 * dependencies at first use.
 * <p>
 * {@link #provisionHotswapAgent} is the second half of an install, and the only
 * part that needs the network: it puts the one asset the loop cannot resolve
 * from a Maven repository into the machine-level cache, so that a machine
 * prepared here - a container image built while it had network access, say -
 * can run the loop later with none. It runs the provisioning code out of the
 * daemon jar the project resolves, which is deliberately not a dependency of
 * this module; see there for why.
 * <p>
 * Everything is installed relative to one directory, and the two skill trees
 * are installed as siblings, because the Claude adapter links to the shared
 * instructions by relative path.
 * <p>
 * All of it is meant to be <em>committed</em> by the developer: it is project
 * tooling, like {@code mvnw}, and the point is that every agent and every
 * developer on the repository gets the same instructions.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class DevCliInstaller {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DevCliInstaller.class);

    private static final String RESOURCE_ROOT = "vaadin-dev-cli/";

    /**
     * Loaded out of the daemon jar the project resolves rather than imported;
     * see {@link #provisionHotswapAgent}.
     */
    private static final String PROVISIONER = "com.vaadin.flow.devloop.daemon.HotswapAgentJar";

    /**
     * What is installed where, resource path to project-relative path.
     * <p>
     * An explicit manifest rather than a directory walk: a jar is not a
     * directory, and naming the files is also the specification of what the
     * goal is allowed to write.
     */
    private static final Map<String, String> PAYLOAD = payload();

    /** The files that have to be executable on POSIX. */
    private static final Set<String> EXECUTABLE = Set.of(".vaadin/vaadin-dev");

    private static Map<String, String> payload() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("vaadin-dev", ".vaadin/vaadin-dev");
        files.put("vaadin-dev.ps1", ".vaadin/vaadin-dev.ps1");
        files.put("vaadin-dev.cmd", ".vaadin/vaadin-dev.cmd");
        // The daemon writes its handshake into the same directory, and that
        // carries a per-run auth token for the local daemon. The scripts are
        // meant to be committed and the handshake must not be, so the
        // directory ships the rule that tells them apart.
        files.put("gitignore", ".vaadin/.gitignore");
        // The canonical, tool-agnostic instructions: the single source of truth
        // for the loop's behaviour, and what a non-Claude agent reads.
        files.put("agents-skill/vaadin-devloop/SKILL.md",
                ".agents/skills/vaadin-devloop/SKILL.md");
        files.put("agents-skill/vaadin-devloop/reference.md",
                ".agents/skills/vaadin-devloop/reference.md");
        // A thin adapter carrying the frontmatter Claude Code requires and the
        // tool bindings for it; it links to the .agents copy above.
        files.put("claude-skill/vaadin-devloop/SKILL.md",
                ".claude/skills/vaadin-devloop/SKILL.md");
        return Map.copyOf(files);
    }

    /**
     * What an installation did, so a caller can report it.
     *
     * @param written
     *            the files created or updated
     * @param unchanged
     *            the files that already held exactly these bytes
     */
    public record Result(List<Path> written, List<Path> unchanged) {
    }

    private DevCliInstaller() {
    }

    /**
     * Installs the CLI and the skills under the given directory.
     * <p>
     * The CLI goes into {@code .vaadin/}, the directory Vaadin already owns in
     * a project: the dev-loop daemon writes its handshake there, so keeping the
     * scripts beside it means one hidden directory rather than a second visible
     * one at the project root.
     * <p>
     * A file is written whenever it is absent or differs from the shipped copy,
     * and left alone when it already holds exactly those bytes. There is
     * deliberately no "you edited this, so I will keep it" case: these files
     * are project tooling, like {@code mvnw}, and regenerating them is what the
     * goal is for. Anyone who needs project-specific agent instructions should
     * add their own skill beside these rather than edit them, and the goal is
     * unbound, so it only ever runs when someone asks for it.
     * <p>
     * That also makes it self-healing. A checkout is free to hand a committed
     * text file back with the platform's line endings, and a {@code vaadin-dev}
     * whose shebang ends {@code \r\n} does not run on Linux or macOS; because
     * any difference is replaced, the next run repairs it.
     *
     * @param targetDirectory
     *            the directory to install into, normally the application
     *            module's base directory
     * @return what was written and what was already up to date
     * @throws IOException
     *             if a file cannot be read or written
     */
    public static Result install(Path targetDirectory) throws IOException {
        List<Path> written = new ArrayList<>();
        List<Path> unchanged = new ArrayList<>();

        for (Map.Entry<String, String> entry : PAYLOAD.entrySet()) {
            String relative = entry.getValue();
            Path target = targetDirectory.resolve(relative);
            if (FileIOUtils.writeIfChanged(target.toFile(),
                    read(entry.getKey()))) {
                written.add(target);
            } else {
                unchanged.add(target);
            }
            setExecutableIfNeeded(relative, target);
        }
        return new Result(List.copyOf(written), List.copyOf(unchanged));
    }

    /**
     * Provisions the HotswapAgent jar into the machine-level dev-loop cache,
     * which is the one thing the loop would otherwise download the first time
     * someone runs {@code vaadin-dev start}.
     * <p>
     * Doing it here rather than there is what makes the loop usable on a
     * machine that has no network access by the time anyone develops on it. The
     * daemon still provisions on demand, through the same code and into the
     * same cache, so this only ever moves the download earlier - it never
     * becomes a second copy or a second version.
     * <p>
     * Nothing is written into the project: the cache is under
     * {@code ~/.vaadin/devloop}, so one download serves every application on
     * the machine and a {@code mvn clean} does not throw it away.
     * <p>
     * The work is done by {@code HotswapAgentJar} inside {@code daemonJar},
     * reached through a throwaway class loader rather than by depending on the
     * daemon from here. Two reasons, and the second is the one that decides it.
     * A plugin dependency is resolved into the plugin realm before any goal
     * runs, so every build - {@code -Pproduction} included, and every Gradle
     * buildscript - would fetch dev-loop tooling nothing in it can call. And
     * the version that matters is the one the project resolves: the CLI runs
     * that daemon, so pre-downloading whatever version another copy of the
     * daemon pins would leave the running daemon asking for a different one and
     * reaching for the network anyway - in exactly the air-gapped container
     * this feature exists for. Reading the pin out of the jar that will run
     * makes the two agree by construction.
     * <p>
     * Reflection is safe here because the daemon is dependency-free, by an
     * enforcer rule in its own module: the platform class loader is a
     * sufficient parent, and using that rather than this module loader also
     * means nothing already in the plugin realm can shadow what is loaded.
     *
     * @param daemonJar
     *            the {@code flow-devloop-daemon} jar the project resolves
     * @param adapter
     *            the plugin adapter to report through
     * @return the provisioned jar, or empty when that daemon is too old to
     *         carry the provisioning code - in which case a warning has been
     *         reported and the first {@code start} downloads as before
     * @throws IOException
     *             if provisioning was attempted and the jar could neither be
     *             found in the cache nor downloaded
     */
    public static Optional<Path> provisionHotswapAgent(Path daemonJar,
            PluginAdapterBase adapter) throws IOException {
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] { daemonJar.toUri().toURL() },
                ClassLoader.getPlatformClassLoader())) {
            Class<?> provisioner;
            try {
                provisioner = loader.loadClass(PROVISIONER);
            } catch (ClassNotFoundException e) {
                LOGGER.debug("No {} in {}", PROVISIONER, daemonJar, e);
                adapter.logWarn("The dev-loop daemon in " + daemonJar
                        + " is older than this plugin and cannot provision "
                        + "HotswapAgent, so the first `vaadin-dev start` will "
                        + "download it and will need network access to do so");
                return Optional.empty();
            }
            // Parameter and return type both come from java.base, so the two
            // class loaders agree on them with nothing to adapt between them.
            Consumer<String> progress = adapter::logInfo;
            String version = (String) provisioner.getField("VERSION").get(null);
            Path jar = (Path) provisioner.getMethod("provision", Consumer.class)
                    .invoke(null, progress);
            adapter.logInfo("HotswapAgent " + version + " is ready at " + jar
                    + " - the dev loop needs no network for it");
            return Optional.of(jar);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException failure) {
                throw failure;
            }
            if (cause instanceof RuntimeException failure) {
                throw failure;
            }
            throw new IOException("provisioning HotswapAgent through "
                    + daemonJar + " failed: " + cause, cause);
        } catch (ReflectiveOperationException e) {
            throw new IOException("the dev-loop daemon in " + daemonJar
                    + " does not expose the provisioning API this plugin "
                    + "expects", e);
        }
    }

    /**
     * A resource stream does not carry the executable bit, and a
     * {@code vaadin-dev} nobody can run is a confusing way to fail. A no-op on
     * a file system without POSIX permissions, which is every Windows one.
     * <p>
     * The owner bit only. That is the one the developer who ran the install
     * needs, and it is also the only one Git records: a checkout of the
     * committed script gets its execute bits from the cloning user's umask, so
     * widening them here would grant nothing that lasts.
     */
    private static void setExecutableIfNeeded(String relative, Path target) {
        if (!EXECUTABLE.contains(relative)) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = new java.util.HashSet<>(
                    Files.getPosixFilePermissions(target));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(target, permissions);
        } catch (UnsupportedOperationException | IOException e) {
            LOGGER.debug("Could not set the executable bit on {}", target, e);
        }
    }

    private static String read(String resource) throws IOException {
        String path = RESOURCE_ROOT + resource;
        try (InputStream in = DevCliInstaller.class.getClassLoader()
                .getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("the vaadin-dev CLI resource " + path
                        + " is missing "
                        + "from flow-plugin-base; this is a build error");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Reports what an installation did, in the order a reader wants it: what
     * changed, then what was left alone and why.
     * <p>
     * Through the plugin adapter rather than a logger of its own, so the output
     * lands in the build log the developer is already reading - the same route
     * {@code BuildFrontendUtil} uses.
     *
     * @param result
     *            the installation result
     * @param targetDirectory
     *            the directory installed into, for relative reporting
     * @param adapter
     *            the plugin adapter to report through
     */
    public static void report(Result result, Path targetDirectory,
            PluginAdapterBase adapter) {
        result.written().forEach(path -> adapter
                .logInfo("Installed " + relative(targetDirectory, path)));
        if (!result.unchanged().isEmpty()) {
            adapter.logInfo(
                    result.unchanged().size() + " file(s) already up to date");
        }
        if (!result.written().isEmpty()) {
            adapter.logInfo(
                    "Commit .vaadin/, .agents/skills/ and .claude/skills/ - they are project tooling, like mvnw");
        }
    }

    private static String relative(Path base, Path path) {
        try {
            return base.relativize(path).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return path.toString();
        }
    }
}
