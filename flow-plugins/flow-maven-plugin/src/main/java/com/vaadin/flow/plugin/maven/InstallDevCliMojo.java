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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.plugin.base.DevCliInstaller;

/**
 * Installs the {@code vaadin-dev} CLI and its agent skills into the project.
 * <p>
 * Creates {@code .vaadin/vaadin-dev} (plus the {@code .ps1} and {@code .cmd}
 * launchers), the shared instructions under
 * {@code .agents/skills/vaadin-devloop/} and the Claude adapter under
 * {@code .claude/skills/vaadin-devloop/}. No jars are staged into the project
 * and no agent configuration is touched - the CLI resolves the dev-loop daemon
 * from the project's own dependencies at first use.
 * <p>
 * It also provisions HotswapAgent into {@code ~/.vaadin/devloop}, which is the
 * only asset the loop downloads rather than resolving from a Maven repository.
 * That is deliberately the goal's job and not the first {@code vaadin-dev
 * start}: an image built with network access and developed in without it - a
 * container, a sandboxed agent environment - would otherwise install fine and
 * then fail to start the loop at all. Once the goal has succeeded, nothing in
 * the loop needs the network again. Machine-level, so nothing lands in the
 * project and one download serves every application on the machine.
 * <p>
 * The provisioning runs out of the daemon jar the project itself resolves - the
 * same one the CLI will run - so the pinned agent version is the version the
 * running daemon goes on to ask for. Hence the dependency resolution this goal
 * requires, and hence no scope filter when looking for it: the daemon travels
 * in as an optional dependency of the dev server, and a project is free to
 * declare it provided or test instead.
 * <p>
 * Everything it writes is meant to be committed, like {@code mvnw}: it is
 * project tooling, and the point is that every developer and every agent on the
 * repository gets the same instructions. It is also rewritten whenever it
 * differs from the shipped copy, so a fix reaches a project by re-running the
 * goal - add your own skill beside these rather than editing them.
 * <p>
 * Unbound, so it never runs as a side effect of a normal build. Run it on the
 * application module: in a reactor that means {@code -pl :app}, which is also
 * where the CLI's own {@code .vaadin/vaadin-dev} invocations are rooted.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@Mojo(name = "install-dev-cli", requiresDependencyResolution = ResolutionScope.TEST)
public class InstallDevCliMojo extends FlowModeAbstractMojo {

    /**
     * Where to install. Defaults to the module the goal runs on, which is what
     * puts the two skill trees beside each other so the Claude adapter's
     * relative link to the shared instructions resolves. Point it at the
     * reactor root if the agent trees belong there instead.
     */
    @Parameter(property = "vaadin.devcli.targetDirectory", defaultValue = "${project.basedir}")
    private File targetDirectory;

    /**
     * Installs the project files only, leaving HotswapAgent to be downloaded by
     * the first {@code vaadin-dev start} that needs it.
     * <p>
     * For a run that has no network and does not need one to succeed. Skipping
     * it gives up the guarantee the provisioning exists for: that the loop
     * starts afterwards without reaching the network.
     */
    @Parameter(property = "vaadin.devcli.skipHotswapAgent", defaultValue = "false")
    private boolean skipHotswapAgent;

    @Override
    protected void executeInternal() throws MojoFailureException {
        Path target = targetDirectory.toPath();
        try {
            DevCliInstaller.report(DevCliInstaller.install(target), target,
                    this);
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Could not install the vaadin-dev CLI into " + target, e);
        }
        // After the files: they are the part of the install that cannot fail
        // for a reason outside the machine, so an unreachable GitHub still
        // leaves a project with a working CLI to report the problem from.
        if (skipHotswapAgent) {
            logInfo("Skipping HotswapAgent; the first `vaadin-dev start` will "
                    + "download it, and will need network access to do so");
            return;
        }
        Optional<Path> daemon = daemonJar();
        if (daemon.isEmpty()) {
            // Not a failure: the scripts are installed and correct, and the
            // CLI itself reports this with the same remedy the moment it is
            // run. Saying it now saves finding out then.
            logWarn("This project does not depend on the dev-loop daemon, so "
                    + "there was no HotswapAgent to provision - and "
                    + "`vaadin-dev` cannot run either until it does. Add "
                    + "com.vaadin:vaadin-dev (optional) to the project.");
            return;
        }
        try {
            DevCliInstaller.provisionHotswapAgent(daemon.get(), this);
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Could not provision HotswapAgent, so the dev loop would "
                            + "have to download it the first time it runs: "
                            + e.getMessage()
                            + ". It is fetched once per machine into "
                            + "~/.vaadin/devloop from a HotswapAgent GitHub "
                            + "release, so run this goal somewhere that is "
                            + "reachable, or put the jar there by hand - it is "
                            + "verified against a pinned checksum either way. "
                            + "Pass -Dvaadin.devcli.skipHotswapAgent=true to "
                            + "install the CLI without it.",
                    e);
        }
    }

    /**
     * The dev-loop daemon jar among the project dependencies, which is the one
     * the CLI resolves and runs.
     * <p>
     * By coordinates rather than by file name, and with no scope filter, so
     * that it is found however the project came to declare it.
     */
    private Optional<Path> daemonJar() {
        return project.getArtifacts().stream()
                .filter(artifact -> "com.vaadin".equals(artifact.getGroupId())
                        && "flow-devloop-daemon"
                                .equals(artifact.getArtifactId()))
                .map(Artifact::getFile).filter(Objects::nonNull)
                .filter(File::isFile).map(File::toPath).findFirst();
    }
}
