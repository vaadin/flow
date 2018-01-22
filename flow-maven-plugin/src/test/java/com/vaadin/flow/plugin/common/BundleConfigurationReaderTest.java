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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class BundleConfigurationReaderTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private File configFile;

    @Before
    public void setUp() throws IOException {
        configFile = temporaryFolder.newFile("test.json");
    }

    @Test
    public void cannot_construct_from_file_containing_invalid_json() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("invalid json"), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(configFile.getAbsolutePath());

        new BundleConfigurationReader(configFile);
    }

    @Test
    public void non_array_fragments_property_throws() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'fragments':{}}"), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("fragments");

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void only_arrays_of_objects_allowed_in_the_fragments_array() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'fragments':['', []]}"), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("fragments");

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void no_fragments_present_returns_empty_set() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'no-fragments-here':{}}"), StandardCharsets.UTF_8);
        Map<String, Set<String>> fragments = new BundleConfigurationReader(configFile).getFragments();
        Assert.assertEquals("Expect to have empty fragments returned for empty fragments configuration", Collections.emptyMap(), fragments);
    }

    @Test
    public void empty_array_of_fragments_returns_empty_set() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'fragments':[]}"), StandardCharsets.UTF_8);
        Map<String, Set<String>> fragments = new BundleConfigurationReader(configFile).getFragments();
        Assert.assertEquals("Expect to have empty fragments returned for empty array in json", Collections.emptyMap(), fragments);
    }

    @Test
    public void fragments_without_name_throw_exception() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'fragments':[ {'files': ['one']} ]}"), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("name");

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_with_name_not_string_throw_exception2() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'fragments':[ {'name': {}, 'files': ['one']} ]}"), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("name");

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_with_empty_name_throw_exception() throws IOException {
        Files.write(configFile.toPath(), Collections.singletonList("{'fragments':[ {'name': '', 'files': ['one']} ]}"), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("name");

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_without_files_throw_exception() throws IOException {
        String fragmentName = "test";
        Files.write(configFile.toPath(), Collections.singletonList(String.format("{'fragments':[ {'name': '%s'} ]}", fragmentName)), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(fragmentName);

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_with_files_not_array_throw_exception() throws IOException {
        String fragmentName = "test";
        Files.write(configFile.toPath(), Collections.singletonList(String.format("{'fragments':[ {'name': '%s', 'files': {}} ]}", fragmentName)), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(fragmentName);

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_with_file_paths_not_string_throw_exception() throws IOException {
        String fragmentName = "test";
        Files.write(configFile.toPath(), Collections.singletonList(String.format("{'fragments':[ {'name': '%s', 'files': [20]} ]}", fragmentName)), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(fragmentName);

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_with_empty_files_throw_exception() throws IOException {
        String fragmentName = "test";
        Files.write(configFile.toPath(), Collections.singletonList(String.format("{'fragments':[ {'name': '%s', 'files': []} ]}", fragmentName)), StandardCharsets.UTF_8);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(fragmentName);

        new BundleConfigurationReader(configFile).getFragments();
    }

    @Test
    public void fragments_parsed_correctly() throws IOException {
        String tutorialExample = "{\n" +
                "  'fragments': [\n" +
                "    {\n" +
                "      'name': 'icons-fragment',\n" +
                "      'files': ['bower_components/vaadin-icons/vaadin-icons.html']\n" +
                "    },\n" +
                "    {\n" +
                "      'name': 'important-components',\n" +
                "      'files': [\n" +
                "         'bower_components/vaadin-form-layout/vaadin-form-layout.html',\n" +
                "         'bower_components/vaadin-form-layout/vaadin-form-item.html',\n" +
                "         'bower_components/vaadin-text-field/vaadin-text-field.html',\n" +
                "         'bower_components/vaadin-text-field/vaadin-password-field.html',\n" +
                "         'bower_components/vaadin-combo-box/vaadin-combo-box.html'\n" +
                "       ]\n" +
                "    },\n" +
                "    {\n" +
                "      'name': 'grid-fragment',\n" +
                "      'files': [\n" +
                "         'gridConnector.js',\n" +
                "         'vaadin-grid-flow-selection-column.html',\n" +
                "         'bower_components/vaadin-grid/vaadin-grid.html',\n" +
                "         'bower_components/vaadin-grid/vaadin-grid-column-group.html',\n" +
                "         'bower_components/vaadin-grid/vaadin-grid-sorter.html'\n" +
                "       ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Files.write(configFile.toPath(), Collections.singletonList(tutorialExample), StandardCharsets.UTF_8);
        Map<String, Set<String>> expectedResult = ImmutableMap.of(
                "icons-fragment", ImmutableSet.of("bower_components/vaadin-icons/vaadin-icons.html"),
                "important-components", ImmutableSet.of(
                        "bower_components/vaadin-form-layout/vaadin-form-layout.html",
                        "bower_components/vaadin-form-layout/vaadin-form-item.html",
                        "bower_components/vaadin-text-field/vaadin-text-field.html",
                        "bower_components/vaadin-text-field/vaadin-password-field.html",
                        "bower_components/vaadin-combo-box/vaadin-combo-box.html"
                ),
                "grid-fragment", ImmutableSet.of(
                        "gridConnector.js",
                        "vaadin-grid-flow-selection-column.html",
                        "bower_components/vaadin-grid/vaadin-grid.html",
                        "bower_components/vaadin-grid/vaadin-grid-column-group.html",
                        "bower_components/vaadin-grid/vaadin-grid-sorter.html"
                )
        );

        Map<String, Set<String>> fragments = new BundleConfigurationReader(configFile).getFragments();

        Assert.assertEquals("Expect tutorial example to be parsed fully into three fragments", expectedResult, fragments);
    }
}
