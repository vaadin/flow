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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.flow.devloop.daemon.HotswapAgentJar;
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
@Mojo(name = "install-dev-cli")
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
        try {
            DevCliInstaller.provisionHotswapAgent(this);
        } catch (IOException e) {
            throw new MojoFailureException("Could not provision HotswapAgent "
                    + HotswapAgentJar.VERSION + " into "
                    + HotswapAgentJar.cacheDir()
                    + ", so the dev loop would have to download it on first "
                    + "use. Run this goal where " + HotswapAgentJar.URL
                    + " is reachable, or place that file in the cache "
                    + "directory by hand - it is verified against a pinned "
                    + "checksum either way. Pass "
                    + "-Dvaadin.devcli.skipHotswapAgent=true to install the "
                    + "CLI without it.", e);
        }
    }
}
