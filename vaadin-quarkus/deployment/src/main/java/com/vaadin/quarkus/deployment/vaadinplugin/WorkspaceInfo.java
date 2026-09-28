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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * The WorkspaceInfo class provides methods for saving and loading project
 * workspace information related to a quarkus application. This information
 * includes details about the module's directory, build directory, source
 * directories, and resource directories. The class uses JSON-based
 * serialization and deserialization for the persistence of workspace
 * information.
 */
class WorkspaceInfo {

    private record ProjectInfo(String groupId, String artifactId,
            String version, String moduleDir, String buildDir,
            List<SourceDirInfo> sourceDirs, List<SourceDirInfo> resourceDirs) {

        WorkspaceModuleId moduleId() {
            return WorkspaceModuleId.of(groupId, artifactId, version);
        }
    }

    private record SourceDirInfo(String dir, String outputDir) {
        SourceDirInfo(SourceDir sourceDir) {
            this(sourceDir.getDir().toFile().getAbsolutePath(),
                    sourceDir.getOutputDir().toFile().getAbsolutePath());
        }
    }

    /**
     * Saves workspace information of the given module to the specified working
     * directory.
     *
     * @param module
     *            the workspace module containing the information to be saved
     * @param workDir
     *            the directory where the workspace information will be written
     *            to
     * @throws Exception
     *             if an error occurs while writing the workspace information
     */
    static void save(WorkspaceModule module, Path workDir) throws Exception {
        Path projectInfoFile = resolveProjectInfoFile(workDir);
        var info = collectWorkspaceInfo(module);
        // The default options, which are CREATE and TRUNCATE_EXISTING. Asking
        // for CREATE alone keeps whatever an earlier run wrote and overwrites
        // it from the start, so a second pass over the same work directory -
        // a dev mode restart, say - leaves the tail of the longer previous
        // document behind and the file no longer parses.
        Files.writeString(projectInfoFile,
                JacksonUtils.getMapper().writeValueAsString(info));
    }

    /**
     * Loads a {@code WorkspaceModule} instance from the specified working
     * directory. If the project information file exists, it reads the project
     * details and constructs a {@code WorkspaceModule} object. If the file does
     * not exist, it returns {@code null}.
     *
     * @param workDir
     *            the directory where the project information file is located
     * @return a {@code WorkspaceModule} object built from the project
     *         information file, or {@code null} if the file does not exist
     */
    static WorkspaceModule load(Path workDir) {
        Path projectInfoFile = resolveProjectInfoFile(workDir);
        if (Files.exists(projectInfoFile)) {
            var info = JacksonUtils.getMapper()
                    .readValue(projectInfoFile.toFile(), ProjectInfo.class);
            return WorkspaceModule.builder().setModuleId(info.moduleId())
                    .setModuleDir(Path.of(info.moduleDir()))
                    .setBuildDir(Path.of(info.buildDir()))
                    .addArtifactSources(new DefaultArtifactSources(
                            ArtifactSources.MAIN, toSourceDirs(info.sourceDirs),
                            toSourceDirs(info.resourceDirs)))
                    .build();
        }
        return null;
    }

    private static Path resolveProjectInfoFile(Path workDir) {
        return workDir.resolve("vaadin-plugin-project-info.txt");
    }

    /**
     * Converts the persisted directory entries into source dirs, tolerating the
     * null an older build wrote for a module without main sources.
     *
     * @param dirs
     *            the persisted entries, possibly null
     * @return the source dirs, empty if there were none
     */
    private static List<SourceDir> toSourceDirs(List<SourceDirInfo> dirs) {
        if (dirs == null) {
            return List.of();
        }
        return dirs.stream().map(
                d -> SourceDir.of(Path.of(d.dir()), Path.of(d.outputDir())))
                .toList();
    }

    private static ProjectInfo collectWorkspaceInfo(WorkspaceModule module) {
        // Empty rather than null for a module without main sources, so that
        // what is written back can be read again.
        List<SourceDirInfo> sourceDirs = List.of();
        List<SourceDirInfo> resourceDirs = List.of();
        if (module.hasMainSources()) {
            sourceDirs = module.getMainSources().getSourceDirs().stream()
                    .map(SourceDirInfo::new).collect(Collectors.toList());
            resourceDirs = module.getMainSources().getResourceDirs().stream()
                    .map(SourceDirInfo::new).collect(Collectors.toList());
        }
        WorkspaceModuleId moduleId = module.getId();
        return new ProjectInfo(moduleId.getGroupId(), moduleId.getArtifactId(),
                moduleId.getVersion(), module.getModuleDir().getAbsolutePath(),
                module.getBuildDir().getAbsolutePath(), sourceDirs,
                resourceDirs);
    }

}
