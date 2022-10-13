/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.plugin.common.ArtifactData;

/**
 * Wrapper around {@link ArtifactData} that holds information about a package located in the corresponding WebJar.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class WebJarPackage {
    private final ArtifactData webJar;
    private final String packageName;
    private final String pathToPackage;

    /**
     * Creates new wrapper instance.
     *
     * @param webJar        the WebJar that holds the package in, not {@code null}
     * @param packageName   name of a package inside the WebJar, not {@code null}
     * @param pathToPackage path to package inside the WebJar, not {@code null}
     */
    public WebJarPackage(ArtifactData webJar, String packageName, String pathToPackage) {
        this.webJar = Objects.requireNonNull(webJar);
        this.packageName = Objects.requireNonNull(packageName);
        this.pathToPackage = Objects.requireNonNull(pathToPackage);
    }

    /**
     * Gets the WebJar that holds the package.
     *
     * @return web jar data
     */
    public ArtifactData getWebJar() {
        return webJar;
    }

    /**
     * Gets the name of the package in the WebJar.
     *
     * @return name of the package
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets path to package that in the WebJar.
     *
     * @return path to package
     */
    public String getPathToPackage() {
        return pathToPackage;
    }

    /**
     * Attempts to select correct package out of two WebJar package data, resolving common WebJar issues:
     * <ul>
     * <li><a href="https://github.com/webjars/webjars/issues/1656">https://github.com/webjars/webjars/issues/1656</a></li>
     * <li><a href="https://github.com/webjars/webjars/issues/1452">https://github.com/webjars/webjars/issues/1452</a></li>
     * </ul>
     * <p>
     * Versions of the parent packages are considered, even if they are different from the package's one.
     * The reason for this is:
     * <a href="https://github.com/bower/spec/blob/master/json.md#version">bower.json version field deprecation notes</a>
     *
     * @param package1 first package data
     * @param package2 second package data
     * @return package with as less issues as possible
     * @throws IllegalArgumentException when packages have different names or versions
     */
    public static WebJarPackage selectCorrectPackage(WebJarPackage package1,
            WebJarPackage package2) {
        if (!Objects.equals(package1.packageName, package2.packageName)) {
            throw new IllegalArgumentException(String.format(
                    "Cannot process packages with different names: '%s' and '%s'",
                    package1.packageName, package2.packageName));
        }

        String normalizedVersion1 = normalizeVersion(
                package1.webJar.getVersion());
        String normalizedVersion2 = normalizeVersion(
                package2.webJar.getVersion());
        if (Objects.equals(normalizedVersion1, normalizedVersion2)) {
            return selectTopmostPackage(package1, package2).orElseGet(
                    () -> tryToSelectPackageWithNormalizedVersion(package1,
                            package2, normalizedVersion1));
        }
        throw new IllegalArgumentException(String.format(
                "Two webJars have same name and different versions: '%s' and '%s', there should be no version differences",
                package1.webJar, package2.webJar));
    }
    
    private static Optional<WebJarPackage> selectTopmostPackage(
            WebJarPackage package1, WebJarPackage package2) {
        String path1 = package1.getPathToPackage();
        String path2 = package2.getPathToPackage();

        if (!Objects.equals(path1, path2)) {
            if (path1.startsWith(path2)) {
                return Optional.of(package2);
            } else if (path2.startsWith(path1)) {
                return Optional.of(package1);
            }
        }
        return Optional.empty();
    }

    private static WebJarPackage tryToSelectPackageWithNormalizedVersion(
            WebJarPackage package1, WebJarPackage package2,
            String normalizedVersion1) {
        return Objects.equals(normalizedVersion1, package1.webJar.getVersion())
                ? package1
                : package2;
    }

    private static String normalizeVersion(String version) {
        if (version.charAt(0) == 'v') {
            return version.substring(1);
        }
        return version;
    }
}
