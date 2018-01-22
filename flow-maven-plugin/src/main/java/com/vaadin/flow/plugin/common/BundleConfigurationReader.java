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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for reading the contents of a bundle configuration file.
 *
 * @author Vaadin Ltd.
 */
public class BundleConfigurationReader {
    private final JSONObject bundleConfigurationJSON;

    /**
     * Constructs a new bundle configuration reader for reading the given file.
     *
     * @param bundleConfigurationFile
     *            the file used to read configuration properties from
     */
    public BundleConfigurationReader(File bundleConfigurationFile) {
        Objects.requireNonNull(bundleConfigurationFile, "Bundle configuration file cannot be null.");
        String readFile;
        try {
            readFile = new String(Files.readAllBytes(bundleConfigurationFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to read specified bundle configuration file %s.", bundleConfigurationFile), e);
        }

        try {
            bundleConfigurationJSON = new JSONObject(readFile);
        } catch (JSONException e) {
            throw new IllegalArgumentException(String.format("Specified bundle configuration file '%s' does not contain valid JSON.", bundleConfigurationFile), e);
        }
    }

    /**
     * Get the fragments data from the bundle configuration specified: fragment name and a set of file paths as strings to the resources that
     * are to be included in the final produced fragment file.
     *
     * @return the fragments defined in the configuration file
     */
    public Map<String, Set<String>> getFragments() {
        Map<String, Set<String>> fragments = new HashMap<>();

        if (bundleConfigurationJSON == null || !bundleConfigurationJSON.has("fragments")) {
            return fragments;
        }

        JSONArray fragmentsArray;
        try {
            fragmentsArray = bundleConfigurationJSON.getJSONArray("fragments");
        } catch (JSONException e) {
            throw new IllegalArgumentException("The 'fragments' property of a given bundle configuration should be an array.", e);
        }

        for (int i = 0; i < fragmentsArray.length(); ++i) {
            JSONObject fragment;
            try {
                fragment = fragmentsArray.getJSONObject(i);
            } catch (JSONException e) {
                throw new IllegalArgumentException("The 'fragments' array of a given bundle configuration should contain fragment objects only.", e);
            }
            String fragmentName = extractFragmentName(fragment);
            fragments.put(fragmentName, extractFragmentFiles(fragment, fragmentName));
        }
        return fragments;
    }

    private String extractFragmentName(JSONObject fragment) {
        String fragmentName;
        try {
            fragmentName = fragment.getString("name");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Each fragment object in json configuration should have `name` string field specified", e);
        }
        if (fragmentName == null || fragmentName.isEmpty()) {
            throw new IllegalArgumentException("Each fragment object in json configuration should have non empty name");
        }
        return fragmentName;
    }

    private Set<String> extractFragmentFiles(JSONObject fragment, String fragmentName) {
        JSONArray fragmentFiles;
        try {
            fragmentFiles = fragment.getJSONArray("files");
        } catch (JSONException e) {
            throw new IllegalArgumentException(String.format("Fragment with name '%s' has no `files` array field specified.", fragmentName), e);
        }

        Set<String> files = Sets.newHashSetWithExpectedSize(fragmentFiles.length());
        for (int j = 0; j < fragmentFiles.length(); ++j) {
            try {
                files.add(fragmentFiles.getString(j));
            } catch (JSONException e) {
                throw new IllegalArgumentException(String.format("The 'files' array of a fragment with name '%s' should only contain string file paths", fragmentName), e);
            }
        }

        if (files.isEmpty()) {
            throw new IllegalArgumentException(String.format("Fragment with name '%s' has no files specified, each fragment should have at least one file specified", fragmentName));
        }
        return files;
    }
}
