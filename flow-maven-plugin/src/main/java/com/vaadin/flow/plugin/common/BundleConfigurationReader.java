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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonObject;

/**
 * Helper class for reading the contents of a bundle configuration file.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class BundleConfigurationReader {
    private final JsonObject bundleConfigurationJson;

    /**
     * Constructs a new bundle configuration reader for reading the given file.
     *
     * @param bundleConfigurationFile
     *            the file used to read configuration properties from
     *
     * @throws UncheckedIOException
     *             if fails to read the json file from the file system
     * @throws IllegalArgumentException
     *             if fails to parse json provided
     */
    public BundleConfigurationReader(File bundleConfigurationFile) {
        Objects.requireNonNull(bundleConfigurationFile,
                "Bundle configuration file cannot be null.");
        String fileContents;
        try {
            fileContents = new String(
                    Files.readAllBytes(bundleConfigurationFile.toPath()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to read specified bundle configuration file %s.",
                    bundleConfigurationFile), e);
        }

        try {
            bundleConfigurationJson = Json.parse(fileContents);
        } catch (JsonException e) {
            throw new IllegalArgumentException(String.format(
                    "Specified bundle configuration file '%s' does not contain valid Json.",
                    bundleConfigurationFile), e);
        }
    }

    /**
     * Get the fragments data from the bundle configuration specified: fragment
     * name and a set of file paths as strings to the resources that are to be
     * included in the final produced fragment file.
     *
     * @return the fragments defined in the configuration file
     *
     * @throws IllegalStateException
     *             if parsed json file does not contain valid fragment data
     */
    public Map<String, Set<String>> getFragments() {
        Map<String, Set<String>> fragments = new HashMap<>();

        if (bundleConfigurationJson == null
                || !bundleConfigurationJson.hasKey("fragments")) {
            return fragments;
        }

        JsonArray fragmentsArray;
        try {
            fragmentsArray = bundleConfigurationJson.getArray("fragments");
        } catch (JsonException | ClassCastException e) {
            throw new IllegalStateException(
                    "The 'fragments' property of a given bundle configuration should be an array.",
                    e);
        }

        for (int i = 0; i < fragmentsArray.length(); ++i) {
            JsonObject fragment;
            try {
                fragment = fragmentsArray.getObject(i);
            } catch (JsonException | ClassCastException e) {
                throw new IllegalStateException(
                        "The 'fragments' array of a given bundle configuration should contain fragment objects only.",
                        e);
            }
            String fragmentName = extractFragmentName(fragment);
            fragments.put(fragmentName,
                    extractFragmentFiles(fragment, fragmentName));
        }
        return fragments;
    }

    private String extractFragmentName(JsonObject fragment) {
        String fragmentName;
        try {
            fragmentName = fragment.getString("name");
        } catch (JsonException | ClassCastException | NullPointerException e) {
            throw new IllegalStateException(
                    "Each fragment object in json configuration should have `name` string field specified",
                    e);
        }
        if (fragmentName == null || fragmentName.isEmpty()) {
            throw new IllegalStateException(
                    "Each fragment object in json configuration should have non empty name");
        }
        return fragmentName;
    }

    private Set<String> extractFragmentFiles(JsonObject fragment,
            String fragmentName) {
        JsonArray fragmentFiles;
        try {
            fragmentFiles = fragment.getArray("files");
            if (fragmentFiles == null) {
                throw new IllegalStateException(String.format(
                        "Fragment with name '%s' has no `files` array field specified.",
                        fragmentName));
            }
        } catch (JsonException | ClassCastException e) {
            throw new IllegalStateException(String.format(
                    "Fragment with name '%s' has no `files` array field specified.",
                    fragmentName), e);
        }

        Set<String> files = Sets
                .newHashSetWithExpectedSize(fragmentFiles.length());
        for (int j = 0; j < fragmentFiles.length(); ++j) {
            try {
                files.add(fragmentFiles.getString(j));
            } catch (JsonException | ClassCastException e) {
                throw new IllegalStateException(String.format(
                        "The 'files' array of a fragment with name '%s' should only contain string file paths",
                        fragmentName), e);
            }
        }

        if (files.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Fragment with name '%s' has no files specified, each fragment should have at least one file specified",
                    fragmentName));
        }
        return files;
    }
}
