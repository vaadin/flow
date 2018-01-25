/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.plugin.common.JarContentsManager;
import com.vaadin.flow.plugin.common.WebJarData;

/**
 * Copies specified artifacts' contents to the output folder.
 * <p>
 * Note: this class is intended to be independent from Maven dependencies so that it can be reused in Gradle plugin in future.
 *
 * @author Vaadin Ltd.
 */
public class ProductionModeCopyStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionModeCopyStep.class);
    private static final String NON_WEB_JAR_RESOURCE_PATH = "META-INF/resources/frontend";
    private static final String BOWER_JSON_FILE_NAME = "bower.json";
    private static final String BOWER_COMPONENTS_DIRECTORY_NAME = "bower_components";

    private final JarContentsManager jarContentsManager;
    private final Set<File> nonWebJars;
    private final Map<String, WebJarPackage> webJarNameToPackage;

    /**
     * Fill and verify initial jar files' data.
     *
     * @param jarContentsManager a class to manage jar file contents, not {@code null}
     * @param webJars            set of WebJars to copy files from, not {@code null}. Only bower webJars are supported now.
     * @param nonWebJars         set of other jars to copy data from, only files from {@link ProductionModeCopyStep#NON_WEB_JAR_RESOURCE_PATH} are copied; not {@code null}
     * @throws IllegalArgumentException if no {@literal bower.json} is found inside any WebJar
     * @throws UncheckedIOException     if any {@link IOException} occurs during {@literal bower.json} parsing
     */
    public ProductionModeCopyStep(JarContentsManager jarContentsManager,
                                  Set<WebJarData> webJars, Set<File> nonWebJars) {
        this.jarContentsManager = Objects.requireNonNull(jarContentsManager);
        this.nonWebJars = Objects.requireNonNull(nonWebJars);
        webJarNameToPackage = extractWebPackages(webJars);
    }

    private Map<String, WebJarPackage> extractWebPackages(Collection<WebJarData> webJars) {
        Map<String, WebJarPackage> result = Maps.newHashMapWithExpectedSize(webJars.size());
        for (WebJarData webJar : webJars) {
            jarContentsManager.findFiles(webJar.getJarFile(), WebJarData.WEB_JAR_FILES_BASE, BOWER_JSON_FILE_NAME).stream()
                    .map(bowerJsonPath -> new WebJarPackage(webJar, getPackageName(webJar, bowerJsonPath), getPackageDirectory(bowerJsonPath)))
                    .forEach(webJarPackage -> result.merge(webJarPackage.getPackageName(), webJarPackage, WebJarPackage::selectCorrectPackage));
        }
        return result;
    }

    private String getPackageDirectory(String bowerJsonPath) {
        return bowerJsonPath.substring(0, bowerJsonPath.lastIndexOf('/') + 1);
    }

    private String getPackageName(WebJarData webJar, String nameSourceJarPath) {
        String fileContents;
        try {
            fileContents = IOUtils.toString(jarContentsManager.getFileContents(webJar.getJarFile(), nameSourceJarPath), StandardCharsets.UTF_8.displayName());
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Unable to read file '%s' from webJar '%s'", nameSourceJarPath, webJar.getJarFile()), e);
        }
        JSONObject jsonObject = new JSONObject(fileContents);
        if (jsonObject.has("name")) {
            return jsonObject.getString("name");
        } else {
            throw new IllegalStateException(String.format("Incorrect WebJar '%s': file '%s' inside it has no 'name' field", webJar, nameSourceJarPath));
        }
    }

    /**
     * Copies files from earlier specified jars and {@code frontendWorkingDirectory}, applying exclusions specified to all files.
     *
     * @param outputDirectory                      the directory to copy files into, not {@code null}
     * @param frontendWorkingDirectory             the directory to copy files from, intended to be current application's directory with frontend files, can be {@code null}
     * @param commaSeparatedWildcardPathExclusions comma separated wildcard exclusions to exclude files, can be {@code null} if no files are excluded
     * @throws IllegalStateException if any directory fails to be created via {@link File#mkdirs()}
     * @throws UncheckedIOException  if any {@link IOException} occurs during other file operations
     */
    public void copyWebApplicationFiles(File outputDirectory, File frontendWorkingDirectory, String commaSeparatedWildcardPathExclusions) {
        LOGGER.info("Copying web application files to '{}'", outputDirectory);
        FlowPluginFileUtils.forceMkdir(outputDirectory);

        String[] wildcardExclusions = getWildcardExclusions(commaSeparatedWildcardPathExclusions);

        if (frontendWorkingDirectory != null && frontendWorkingDirectory.isDirectory()) {
            try {
                FileUtils.copyDirectory(frontendWorkingDirectory, outputDirectory, generateFilterWithExclusions(wildcardExclusions));
            } catch (IOException e) {
                throw new UncheckedIOException(String.format("Failed to copy contents from `%s` to `%s`", frontendWorkingDirectory, outputDirectory), e);
            }
        }

        if (!webJarNameToPackage.isEmpty()) {
            File bowerComponents = new File(outputDirectory, BOWER_COMPONENTS_DIRECTORY_NAME);
            webJarNameToPackage.forEach((name, webJarPackage) -> {
                File webJarDirectory = new File(bowerComponents, name);
                FlowPluginFileUtils.forceMkdir(webJarDirectory);
                jarContentsManager.copyFilesFromJarTrimmingBasePath(webJarPackage.getWebJar().getJarFile(), webJarPackage.getPathToPackage(), webJarDirectory, wildcardExclusions);
            });
        }

        for (File notWebJar : nonWebJars) {
            jarContentsManager.copyFilesFromJarTrimmingBasePath(notWebJar, NON_WEB_JAR_RESOURCE_PATH, outputDirectory, wildcardExclusions);
        }
    }

    private FileFilter generateFilterWithExclusions(String... pathExclusions) {
        if (pathExclusions == null || pathExclusions.length == 0) {
            return null;
        }
        return pathname -> Stream.of(pathExclusions).noneMatch(exclusionRule -> FilenameUtils.wildcardMatch(pathname.getName(), exclusionRule));
    }

    private String[] getWildcardExclusions(String commaSeparatedWildcardPathExclusions) {
        if (commaSeparatedWildcardPathExclusions == null || commaSeparatedWildcardPathExclusions.isEmpty()) {
            return new String[0];
        }
        // regex: remove all spaces next to commas
        return commaSeparatedWildcardPathExclusions.trim().replaceAll("[\\s]*,[\\s]*", ",").split(",");
    }
}
