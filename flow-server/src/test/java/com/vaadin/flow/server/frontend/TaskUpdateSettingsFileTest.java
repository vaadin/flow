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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.TaskUpdateSettingsFile.DEV_SETTINGS_FILE;

public class TaskUpdateSettingsFileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Options options;

    private File buildDirectory;

    private static final Set<String> ABSOLUTE_PATH_ENTRIES = Set.of(
            "frontendFolder", "themeResourceFolder", "staticOutput",
            "statsOutput", "frontendBundleOutput", "devBundleOutput",
            "devBundleStatsOutput", "jarResourcesFolder",
            "clientServiceWorkerSource");

    @Before
    public void setUp() throws IOException {
        ClassFinder finder = Mockito.spy(new ClassFinder.DefaultClassFinder(
                this.getClass().getClassLoader()));
        buildDirectory = temporaryFolder.newFolder("target");
        options = new MockOptions(finder, temporaryFolder.getRoot())
                .withBuildDirectory("target").withJarFrontendResourcesFolder(
                        temporaryFolder.newFolder("resources"));
    }

    @Test
    public void execute_withWebappResourcesDirectory_useAbsolutePaths()
            throws IOException {

        options.withBuildResultFolders(
                Paths.get(buildDirectory.getPath(), "classes",
                        VAADIN_WEBAPP_RESOURCES).toFile(),
                Paths.get(buildDirectory.getPath(), "classes",
                        VAADIN_SERVLET_RESOURCES).toFile());
        TaskUpdateSettingsFile updateSettings = new TaskUpdateSettingsFile(
                options, "theme", new PwaConfiguration());
        updateSettings.execute();
        JsonNode settingsJson = readSettingsFile();
        assertPathsMatchProjectFolder(settingsJson);
    }

    @Test
    public void execute_withoutWebappResourcesDirectory_useAbsolutePaths()
            throws IOException {
        TaskUpdateSettingsFile updateSettings = new TaskUpdateSettingsFile(
                options, "theme", new PwaConfiguration());
        updateSettings.execute();
        JsonNode settingsJson = readSettingsFile();
        assertPathsMatchProjectFolder(settingsJson);
    }

    private JsonNode readSettingsFile() throws IOException {
        File settings = new File(temporaryFolder.getRoot(),
                "target/" + DEV_SETTINGS_FILE);
        JsonNode settingsJson = JacksonUtils.readTree(
                IOUtils.toString(settings.toURI(), StandardCharsets.UTF_8));
        return settingsJson;
    }

    private void assertPathsMatchProjectFolder(JsonNode json) {
        ABSOLUTE_PATH_ENTRIES.forEach(key -> {
            String path = json.get(key).asText();
            Assert.assertTrue(
                    "Expected '" + key + "' to have an absolute path matching "
                            + temporaryFolder.getRoot().getPath() + ", but was "
                            + path,
                    Paths.get(path)
                            .startsWith(temporaryFolder.getRoot().getPath()));
        });
    }

}