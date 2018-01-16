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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
        } catch (JSONException je) {
            throw new JSONException(String.format("Specified bundle configuration file %s does not contain valid JSON.", bundleConfigurationFile), je);
        }
    }
    
    /**
     * Get the set of fragments contained in this bundle configuration. Each
     * fragment consists of a set of file paths as strings to the resources that
     * are to be included in the final produced fragment file.
     *
     * @return the fragments defined in the configuration file
     */
    public Set<Set<String>> getFragments() {
        return readFragments();
    }

    private Set<Set<String>> readFragments() {
        Set<Set<String>> fragments = new HashSet<>();

        if (bundleConfigurationJSON == null || !bundleConfigurationJSON.has("fragments")) {
            return fragments;
        }

        JSONArray fragmentsArray;
        try {
            fragmentsArray = bundleConfigurationJSON.getJSONArray("fragments");
        } catch (JSONException je) {
            throw new JSONException("The 'fragments' property of a given bundle configuration should be an array.", je);
        }

        for (int i = 0; i < fragmentsArray.length(); ++i) {
            try {
                JSONArray fragmentFiles = fragmentsArray.getJSONArray(i);
                fragments.add(readFragment(fragmentFiles));
            } catch (JSONException je) {
                throw new JSONException("The 'fragments' array of a given bundle configuration should only contain arrays of fragment file names.", je);
            }
        }
        return fragments;
    }

    private Set<String> readFragment(JSONArray fragmentFiles) {
        Set<String> fragment = new HashSet<>();
        for (int j = 0; j < fragmentFiles.length(); ++j) {
            String fragmentFile;
            try {
                fragmentFile = fragmentFiles.getString(j);
            } catch (JSONException je) {
                throw new JSONException("The 'fragments' array of a given bundle configuration should only contain string file paths.", je);
            }
            fragment.add(fragmentFile);
        }
        return fragment;
    }
}
