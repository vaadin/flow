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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vaadin Ltd.
 */
public class BundleConfigurationReaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void cannot_construct_from_null_file() {
        new BundleConfigurationReader(null);
    }

    @Test(expected = JSONException.class)
    public void cannot_construct_from_file_containing_invalid_json() throws IOException {
        File configFile = temporaryFolder.newFile("test.json");
        Files.write(configFile.toPath(), Arrays.asList("invalid json"), StandardCharsets.UTF_8);
        new BundleConfigurationReader(configFile);
    }

    @Test(expected = JSONException.class)
    public void non_array_fragments_property_throws() throws IOException {
        File configFile = temporaryFolder.newFile("test.json");
        Files.write(configFile.toPath(), Arrays.asList("{'fragments':{}}"), StandardCharsets.UTF_8);
        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test(expected = JSONException.class)
    public void only_arrays_of_strings_allowed_in_the_fragments_array() throws IOException {
        File configFile = temporaryFolder.newFile("test.json");
        Files.write(configFile.toPath(), Arrays.asList("{'fragments':[{}, '']}"), StandardCharsets.UTF_8);
        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_parsed_correctly() throws IOException {
        File configFile = temporaryFolder.newFile("test.json");
        Files.write(configFile.toPath(), Arrays.asList("{'fragments':[['dependency-1', 'dependency-2'], ['dependency-3']]}"), StandardCharsets.UTF_8);
        Set<Set<String>> fragments = new BundleConfigurationReader(configFile).getFragments();
        Set<Set<String>> expected = createSet(createSet("dependency-1", "dependency-2"), createSet("dependency-3"));
        Assert.assertEquals(expected, fragments);
    }

    @Test
    public void no_fragments_present_returns_empty_set() throws IOException {
        File configFile = temporaryFolder.newFile("test.json");
        Files.write(configFile.toPath(), Arrays.asList("{'no-fragments-here':{}}"), StandardCharsets.UTF_8);
        Set<Set<String>> fragments = new BundleConfigurationReader(configFile).getFragments();
        Assert.assertEquals(Collections.emptySet(), fragments);
    }
    
    @Test
    public void empty_array_of_fragments_returns_empty_set() throws IOException {
        File configFile = temporaryFolder.newFile("test.json");
        Files.write(configFile.toPath(), Arrays.asList("{'fragments':[]}"), StandardCharsets.UTF_8);
        Set<Set<String>> fragments = new BundleConfigurationReader(configFile).getFragments();
        Assert.assertEquals(Collections.emptySet(), fragments);
    }
    
    @SafeVarargs
    private static <T> Set<T> createSet(T... items) {
        return Stream.of(items).collect(Collectors.toSet());
    }
}
