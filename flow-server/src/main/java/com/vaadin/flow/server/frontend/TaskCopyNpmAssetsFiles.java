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
import java.nio.file.Paths;
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

import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_FILES_PATH;

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

    private final File staticOutput;

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param options
     *            build options
     */
    TaskCopyNpmAssetsFiles(Options options) {
        this.options = options;

        if (options.isDevBundleBuild()) {
            staticOutput = new File(
                    DevBundleUtils.getDevBundleFolder(options.getNpmFolder(),
                            options.getBuildDirectoryName()),
                    "webapp");
        } else {
            String webappResources;
            if (options.getWebappResourcesDirectory() == null) {
                webappResources = FrontendUtils.getUnixPath(options
                        .getNpmFolder().toPath()
                        .resolve(Paths.get(options.getBuildDirectoryName(),
                                "classes", VAADIN_WEBAPP_RESOURCES)
                                .normalize()));
            } else {
                webappResources = options.getWebappResourcesDirectory()
                        .getPath();
            }

            staticOutput = new File(webappResources, VAADIN_STATIC_FILES_PATH);
        }
    }

    @Override
    public void execute() {
        if (!options.copyAssets() && !options.isDevBundleBuild()) {
            return;
        }

        if (hasAssets()) {
            long start = System.nanoTime();
            log().info("Copying npm assets from node_modules ...");

            Map<String, List<String>> assets = options
                    .getFrontendDependenciesScanner().getAssets();
            copyNpmAssets(assets);

            if (!options.isProductionMode()) {
                assets = options.getFrontendDependenciesScanner()
                        .getDevAssets();
                copyNpmAssets(assets);
            }

            long ms = (System.nanoTime() - start) / 1000000;
            log().info("Copying npm assets done. Took {} ms.", ms);
        }

    }

    private boolean hasAssets() {
        return !options.getFrontendDependenciesScanner().getAssets().isEmpty()
                || !options.getFrontendDependenciesScanner().getDevAssets()
                        .isEmpty();
    }

    private void copyNpmAssets(Map<String, List<String>> npmAssets) {
        npmAssets.forEach((npmModule, npmAssetList) -> {
            npmAssetList.forEach(npmAsset -> {
                if (npmAsset.isBlank()) {
                    return;
                }
                String[] split = npmAsset.split(":");
                if (split.length != 2) {
                    throw new InvalidParameterException("Invalid npm asset: "
                            + npmAsset + " for npm module: " + npmModule);
                }
                log().debug("Rule {} to {}", split[0], split[1]);
                File npmModuleDir = new File(options.getNodeModulesFolder(),
                        npmModule);
                List<Path> paths = collectFiles(npmModuleDir.toPath(),
                        split[0].strip());
                log().debug("Paths amount {}", paths.size());
                paths.stream().map(Path::toFile).forEach(file -> {
                    File targetFolder = new File(staticOutput,
                            split[1].strip());
                    File destFile = new File(targetFolder, file.getName());
                    // Copy file to a target path, if target file doesn't exist
                    // or if file to copy is newer.
                    if (!destFile.exists()
                            || destFile.lastModified() < file.lastModified()) {
                        log().debug("Copying npm file {} to {}",
                                file.getAbsolutePath(),
                                destFile.getAbsolutePath());
                        try {
                            FileUtils.copyFile(file, destFile);
                        } catch (IOException e) {
                            throw new UncheckedIOException(String.format(
                                    "Failed to copy project frontend resources from '%s' to '%s'",
                                    file, destFile), e);
                        }
                    }
                });
            });
        });
    }

    private List<Path> collectFiles(Path basePath, String matcherPattern) {
        final List<Path> filePaths = new ArrayList<>();

        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:**" + matcherPattern);
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) {
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
        if (!targetDirectory.exists()) {
            return new HashSet<>();
        }
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
