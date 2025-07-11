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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies JavaScript and CSS files from JAR files into a given folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskCopyNpmAssetsFiles
        extends AbstractFileGeneratorFallibleCommand {
    private final Options options;

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param options
     *            build options
     */
    TaskCopyNpmAssetsFiles(Options options) {
        this.options = options;
    }

    @Override
    public void execute() {
        long start = System.nanoTime();
        log().info("Copying npm assets from node_modules ...");
        // File targetDirectory = options.getJarFrontendResourcesFolder();
        // TaskCopyLocalFrontendFiles.createTargetFolder(targetDirectory);

        // Set<String> existingFiles = new HashSet<>();
        Set<String> handledFiles = new HashSet<>();

        Map<String, List<String>> npmAssets = options
                .getFrontendDependenciesScanner().getAssets();
        // log().info("Assets found: {}", npmAssets.size());
        npmAssets.forEach((npmModule, npmAssetList) -> {
            npmAssetList.forEach(npmAsset -> {
                if (npmAsset.isBlank()) {
                    return;
                }
                // log().info("Npm module to copy {} : asset {}", npmModule,
                // npmAsset);
                String[] split = npmAsset.split(":");
                if (split.length != 2) {
                    throw new InvalidParameterException("Invalid npm asset: "
                            + npmAsset + " for npm module: " + npmModule);
                }
                log().info("Rule {}", split[0]);
                File npmModuleDir = new File(options.getNodeModulesFolder(),
                        npmModule);
                List<Path> paths = collectFiles(npmModuleDir.toPath(),
                        split[0].strip());
                log().info("Paths amount {}", paths.size());
                paths.stream().map(Path::toFile).forEach(file -> {
                    File destFile = new File(options.getFrontendDirectory(),
                            npmModuleDir.toPath().relativize(file.toPath())
                                    .toString());
                    log().info("Copying npm file {}", file.getAbsolutePath());
                    log().info("Copying npm asset {}",
                            destFile.getAbsolutePath());
                    try {
                        FileUtils.copyFile(file, destFile);
                        handledFiles.add(destFile.getAbsolutePath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(String.format(
                                "Failed to copy project frontend resources from '%s' to '%s'",
                                file, destFile), e);
                    }
                });
            });
        });
        //
        // if(!options.isProductionMode()) {
        // Map<String, String> npmDevAssets =
        // options.getFrontendDependenciesScanner()
        // .getDevAssets();
        // }

        // existingFiles.removeAll(handledFiles);
        // existingFiles.forEach(
        // filename -> new File(targetDirectory, filename).delete());
        long ms = (System.nanoTime() - start) / 1000000;
        log().info("Copying npm assets done. Took {} ms.", ms);
        // track(handledFiles.stream().map(relativePath ->
        // targetDirectory.toPath()
        // .resolve(relativePath).toFile()).toList());
    }

    private List<Path> collectFiles(Path basePath, String matcherPattern) {
        final List<Path> filePaths = new ArrayList<>();
        log().info("!! {}", matcherPattern);
        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:**" + matcherPattern);
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        filePaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return filePaths;
    }

    static Set<String> getFilesInDirectory(File targetDirectory,
            String... relativePathExclusions) throws IOException {
        try (Stream<Path> stream = Files.walk(targetDirectory.toPath())) {
            return stream.filter(path -> path.toFile().isFile()
                    && TaskCopyLocalFrontendFiles.keepFile(targetDirectory,
                            relativePathExclusions, path.toFile()))
                    .map(path -> targetDirectory.toPath().relativize(path)
                            .toString().replaceAll("\\\\", "/"))
                    .collect(Collectors.toSet());
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

}
