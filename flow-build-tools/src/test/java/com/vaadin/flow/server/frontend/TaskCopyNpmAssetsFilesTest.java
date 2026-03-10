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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.DevBundleUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_ASSETS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCopyNpmAssetsFilesTest {

    @TempDir
    File temporaryFolder;

    private Options options;
    private ClassFinder classFinder;
    private FrontendDependenciesScanner scanner;
    private File webappResourcesDirectory;

    // root
    // |
    // |- node_modules
    // | | - test-button
    // | | | - images
    // | | | | image.jpg
    // | | | | image.gif
    // | | | - templates
    // | | | | button.template
    @BeforeEach
    void setUp() throws IOException {
        File nodeModules = new File(temporaryFolder, "node_modules");

        nodeModules.mkdirs();
        File testButtonFolder = new File(nodeModules, "test-button");
        File imageAssets = new File(testButtonFolder, "images");
        File templateAssets = new File(testButtonFolder, "templates");

        imageAssets.mkdirs();
        templateAssets.mkdir();

        Files.write(new File(imageAssets, "image.jpg").toPath(),
                List.of("image file"));
        Files.write(new File(imageAssets, "image.gif").toPath(),
                List.of("gif file"));

        Files.write(new File(templateAssets, "button.template").toPath(),
                List.of("template file"));

        webappResourcesDirectory = new File(temporaryFolder, "webapp");
        File resourceOutputDirectory = new File(temporaryFolder, "resource");
        resourceOutputDirectory.mkdirs();

        classFinder = Mockito.mock(ClassFinder.class);
        scanner = Mockito.mock(FrontendDependenciesScanner.class);

        options = new Options(Mockito.mock(Lookup.class), classFinder,
                temporaryFolder);
        options.withBuildResultFolders(webappResourcesDirectory,
                resourceOutputDirectory)
                .withFrontendDependenciesScanner(scanner);
    }

    @Test
    void assertFolderIsCopied() throws IOException {
        Mockito.when(scanner.getAssets())
                .thenReturn(Map.of("test-button", List.of("images/**:button")));

        TaskCopyNpmAssetsFiles taskCopyNpmAssetsFiles = new TaskCopyNpmAssetsFiles(
                options);
        taskCopyNpmAssetsFiles.execute();

        Set<String> filesInDirectory = getFilesInDirectory(
                webappResourcesDirectory);
        assertEquals(2, filesInDirectory.size());
        assertTrue(filesInDirectory
                .contains("VAADIN/static/assets/button/image.jpg"));
        assertTrue(filesInDirectory
                .contains("VAADIN/static/assets/button/image.gif"));
    }

    @Test
    void copiedFolderStructureIsKept() throws IOException {
        Mockito.when(scanner.getAssets())
                .thenReturn(Map.of("test-button", List.of("**:button")));

        TaskCopyNpmAssetsFiles taskCopyNpmAssetsFiles = new TaskCopyNpmAssetsFiles(
                options);
        taskCopyNpmAssetsFiles.execute();

        Set<String> filesInDirectory = getFilesInDirectory(
                webappResourcesDirectory);
        assertEquals(3, filesInDirectory.size());
        assertTrue(
                filesInDirectory.contains(
                        "VAADIN/static/assets/button/images/image.jpg"),
                "Could not find file images/image.jpg");
        assertTrue(
                filesInDirectory.contains(
                        "VAADIN/static/assets/button/images/image.gif"),
                "Could not find file images/image.gif");
        assertTrue(filesInDirectory.contains(
                "VAADIN/static/assets/button/templates/button.template"),
                "Could not find file templates/button.template");
    }

    @Test
    void singleAssertFromFolderIsCopied() throws IOException {
        Mockito.when(scanner.getAssets()).thenReturn(
                Map.of("test-button", List.of("images/*.jpg:copy")));

        TaskCopyNpmAssetsFiles taskCopyNpmAssetsFiles = new TaskCopyNpmAssetsFiles(
                options);
        taskCopyNpmAssetsFiles.execute();

        Set<String> filesInDirectory = getFilesInDirectory(
                webappResourcesDirectory);
        assertEquals(1, filesInDirectory.size());
        assertEquals("VAADIN/static/assets/copy/image.jpg",
                filesInDirectory.iterator().next());
    }

    @Test
    void allAssetsAreCopied() throws IOException {
        Mockito.when(scanner.getAssets()).thenReturn(Map.of("test-button",
                Arrays.asList("images/**:button", "templates/**:button")));

        TaskCopyNpmAssetsFiles taskCopyNpmAssetsFiles = new TaskCopyNpmAssetsFiles(
                options);
        taskCopyNpmAssetsFiles.execute();

        Set<String> filesInDirectory = getFilesInDirectory(
                webappResourcesDirectory);
        assertEquals(3, filesInDirectory.size());
        assertTrue(filesInDirectory
                .contains("VAADIN/static/assets/button/image.jpg"));
        assertTrue(filesInDirectory
                .contains("VAADIN/static/assets/button/image.gif"));
        assertTrue(filesInDirectory
                .contains("VAADIN/static/assets/button/button.template"));
    }

    @Test
    void devBundleCopiesAssetsToCorrectFolder() throws IOException {
        Mockito.when(scanner.getAssets()).thenReturn(
                Map.of("test-button", List.of("templates/**:button")));
        options.withBundleBuild(true).withProductionMode(false)
                .withBuildDirectory("target");

        TaskCopyNpmAssetsFiles taskCopyNpmAssetsFiles = new TaskCopyNpmAssetsFiles(
                options);
        taskCopyNpmAssetsFiles.execute();

        File devBundleTarget = new File(
                DevBundleUtils.getDevBundleFolder(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                "webapp/" + VAADIN_STATIC_ASSETS_PATH);

        Set<String> filesInDirectory = getFilesInDirectory(devBundleTarget);
        assertEquals(1, filesInDirectory.size());
        assertEquals("button/button.template",
                filesInDirectory.iterator().next());
    }

    @Test
    void noAssetsCopiedWhenCopyFlagFalse() throws IOException {
        Mockito.when(scanner.getAssets()).thenReturn(Map.of("test-button",
                Arrays.asList("images/**:button", "templates/**:button")));
        options.setCopyAssets(false);

        TaskCopyNpmAssetsFiles taskCopyNpmAssetsFiles = new TaskCopyNpmAssetsFiles(
                options);
        taskCopyNpmAssetsFiles.execute();

        Set<String> filesInDirectory = getFilesInDirectory(
                webappResourcesDirectory);
        assertEquals(0, filesInDirectory.size(),
                "Nothing should be copied for CopyAssets false");
    }

    static Set<String> getFilesInDirectory(File targetDirectory,
            String... relativePathExclusions) throws IOException {
        if (!targetDirectory.exists()) {
            LoggerFactory.getLogger(TaskCopyNpmAssetsFiles.class)
                    .info("No directory {}", targetDirectory);
            return new HashSet<>();
        }
        try (Stream<Path> stream = Files.walk(targetDirectory.toPath())) {
            return stream.filter(path -> path.toFile().isFile())
                    .map(path -> targetDirectory.toPath().relativize(path)
                            .toString().replaceAll("\\\\", "/"))
                    .collect(Collectors.toSet());
        }
    }
}
