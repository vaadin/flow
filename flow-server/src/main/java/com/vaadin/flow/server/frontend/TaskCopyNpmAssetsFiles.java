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
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;

import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_ASSETS_PATH;

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
                    "webapp/" + VAADIN_STATIC_ASSETS_PATH);
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

            staticOutput = new File(webappResources, VAADIN_STATIC_ASSETS_PATH);
        }
    }

    @Override
    public void execute() {
        if (!options.copyAssets()) {
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
            npmAssetList.stream().filter(Predicate.not(String::isBlank))
                    .forEach(npmAsset -> {
                        handleAsset(npmModule, npmAsset);
                    });
        });
    }

    private void handleAsset(String npmModule, String npmAsset) {
        Rule rule = getRule(npmAsset, npmModule);
        File npmModuleDir = new File(options.getNodeModulesFolder(), npmModule);

        List<Path> paths = collectFiles(npmModuleDir.toPath(), rule.copyRule);
        Path basePath = getBasePath(npmModuleDir.toPath(), rule.copyRule);

        paths.stream().map(Path::toFile).forEach(file -> {
            copyFileToTarget(file, rule, basePath);
        });
    }

    private Rule getRule(String npmAsset, String npmModule) {
        String[] split = npmAsset.split(":");
        if (split.length != 2) {
            throw new InvalidParameterException("Invalid npm asset: " + npmAsset
                    + " for npm module: " + npmModule);
        }
        Rule rule = new Rule(split[0].strip(), split[1].strip());
        log().debug("Rule {} to {}", rule.copyRule, rule.targetFolder);
        return rule;
    }

    private void copyFileToTarget(File file, Rule copyRule, Path basePath) {
        File baseDestinationFolder = new File(staticOutput,
                copyRule.targetFolder);

        Path relativePath = basePath.relativize(file.toPath());
        File destFile = new File(baseDestinationFolder,
                relativePath.toString());
        // Copy file to a target path, if target file doesn't exist
        // or if file to copy is newer.
        if (!destFile.exists()
                || destFile.lastModified() < file.lastModified()) {
            log().debug("Copying npm file {} to {}", file.getAbsolutePath(),
                    destFile.getAbsolutePath());
            try {
                Files.createDirectories(destFile.toPath().getParent());
                Files.copy(file.toPath(), destFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(String.format(
                        "Failed to copy project frontend resources from '%s' to '%s'",
                        file, destFile), e);
            }
        }
    }

    private Path getBasePath(Path npmModuleDir, String copyRule) {
        // Extract the static part of the copy rule (before any wildcards)
        String basePathStr = copyRule;

        // Remove leading slashes
        if (basePathStr.startsWith("/")) {
            basePathStr = basePathStr.substring(1);
        }

        int wildcardIndex = -1;
        if (basePathStr.contains("*")) {
            wildcardIndex = basePathStr.indexOf('*');
        } else if (basePathStr.contains("?")) {
            wildcardIndex = basePathStr.indexOf('?');
        }

        if (wildcardIndex != -1) {
            basePathStr = basePathStr.substring(0, wildcardIndex);
            // Remove trailing slash or incomplete path segment
            int lastSlash = basePathStr.lastIndexOf('/');
            if (lastSlash != -1) {
                // Resolve the base path relative to npmModuleDir
                return npmModuleDir
                        .resolve(basePathStr.substring(0, lastSlash));
            }
        }

        return npmModuleDir;
    }

    private List<Path> collectFiles(Path basePath, String matcherPattern) {
        final List<Path> filePaths = new ArrayList<>();
        if (!basePath.toFile().exists()) {
            return filePaths;
        }
        String pattern;
        if (matcherPattern.startsWith("**")) {
            pattern = matcherPattern;
        } else if (matcherPattern.startsWith("/")) {
            pattern = "**" + matcherPattern;
        } else {
            pattern = "**/" + matcherPattern;
        }

        log().debug("getting files using pattern {}", pattern);

        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern);

        filePaths.addAll(getFileNames(basePath, matcher));

        log().debug("Paths amount {}", filePaths.size());

        return filePaths;
    }

    private List<Path> getFileNames(Path dir, PathMatcher matcher) {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path.toFile().isDirectory()) {
                    fileNames.addAll(getFileNames(path, matcher));
                } else if (matcher.matches(path)) {
                    fileNames.add(path);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk through directory",
                    e);
        }
        return fileNames;
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    private static record Rule(String copyRule, String targetFolder) {
    }
}
