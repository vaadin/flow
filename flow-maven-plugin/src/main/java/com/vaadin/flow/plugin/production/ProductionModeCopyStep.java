/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.plugin.common.ArtifactData;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.JarContentsManager;
import com.vaadin.flow.utils.FlowFileUtils;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Copies specified artifacts' contents to the output folder.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ProductionModeCopyStep {
    static final String WEB_JAR_FILES_BASE = "META-INF/resources/webjars/";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ProductionModeCopyStep.class);
    private static final String BOWER_JSON_FILE_NAME = "bower.json";
    private static final String BOWER_COMPONENTS_DIRECTORY_NAME = "bower_components";

    private final JarContentsManager jarContentsManager;
    private final Set<File> nonWebJars = new HashSet<>();
    private final Map<String, WebJarPackage> webJarNameToPackage = new HashMap<>();

    /**
     * Fill and verify initial jar files' data.
     *
     * @param projectArtifacts
     *            project artifacts to get the data from
     * @throws IllegalArgumentException
     *             if no {@literal bower.json} is found inside any WebJar
     * @throws UncheckedIOException
     *             if any {@link IOException} occurs during
     *             {@literal bower.json} parsing
     */
    public ProductionModeCopyStep(Collection<ArtifactData> projectArtifacts) {
        this(new JarContentsManager(), projectArtifacts);
    }

    /**
     * Fill and verify initial jar files' data, use custom version of
     * {@link JarContentsManager} for the operations.
     *
     * @param jarContentsManager
     *            a class to manage jar file contents, not {@code null}
     * @param projectArtifacts
     *            project artifacts to get the data from
     * @throws IllegalArgumentException
     *             if no {@literal bower.json} is found inside any WebJar
     * @throws UncheckedIOException
     *             if any {@link IOException} occurs during
     *             {@literal bower.json} parsing
     */
    public ProductionModeCopyStep(JarContentsManager jarContentsManager,
            Collection<ArtifactData> projectArtifacts) {
        this.jarContentsManager = Objects.requireNonNull(jarContentsManager);

        for (ArtifactData artifact : projectArtifacts) {
            File artifactFile = artifact.getFileOrDirectory();
            if (artifactFile.isFile()) {
                if (jarContentsManager.containsPath(artifactFile,
                        WEB_JAR_FILES_BASE)) {
                    storeWebJarData(artifact);
                } else {
                    nonWebJars.add(artifactFile);
                }
            } else {
                LOGGER.debug(
                        "Skipping project artifact '{}' because it does not exist or not a file",
                        artifact);
            }
        }
    }

    private void storeWebJarData(ArtifactData webJar) {
        getWebJarFiles(webJar).stream()
                .map(bowerJsonPath -> new WebJarPackage(webJar,
                        getPackageName(webJar, bowerJsonPath),
                        getPackageDirectory(bowerJsonPath)))
                .forEach(webJarPackage -> webJarNameToPackage.merge(
                        webJarPackage.getPackageName(), webJarPackage,
                        WebJarPackage::selectCorrectPackage));
    }

    private List<String> getWebJarFiles(ArtifactData webJar) {
        List<String> bowerJsonFiles = jarContentsManager.findFiles(
                webJar.getFileOrDirectory(), WEB_JAR_FILES_BASE,
                BOWER_JSON_FILE_NAME);
        if (!bowerJsonFiles.isEmpty()) {
            return bowerJsonFiles;
        }
        // try to find something here since there are bowergithub WebJars that
        // have no
        // bower.json but have package.json like
        // https://repo1.maven.org/maven2/org/webjars/bowergithub/webcomponents/shadycss/1.5.0-1/
        List<String> packageJsonFallback = jarContentsManager.findFiles(
                webJar.getFileOrDirectory(), WEB_JAR_FILES_BASE,
                Constants.PACKAGE_JSON);
        if (packageJsonFallback.isEmpty()) {
            LOGGER.warn(
                    "Found no bower.json or package.json files inside {}. No files will be extracted.",
                    webJar);
        }
        return packageJsonFallback;
    }

    private String getPackageDirectory(String bowerJsonPath) {
        return bowerJsonPath.substring(0, bowerJsonPath.lastIndexOf('/') + 1);
    }

    private String getPackageName(ArtifactData webJar,
            String nameSourceJarPath) {
        String fileContents;
        try {
            fileContents = IOUtils.toString(
                    jarContentsManager.getFileContents(
                            webJar.getFileOrDirectory(), nameSourceJarPath),
                    StandardCharsets.UTF_8.displayName());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Unable to read file '%s' from webJar '%s'",
                            nameSourceJarPath, webJar.getFileOrDirectory()),
                    e);
        }
        JsonObject jsonObject = Json.parse(fileContents);
        if (jsonObject.hasKey("name")) {
            String name = jsonObject.getString("name");
            return name.substring(name.lastIndexOf('/') + 1);
        } else {
            throw new IllegalStateException(String.format(
                    "Incorrect WebJar '%s': file '%s' inside it has no 'name' field",
                    webJar, nameSourceJarPath));
        }
    }

    /**
     * Copies files from earlier specified jars and
     * {@code frontendWorkingDirectory}, applying exclusions specified to all
     * files.
     *
     * @param outputDirectory
     *            the directory to copy files into, not {@code null}
     * @param frontendWorkingDirectory
     *            the directory to copy files from, intended to be current
     *            application's directory with frontend files, can be
     *            {@code null}
     * @param commaSeparatedWildcardPathExclusions
     *            comma separated wildcard exclusions to exclude files, can be
     *            {@code null} if no files are excluded
     * @throws IllegalStateException
     *             if any directory fails to be created via
     *             {@link File#mkdirs()}
     * @throws UncheckedIOException
     *             if any {@link IOException} occurs during other file
     *             operations
     */
    public void copyWebApplicationFiles(File outputDirectory,
            File frontendWorkingDirectory,
            String commaSeparatedWildcardPathExclusions) {
        LOGGER.info("Copying web application files to '{}'", outputDirectory);
        FlowFileUtils.forceMkdir(outputDirectory);

        String[] wildcardExclusions = getWildcardPaths(
                commaSeparatedWildcardPathExclusions);

        if (frontendWorkingDirectory != null
                && frontendWorkingDirectory.isDirectory()) {
            try {
                FileUtils.copyDirectory(frontendWorkingDirectory,
                        outputDirectory,
                        generateFilterWithExclusions(wildcardExclusions));
            } catch (IOException e) {
                throw new UncheckedIOException(String.format(
                        "Failed to copy contents from `%s` to `%s`",
                        frontendWorkingDirectory, outputDirectory), e);
            }
        }

        if (!webJarNameToPackage.isEmpty()) {
            File bowerComponents = new File(outputDirectory,
                    BOWER_COMPONENTS_DIRECTORY_NAME);
            webJarNameToPackage.forEach((name, webJarPackage) -> {
                File webJarDirectory = new File(bowerComponents, name);
                FlowFileUtils.forceMkdir(webJarDirectory);
                jarContentsManager.copyFilesFromJarTrimmingBasePath(
                        webJarPackage.getWebJar().getFileOrDirectory(),
                        webJarPackage.getPathToPackage(), webJarDirectory,
                        wildcardExclusions);
            });
        }

        for (File notWebJar : nonWebJars) {
            jarContentsManager.copyFilesFromJarTrimmingBasePath(notWebJar,
                    Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                    outputDirectory, wildcardExclusions);
        }
    }

    /**
     * Copies files from earlier specified jars and {@code
     * frontendWorkingDirectory}, applying exclusions specified to all files.
     *
     * @param frontendDirectory
     *            the directory to copy files into, not {@code null}
     * @param commaSeparatedWildcardPathInclusions
     *            comma separated wildcard to include files, can be {@code null}
     *            if no files are included
     * @param jsResourcePath
     *            path to get the js files from
     * @throws UncheckedIOException
     *             if any {@link IOException} occurs during other file
     *             operations
     */
    public void copyFrontendJavaScriptFiles(File frontendDirectory,
            String commaSeparatedWildcardPathInclusions,
            String jsResourcePath) {
        LOGGER.info("Copying frontend '.js' files to '{}'", frontendDirectory);
        FlowFileUtils.forceMkdir(frontendDirectory);

        String[] wildcardInclusions = getWildcardPaths(
                commaSeparatedWildcardPathInclusions);

        for (File jarFile : nonWebJars) {
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(jarFile,
                    jsResourcePath, frontendDirectory, wildcardInclusions);
        }
    }

    private FileFilter generateFilterWithExclusions(String... pathExclusions) {
        if (pathExclusions == null || pathExclusions.length == 0) {
            return null;
        }
        return pathname -> Stream.of(pathExclusions)
                .noneMatch(exclusionRule -> FilenameUtils
                        .wildcardMatch(pathname.getName(), exclusionRule));
    }

    private String[] getWildcardPaths(String commaSeparatedWildcardPaths) {
        if (commaSeparatedWildcardPaths == null
                || commaSeparatedWildcardPaths.isEmpty()) {
            return new String[0];
        }
        // regex: remove all spaces next to commas
        return commaSeparatedWildcardPaths.trim()
                .replaceAll("[\\s]*,[\\s]*", ",").split(",");
    }
}
