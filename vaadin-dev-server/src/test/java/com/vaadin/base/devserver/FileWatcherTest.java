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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWatcherTest {

    @TempDir
    File temporaryFolder;

    @Test
    void fileWatcherTriggeredForModification() throws Exception {
        AtomicReference<File> changed = new AtomicReference<>();

        File dir = new File(temporaryFolder, "watched");

        dir.mkdirs();
        FileWatcher watcher = new FileWatcher(file -> {
            changed.set(file);
        }, dir);

        watcher.start();

        try {
            File newFile = new File(dir, "newFile.txt");
            newFile.createNewFile();

            Thread.sleep(50); // The watcher is supposed to be triggered
                              // immediately
            assertEquals(newFile, changed.get());
        } finally {
            watcher.stop();
        }
    }

    @Test
    void externalDependencyWatcher_setViaParameter_TriggeredForModification()
            throws Exception {
        File projectFolder = new File(temporaryFolder, "projectFolder");
        projectFolder.mkdirs();

        String metaInf = "/src/main/resources/META-INF/";
        String rootProjectResourceFrontend = projectFolder.getAbsolutePath()
                + metaInf + "resources/frontend";
        String subProjectLegacyFrontend = projectFolder.getAbsolutePath()
                + "/fakeproject" + metaInf + "frontend";

        new File(rootProjectResourceFrontend).mkdirs();
        new File(subProjectLegacyFrontend).mkdirs();

        File jarFrontendResources = new File(temporaryFolder,
                "jarFrontendResources");
        jarFrontendResources.mkdirs();

        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getStringProperty(
                InitParameters.FRONTEND_HOTDEPLOY_DEPENDENCIES, null))
                .thenReturn("./,./fakeproject");
        Mockito.when(config.getProjectFolder()).thenReturn(projectFolder);

        try (MockedStatic<ApplicationConfiguration> appConfig = Mockito
                .mockStatic(ApplicationConfiguration.class)) {
            appConfig.when(() -> ApplicationConfiguration.get(Mockito.any()))
                    .thenReturn(config);
            try (var watcher = new ExternalDependencyWatcher(vaadinContext,
                    jarFrontendResources)) {

                assertFileCountFound(jarFrontendResources, 0);

                createFile(rootProjectResourceFrontend + "/somestyles.css");
                assertFileCountFound(jarFrontendResources, 1);

                createFile(subProjectLegacyFrontend + "/somejs.js");
                assertFileCountFound(jarFrontendResources, 2);

                // Map files as listFiles makes no promises on ordering
                List<String> frontendFiles = Arrays
                        .stream(jarFrontendResources.listFiles())
                        .map(File::getName).toList();
                assertTrue(frontendFiles.contains("somestyles.css"),
                        "No 'somestyles.css' file found");
                assertTrue(frontendFiles.contains("somejs.js"),
                        "No 'somejs.js' file found");
            }
        }
    }

    @Test
    void externalDependencyWatcher_setAsDefaultForRunnerProjectButNotSubProject_TriggeredForModification()
            throws Exception {
        File projectFolder = new File(temporaryFolder, "projectFolder");
        projectFolder.mkdirs();

        String metaInf = "/src/main/resources/META-INF/";
        String rootPorjectResourceFrontend = projectFolder.getAbsolutePath()
                + metaInf + "resources/frontend";
        String subProjectLegacyFrontend = projectFolder.getAbsolutePath()
                + "/fakeproject" + metaInf + "frontend";

        new File(rootPorjectResourceFrontend).mkdirs();
        new File(subProjectLegacyFrontend).mkdirs();

        File jarFrontendResources = new File(temporaryFolder,
                "jarFrontendResources");
        jarFrontendResources.mkdirs();

        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getStringProperty(
                InitParameters.FRONTEND_HOTDEPLOY_DEPENDENCIES, null))
                .thenReturn(null);
        Mockito.when(config.getProjectFolder()).thenReturn(projectFolder);

        try (MockedStatic<ApplicationConfiguration> appConfig = Mockito
                .mockStatic(ApplicationConfiguration.class)) {
            appConfig.when(() -> ApplicationConfiguration.get(Mockito.any()))
                    .thenReturn(config);
            try (var watcher = new ExternalDependencyWatcher(vaadinContext,
                    jarFrontendResources)) {

                assertFileCountFound(jarFrontendResources, 0);

                createFile(rootPorjectResourceFrontend + "/somestyles.css");
                assertFileCountFound(jarFrontendResources, 1);

                createFile(subProjectLegacyFrontend + "/somejs.js");
                assertFileCountFound(jarFrontendResources, 1);

                assertEquals("somestyles.css",
                        jarFrontendResources.listFiles()[0].getName());
            }
        }
    }

    private void assertFileCountFound(File directory, int count) {
        Awaitility.await().untilAsserted(directory::listFiles, files -> {
            assertEquals(count, files.length,
                    "Wrong amount of copied files found when there should be "
                            + count + ". Current files were: "
                            + Arrays.toString(files));
        });

    }

    private void createFile(String path) throws IOException {
        File newFile = new File(path);
        Files.writeString(newFile.toPath(), "some text");
    }
}
