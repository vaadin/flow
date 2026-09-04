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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The installer writes project tooling, and the promise is that re-running the
 * goal gets you the shipped version - so what it does on a second run matters
 * as much as on the first.
 * <p>
 * The provisioning tests build their own daemon jar rather than using the real
 * one, and that is the point: this module deliberately does not depend on the
 * daemon, so all that holds the two sides together is the name of a class, a
 * field and a method signature. A test that imported the real class would not
 * be checking that at all.
 */
public class DevCliInstallerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final List<String> info = new ArrayList<>();

    private final List<String> warnings = new ArrayList<>();

    private static final List<String> INSTALLED = List.of(".vaadin/vaadin-dev",
            ".vaadin/vaadin-dev.ps1", ".vaadin/vaadin-dev.cmd",
            ".vaadin/.gitignore", ".agents/skills/vaadin-devloop/SKILL.md",
            ".agents/skills/vaadin-devloop/reference.md",
            ".claude/skills/vaadin-devloop/SKILL.md");

    @Test
    public void install_createsExactlyThePayload() throws IOException {
        Path root = temporaryFolder.getRoot().toPath();

        DevCliInstaller.Result result = DevCliInstaller.install(root);

        Assert.assertEquals(INSTALLED.size(), result.written().size());
        for (String relative : INSTALLED) {
            Assert.assertTrue(relative + " was not installed",
                    Files.isRegularFile(root.resolve(relative)));
        }
        // Nothing generated and no jars: the CLI resolves the daemon itself.
        try (var walk = Files.walk(root)) {
            Assert.assertTrue("no jar should be staged into the project",
                    walk.noneMatch(path -> path.toString().endsWith(".jar")));
        }
    }

    @Test
    public void install_theClaudeAdapterLinkResolvesToTheSharedInstructions()
            throws IOException {
        Path root = temporaryFolder.getRoot().toPath();
        DevCliInstaller.install(root);
        Path adapter = root.resolve(".claude/skills/vaadin-devloop/SKILL.md");

        String content = Files.readString(adapter);

        // The whole reason both trees install under one directory: the adapter
        // carries no reference.md of its own and links to the .agents copy.
        String link = "../../../.agents/skills/vaadin-devloop/SKILL.md";
        Assert.assertTrue("the adapter should link to the shared instructions",
                content.contains(link));
        Assert.assertTrue("the relative link should resolve", Files
                .isRegularFile(adapter.getParent().resolve(link).normalize()));
    }

    @Test
    public void install_commandExamplesUseTheInstalledLocation()
            throws IOException {
        Path root = temporaryFolder.getRoot().toPath();
        DevCliInstaller.install(root);

        String skill = Files.readString(
                root.resolve(".agents/skills/vaadin-devloop/SKILL.md"));

        Assert.assertTrue(skill.contains(".vaadin/vaadin-dev status"));
        // No redundant ./ prefix: the path already holds a slash, so a shell
        // never searches PATH for it. And not the pre-move location either.
        Assert.assertFalse(skill.contains("./.vaadin/"));
        Assert.assertFalse(skill.contains(" ./vaadin-dev "));
    }

    @Test
    public void install_isIdempotent() throws IOException {
        Path root = temporaryFolder.getRoot().toPath();
        DevCliInstaller.install(root);

        DevCliInstaller.Result second = DevCliInstaller.install(root);

        Assert.assertEquals(List.of(), second.written());
        Assert.assertEquals(INSTALLED.size(), second.unchanged().size());
    }

    @Test
    public void install_replacesAnEditedFile() throws IOException {
        Path root = temporaryFolder.getRoot().toPath();
        DevCliInstaller.install(root);
        Path skill = root.resolve(".agents/skills/vaadin-devloop/SKILL.md");
        Files.writeString(skill, "my own notes");

        DevCliInstaller.Result result = DevCliInstaller.install(root);

        // These are project tooling, like mvnw: regenerating them is what the
        // goal is for, and it is the only way a fix reaches a project. Someone
        // wanting project-specific instructions adds a skill of their own.
        Assert.assertEquals(List.of(skill), result.written());
        Assert.assertNotEquals("my own notes", Files.readString(skill));
    }

    @Test
    public void install_repairsAFileACheckoutRewroteWithCrLf()
            throws IOException {
        Path root = temporaryFolder.getRoot().toPath();
        DevCliInstaller.install(root);
        Path script = root.resolve(".vaadin/vaadin-dev");
        Files.writeString(script,
                Files.readString(script).replace("\n", "\r\n"));

        DevCliInstaller.Result result = DevCliInstaller.install(root);

        // A committed payload plus a Windows checkout produces exactly this,
        // and
        // a shebang ending in CR does not run on Linux or macOS. Replacing any
        // difference is what makes the goal self-healing here.
        Assert.assertEquals(List.of(script), result.written());
        Assert.assertFalse("the CRLF copy should have been replaced",
                Files.readString(script).contains("\r\n"));
    }

    @Test
    public void install_intoADirectoryThatDoesNotExistYet() throws IOException {
        Path root = temporaryFolder.getRoot().toPath().resolve("app");

        DevCliInstaller.install(root);

        Assert.assertTrue(
                Files.isRegularFile(root.resolve(".vaadin/vaadin-dev")));
    }

    @Test
    public void provisionHotswapAgent_runsTheProvisionerFromTheGivenJar()
            throws Exception {
        Path daemon = daemonJar("""
                package com.vaadin.flow.devloop.daemon;

                public final class HotswapAgentJar {
                    public static final String VERSION = "9.9.9";

                    public static java.nio.file.Path provision(
                            java.util.function.Consumer<String> progress) {
                        progress.accept("provisioning HotswapAgent 9.9.9");
                        return java.nio.file.Path.of("cache", "ha-9.9.9.jar");
                    }
                }
                """);

        Optional<Path> provisioned = DevCliInstaller
                .provisionHotswapAgent(daemon, adapter());

        // The version reported is the one pinned by the jar that was handed
        // over, not one this plugin knows: the CLI runs the daemon the project
        // resolves, so that is the only version worth pre-downloading.
        Assert.assertEquals(Optional.of(Path.of("cache", "ha-9.9.9.jar")),
                provisioned);
        Assert.assertTrue("the download progress should reach the build log",
                info.contains("provisioning HotswapAgent 9.9.9"));
        Assert.assertTrue("the outcome should name the version and the path",
                info.stream().anyMatch(line -> line
                        .startsWith("HotswapAgent 9.9.9 is ready at")));
        Assert.assertEquals(List.of(), warnings);
    }

    @Test
    public void provisionHotswapAgent_daemonTooOldToProvision_warnsAndCarriesOn()
            throws Exception {
        // A daemon from before this feature. Not a failure: the CLI is
        // installed and correct, and the first start downloads as it used to.
        Path daemon = daemonJar(null);

        Optional<Path> provisioned = DevCliInstaller
                .provisionHotswapAgent(daemon, adapter());

        Assert.assertEquals(Optional.empty(), provisioned);
        Assert.assertEquals(1, warnings.size());
        Assert.assertTrue(warnings.get(0),
                warnings.get(0).contains("older than this plugin"));
    }

    /**
     * A jar holding the given compiled source, or an empty one when it is
     * {@code null}.
     */
    private Path daemonJar(String source) throws Exception {
        Path work = temporaryFolder.newFolder().toPath();
        Path jar = work.resolve("flow-devloop-daemon.jar");
        try (JarOutputStream out = new JarOutputStream(
                Files.newOutputStream(jar))) {
            if (source != null) {
                String name = "com/vaadin/flow/devloop/daemon/HotswapAgentJar.class";
                out.putNextEntry(new JarEntry(name));
                Files.copy(compile(work, source).resolve(name),
                        (OutputStream) out);
                out.closeEntry();
            }
        }
        return jar;
    }

    private Path compile(Path work, String source) throws IOException {
        Path java = work.resolve("HotswapAgentJar.java");
        Files.writeString(java, source);
        Path classes = Files.createDirectories(work.resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assert.assertNotNull("a JDK is required to run this test", compiler);
        Assert.assertEquals("the stub daemon should compile", 0, compiler.run(
                null, null, null, "-d", classes.toString(), java.toString()));
        return classes;
    }

    /**
     * A recording adapter. A proxy rather than a mock because only two of the
     * interface methods are reached, and both only to collect a line.
     */
    private PluginAdapterBase adapter() {
        return (PluginAdapterBase) Proxy.newProxyInstance(
                PluginAdapterBase.class.getClassLoader(),
                new Class<?>[] { PluginAdapterBase.class },
                (proxy, method, args) -> {
                    if ("logInfo".equals(method.getName())) {
                        info.add(String.valueOf(args[0]));
                    } else if ("logWarn".equals(method.getName())) {
                        warnings.add(String.valueOf(args[0]));
                    }
                    return null;
                });
    }
}
