/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Watches external dependency folders for changes and copies modified files to
 * the JAR frontend resources folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ExternalDependencyWatcher implements Closeable {

    final private static Set<FileWatcher> watchers = new HashSet<>();

    public ExternalDependencyWatcher(VaadinContext context,
            File jarFrontendResourcesFolder) {
        ApplicationConfiguration config = ApplicationConfiguration.get(context);

        String hotdeployDependenciesProperty = config.getStringProperty(
                InitParameters.FRONTEND_HOTDEPLOY_DEPENDENCIES, null);

        List<String> hotdeployDependencyFolders = new ArrayList<>();
        File projectFolder = config.getProjectFolder();
        if (hotdeployDependenciesProperty != null) {
            for (String folder : hotdeployDependenciesProperty.split(",")) {
                if (!folder.isBlank()) {
                    hotdeployDependencyFolders.add(folder.trim());
                }
            }
        } else {
            // Always watch src/main/resources/META-INF from active project
            hotdeployDependencyFolders.add(projectFolder.getAbsolutePath());

            File pomFile = new File(projectFolder, "pom.xml");
            File parentPomFile = MavenUtils
                    .getParentPomOfMultiModuleProject(pomFile);
            if (parentPomFile != null) {
                Document parentPom = MavenUtils.parsePomFile(parentPomFile);
                if (parentPom != null) {
                    Path currentPomToParentPomPath = pomFile.getParentFile()
                            .toPath()
                            .relativize(parentPomFile.getParentFile().toPath());
                    hotdeployDependencyFolders = MavenUtils
                            .getModuleFolders(parentPom).stream()
                            .map(folder -> currentPomToParentPomPath
                                    + File.separator + folder)
                            .toList();
                }
            }
        }

        for (String hotdeployDependencyFolder : hotdeployDependencyFolders) {
            Path moduleFolder = projectFolder.toPath()
                    .resolve(hotdeployDependencyFolder).normalize();
            Path metaInf = moduleFolder
                    .resolve(Path.of("src", "main", "resources", "META-INF"));
            if (!watchDependencyFolder(metaInf.toFile(),
                    jarFrontendResourcesFolder)
                    && hotdeployDependenciesProperty != null) {
                getLogger().warn("No folders to watch were found in "
                        + metaInf.normalize().toAbsolutePath()
                        + ". This should be the META-INF folder that contains either frontend or resources/frontend");
            }
        }
    }

    /**
     * Starts watching the given META-INF folders for changes.
     *
     * @param metaInfFolder
     *            the folder to watch
     * @param jarFrontendResourcesFolder
     *            the jar frontend resource folder to copy changed files to
     * @return true if at least one folder is watched, false if no folders to
     *         watch were found
     */
    private boolean watchDependencyFolder(File metaInfFolder,
            File jarFrontendResourcesFolder) {
        File metaInfFrontend = new File(metaInfFolder, "frontend");
        File metaInfResourcesFrontend = new File(
                new File(metaInfFolder, "resources"), "frontend");
        File metaInfResourcesThemes = new File(
                new File(metaInfFolder, "resources"), "themes");

        boolean watching1 = watchAndCopy(metaInfFrontend,
                jarFrontendResourcesFolder);
        boolean watching2 = watchAndCopy(metaInfResourcesFrontend,
                jarFrontendResourcesFolder);
        boolean watching3 = watchAndCopy(metaInfResourcesThemes,
                new File(jarFrontendResourcesFolder, "themes"));

        return watching1 || watching2 || watching3;
    }

    private boolean watchAndCopy(File watchFolder, File targetFolder) {
        if (!watchFolder.exists()) {
            return false;
        }

        try {
            FileWatcher watcher = new FileWatcher(updatedFile -> {
                if (FileIOUtils.isProbablyTemporaryFile(updatedFile)) {
                    return;
                }
                Path pathInsideWatchFolder = watchFolder.toPath()
                        .relativize(updatedFile.toPath());
                Path target = targetFolder.toPath()
                        .resolve(pathInsideWatchFolder);
                try {
                    Files.copy(updatedFile.toPath(), target,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (NoSuchFileException e) {
                    // This happens if an editor creates temporary files and
                    // they are removed before copy is called
                } catch (IOException e) {
                    getLogger().warn("Unable to copy modified file from "
                            + updatedFile + " to " + target, e);
                }
            }, watchFolder);
            watcher.start();
            watchers.add(watcher);
            getLogger().debug("Watching {} for frontend file changes",
                    watchFolder);
            return true;
        } catch (Exception e) {
            getLogger().error("Unable to start file watcher for " + watchFolder,
                    e);
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        for (FileWatcher watcher : watchers) {
            try {
                watcher.stop();
            } catch (IOException e) {
                getLogger().error("Unable to stop file watcher", e);
            }
        }
        watchers.clear();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
