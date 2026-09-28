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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The workspace details a Quarkus build only has during code generation are
 * written here and read back by a later build step, so what
 * {@link WorkspaceInfo#load(Path)} returns has to describe the same module
 * {@link WorkspaceInfo#save} was given.
 */
class WorkspaceInfoTest {

    @TempDir
    Path workDir;

    @TempDir
    Path projectDir;

    @Test
    void saveAndLoad_moduleWithSources_roundTripsTheModule() throws Exception {
        Path sources = projectDir.resolve("src/main/java");
        Path resources = projectDir.resolve("src/main/resources");
        Path classes = projectDir.resolve("target/classes");

        WorkspaceInfo.save(module(sources, resources, classes), workDir);
        WorkspaceModule loaded = WorkspaceInfo.load(workDir);

        assertNotNull(loaded);
        assertEquals("com.example", loaded.getId().getGroupId());
        assertEquals("demo", loaded.getId().getArtifactId());
        assertEquals("1.0", loaded.getId().getVersion());
        assertEquals(projectDir.toFile().getAbsolutePath(),
                loaded.getModuleDir().getAbsolutePath());
        assertEquals(projectDir.resolve("target").toFile().getAbsolutePath(),
                loaded.getBuildDir().getAbsolutePath());

        assertTrue(loaded.hasMainSources());
        assertEquals(List.of(sources.toFile().getAbsolutePath()),
                loaded.getMainSources().getSourceDirs().stream()
                        .map(dir -> dir.getDir().toFile().getAbsolutePath())
                        .toList());
        assertEquals(List.of(resources.toFile().getAbsolutePath()),
                loaded.getMainSources().getResourceDirs().stream()
                        .map(dir -> dir.getDir().toFile().getAbsolutePath())
                        .toList());
        assertEquals(classes.toFile().getAbsolutePath(),
                loaded.getMainSources().getSourceDirs().iterator().next()
                        .getOutputDir().toFile().getAbsolutePath());
    }

    @Test
    void saveAndLoad_moduleWithoutSources_roundTripsWithoutSourceDirs()
            throws Exception {
        WorkspaceModule module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("com.example", "demo", "1.0"))
                .setModuleDir(projectDir)
                .setBuildDir(projectDir.resolve("target")).build();
        assertFalse(module.hasMainSources(),
                "the fixture is meant to have no sources");

        WorkspaceInfo.save(module, workDir);
        WorkspaceModule loaded = WorkspaceInfo.load(workDir);

        assertNotNull(loaded);
        assertEquals("demo", loaded.getId().getArtifactId());
        assertTrue(loaded.getMainSources().getSourceDirs().isEmpty());
        assertTrue(loaded.getMainSources().getResourceDirs().isEmpty());
    }

    @Test
    void load_nothingSaved_answersNull() {
        assertNull(WorkspaceInfo.load(workDir),
                "a build that never ran code generation has no workspace "
                        + "information, and that is not an error");
    }

    @Test
    void save_calledTwice_leavesTheLatestInformation() throws Exception {
        WorkspaceInfo.save(module(projectDir.resolve("src/main/java"),
                projectDir.resolve("src/main/resources"),
                projectDir.resolve("target/classes")), workDir);

        Path otherProject = projectDir.resolve("other");
        WorkspaceInfo.save(
                WorkspaceModule.builder()
                        .setModuleId(WorkspaceModuleId.of("com.example",
                                "other", "2.0"))
                        .setModuleDir(otherProject)
                        .setBuildDir(otherProject.resolve("target")).build(),
                workDir);

        assertEquals("other",
                WorkspaceInfo.load(workDir).getId().getArtifactId());
    }

    @Test
    void save_writesASingleFileIntoTheWorkDirectory() throws Exception {
        WorkspaceInfo.save(module(projectDir.resolve("src/main/java"),
                projectDir.resolve("src/main/resources"),
                projectDir.resolve("target/classes")), workDir);

        try (var files = Files.list(workDir)) {
            assertEquals(1, files.count(),
                    "save() is expected to write exactly one file");
        }
    }

    private WorkspaceModule module(Path sources, Path resources, Path classes)
            throws IOException {
        Files.createDirectories(sources);
        Files.createDirectories(resources);
        Files.createDirectories(classes);
        return WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("com.example", "demo", "1.0"))
                .setModuleDir(projectDir)
                .setBuildDir(projectDir.resolve("target"))
                .addArtifactSources(
                        new DefaultArtifactSources(ArtifactSources.MAIN,
                                List.of(SourceDir.of(sources, classes)),
                                List.of(SourceDir.of(resources, classes))))
                .build();
    }

    @Test
    void load_documentWrittenBeforeSourceDirsWereAlwaysWritten_readsIt()
            throws Exception {
        // An older build wrote null for a module without main sources. Such a
        // file can still be sitting in a work directory, and has to load
        // rather than fail on the missing lists.
        WorkspaceInfo.save(module(projectDir.resolve("src/main/java"),
                projectDir.resolve("src/main/resources"),
                projectDir.resolve("target/classes")), workDir);
        Path projectInfoFile;
        try (var files = Files.list(workDir)) {
            projectInfoFile = files.findFirst().orElseThrow();
        }
        Files.writeString(projectInfoFile, """
                {"groupId":"com.example","artifactId":"legacy","version":"1.0",\
                "moduleDir":"%s","buildDir":"%s",\
                "sourceDirs":null,"resourceDirs":null}""".formatted(
                projectDir.toFile().getAbsolutePath().replace("\\", "\\\\"),
                projectDir.resolve("target").toFile().getAbsolutePath()
                        .replace("\\", "\\\\")));

        WorkspaceModule loaded = WorkspaceInfo.load(workDir);

        assertNotNull(loaded);
        assertEquals("legacy", loaded.getId().getArtifactId());
        assertTrue(loaded.getMainSources().getSourceDirs().isEmpty());
        assertTrue(loaded.getMainSources().getResourceDirs().isEmpty());
    }
}
