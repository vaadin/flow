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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The installer writes project tooling, and the promise is that re-running the
 * goal gets you the shipped version - so what it does on a second run matters
 * as much as on the first.
 */
public class DevCliInstallerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
}
