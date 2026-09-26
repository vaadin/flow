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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The daemon's side of the file the build extension writes. Its whole value is
 * that it is Maven's answer rather than the pom reader's guess, so the two
 * things worth pinning are that it is read exactly as written - and that it is
 * dropped the moment a pom is newer than it, because a stale answer from this
 * file would outrank a current one from the poms.
 */
class EffectiveModelTest {

    @TempDir
    private Path module;

    @Test
    void aPluginIsReadBackWithItsVersionAndConfiguration() throws IOException {
        write("""
                plugins=1
                plugin.0=org.eclipse.jetty.ee11\\:jetty-ee11-maven-plugin\\:12.1.13
                plugin.0.scan=2
                profiles=jetty
                """);

        Reactor.PluginConfig plugin = model().orElseThrow()
                .plugin("org.eclipse.jetty.ee11", "jetty-ee11-maven-plugin")
                .orElseThrow();

        assertEquals("org.eclipse.jetty.ee11:jetty-ee11-maven-plugin:12.1.13",
                plugin.coordinates());
        assertEquals(Optional.of("2"), plugin.configured("scan"));
        assertEquals(List.of("jetty"), model().orElseThrow().activeProfiles());
    }

    /**
     * Maven leaves the version out when the project does; the daemon then names
     * the goal without one and lets Maven pin it, as it would for any other
     * invocation.
     */
    @Test
    void aPluginWithNoVersionIsReadAsHavingNone() throws IOException {
        write("""
                plugins=1
                plugin.0=org.eclipse.jetty.ee10\\:jetty-ee10-maven-plugin\\:
                """);

        assertEquals("org.eclipse.jetty.ee10:jetty-ee10-maven-plugin",
                model().orElseThrow()
                        .plugin("org.eclipse.jetty.ee10",
                                "jetty-ee10-maven-plugin")
                        .orElseThrow().coordinates());
    }

    /**
     * A build that runs no server plugin is an answer, not a gap: it is what
     * says the application has an entry point of its own.
     */
    @Test
    void aPluginTheBuildDoesNotRunIsAbsent() throws IOException {
        write("""
                plugins=1
                plugin.0=org.apache.maven.plugins\\:maven-compiler-plugin\\:3.13.0
                """);

        assertTrue(model().orElseThrow()
                .plugin("org.eclipse.jetty.ee10", "jetty-ee10-maven-plugin")
                .isEmpty());
    }

    @Test
    void noFileIsNoAnswer() {
        assertTrue(model().isEmpty());
    }

    /**
     * The one thing the whole file rests on: the extension writes where this
     * reads. Both ends spell the path out as a literal - the daemon may not
     * load a class compiled against Maven's API - so nothing but this keeps
     * them from drifting apart, and drifting apart would read as "no model"
     * rather than as any kind of failure.
     */
    @Test
    void theExtensionWritesWhereThisReads() {
        assertEquals(DevLoopBuildExtension.MODEL_FILE, EffectiveModel.FILE);
    }

    /**
     * The pom is what moves the answer, so a pom newer than the file means the
     * file describes a project that no longer exists. Falling back to reading
     * the poms is right then: they are stale by nothing.
     */
    @Test
    void aModelOlderThanThePomIsIgnored() throws IOException {
        write("""
                plugins=1
                plugin.0=org.eclipse.jetty.ee11\\:jetty-ee11-maven-plugin\\:12.1.13
                """);
        Path pom = module.resolve("pom.xml");
        Files.writeString(pom, "<project/>");
        Files.setLastModifiedTime(pom,
                FileTime.fromMillis(Files
                        .getLastModifiedTime(
                                module.resolve(EffectiveModel.FILE))
                        .toMillis() + 2000));

        assertTrue(EffectiveModel.read(module, List.of(pom)).isEmpty());
    }

    /** A file cut short by a build that died mid-write is not a failure. */
    @Test
    void anUnreadableCountLeavesNoPlugins() throws IOException {
        write("plugins=not-a-number\n");

        assertTrue(model().orElseThrow()
                .plugin("org.eclipse.jetty.ee11", "jetty-ee11-maven-plugin")
                .isEmpty());
    }

    private Optional<EffectiveModel> model() {
        return EffectiveModel.read(module, List.of());
    }

    private void write(String contents) throws IOException {
        Path file = module.resolve(EffectiveModel.FILE);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }
}
