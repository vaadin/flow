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
package com.vaadin.flow.server.webjar;

import java.util.Objects;

/**
 * A class to hold information about webJar versions and names and ease the
 * webJars usage.
 * <p>
 * WebJars are generated with several mismatches in their names and versions.
 * For example, {@literal paper-input} has three different jar names in the
 * repository:
 * <ul>
 * <li>github-com-PolymerElements-paper-input</li>
 * <li>github-com-polymerelements-paper-input</li>
 * <li>paper-input</li>
 * </ul>
 * <p>
 * WebJars corresponding to the same <strong>bower</strong> version numbers may
 * start with 'v' prefix, so the repository contains both 'v2.0.1' and '2.0.1'
 * for same components.
 * <p>
 * Some webjar with one combination of name and version formats depend on webjar
 * with different combination. For instance,
 * {@literal org.webjar.bower:paper-slider:jar:2.0.2} (correct version format)
 * depends on {@literal org.webjar.bower:github-com-polymer-polymer:jar:v2.0.2}
 * <p>
 * Also, Maven is unable to resolve dependencies in the way {@literal bower}
 * does it, that's why we need to additionally filter transitive dependencies
 * and pick the one with proper version number.
 *
 * @see <a href="https://github.com/webJars/webJars/issues/1656">Issue</a>
 * @see <a href="https://www.webJars.org/">WebJars page</a>
 */
public class WebJarBowerDependency {
    private final String webJarName;
    private final String version;
    private final String bowerName;
    private final SemanticVersion semanticVersion;

    /**
     * Creates a webJar bower dependency class, also computing correct versions
     * of the packages.
     *
     * @param bowerName
     *            the name of a corresponding bower module (specified in
     *            bower.json)
     * @param webJarName
     *            the name of a webJar, not {@code null}
     * @param version
     *            the version of a webJar, not {@code null}
     *
     * @throws IllegalArgumentException
     *             as a result of
     *             {@link SemanticVersion#SemanticVersion(String)}
     */
    public WebJarBowerDependency(String bowerName, String webJarName,
            String version) {
        this.bowerName = Objects.requireNonNull(bowerName);
        this.webJarName = Objects.requireNonNull(webJarName);
        this.version = Objects.requireNonNull(version);
        semanticVersion = new SemanticVersion(version);
    }

    /**
     * Gets the webjar path that would be used to access its resources.
     *
     * @return the webJar path string
     */
    public String toWebPath() {
        return webJarName + '/' + version;
    }

    /**
     * Compares current and other dependencies' versions, also checking that
     * dependency names are equal.
     *
     * @param dependency
     *            the dependency to compare with
     * @return zero integer, if dependencies have the same version, positive
     *         integer, if current dependency was released later, negative
     *         integer otherwise
     * @throws IllegalArgumentException
     *             if dependencies have different
     *             {@link WebJarBowerDependency#bowerName}
     * @throws IllegalArgumentException
     *             as a
     *             {@link SemanticVersion#comparePatchParts(SemanticVersion)}
     *             result
     */
    public int compareVersions(WebJarBowerDependency dependency) {
        if (!Objects.equals(bowerName,
                Objects.requireNonNull(dependency).bowerName)) {
            throw new IllegalArgumentException(String.format(
                    "Received incomparable bower webJars with different bower names: '%s' and '%s'",
                    this, dependency));
        }
        return semanticVersion.comparePatchParts(dependency.semanticVersion);
    }

    @Override
    public String toString() {
        return "WebJarBowerDependency{" + "webJarName='" + webJarName + '\''
                + ", version='" + version + '\'' + '}';
    }
}
