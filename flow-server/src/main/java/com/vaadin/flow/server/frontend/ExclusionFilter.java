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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Excludes dependencies listed in an "exclusions" array of
 * vaadin-*versions.json file from a package.json.
 */
public class ExclusionFilter implements Serializable {

    private final ClassFinder finder;

    private final boolean reactEnabled;

    private final boolean excludeWebComponentNpmPackages;

    /**
     * Create a new exclusion filter.
     *
     * @param finder
     *            the class finder to use
     * @param reactEnabled
     *            whether React is enabled
     */
    public ExclusionFilter(ClassFinder finder, boolean reactEnabled) {
        this(finder, reactEnabled, false);
    }

    /**
     * Create a new exclusion filter.
     *
     * @param finder
     *            the class finder to use
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponentNpmPackages
     *            whether to exclude web component npm packages
     */
    public ExclusionFilter(ClassFinder finder, boolean reactEnabled,
            boolean excludeWebComponentNpmPackages) {
        this.finder = finder;
        this.reactEnabled = reactEnabled;
        this.excludeWebComponentNpmPackages = excludeWebComponentNpmPackages;
    }

    /**
     * Exclude dependencies from the given map based on the
     * vaadin-*versions.json files.
     *
     * @param dependencies
     *            the dependencies to filter
     * @return the filtered dependencies
     * @throws IOException
     *             if an I/O error occurs
     */
    public Map<String, String> exclude(Map<String, String> dependencies)
            throws IOException {
        var exclusions = getExclusions();
        return dependencies.entrySet().stream()
                .filter(entry -> !exclusions.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    private List<String> getExclusions() throws IOException {
        List<String> exclusions = new ArrayList<>();
        URL coreVersionsResource = finder
                .getResource(Constants.VAADIN_CORE_VERSIONS_JSON);
        if (coreVersionsResource != null) {
            exclusions.addAll(getExclusions(coreVersionsResource));
        }
        URL vaadinVersionsResource = finder
                .getResource(Constants.VAADIN_VERSIONS_JSON);
        if (vaadinVersionsResource != null) {
            exclusions.addAll(getExclusions(vaadinVersionsResource));
        }
        return exclusions;
    }

    private Set<String> getExclusions(URL versionsResource) throws IOException {
        try (InputStream content = versionsResource.openStream()) {
            VersionsJsonConverter convert = new VersionsJsonConverter(
                    JacksonUtils.readTree(
                            IOUtils.toString(content, StandardCharsets.UTF_8)),
                    reactEnabled, excludeWebComponentNpmPackages);
            return convert.getExclusions();
        }
    }
}
