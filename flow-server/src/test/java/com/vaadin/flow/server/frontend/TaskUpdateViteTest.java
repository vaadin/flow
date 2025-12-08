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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.frontend.TaskUpdateSettingsFile.DEV_SETTINGS_FILE;

public class TaskUpdateViteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassFinder finder;

    private Options options;

    @Before
    public void setUp() throws IOException {
        finder = Mockito.spy(new ClassFinder.DefaultClassFinder(
                this.getClass().getClassLoader()));
        options = new MockOptions(finder, temporaryFolder.getRoot())
                .withBuildDirectory("build");
    }

    @Test
    public void generatedTemplate_correctSettingsPath() throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(options, null);
        task.execute();

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertTrue("Settings file folder was not correctly updated.",
                template.contains("./build/" + DEV_SETTINGS_FILE));
    }

    @Test
    public void configFileExists_fileNotOverwritten() throws IOException {
        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_CONFIG);
        final String importString = "Hello Fake configuration";
        FileUtils.write(configFile, importString, StandardCharsets.UTF_8);

        new TaskUpdateVite(options, null).execute();

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertEquals("Settings file content was changed", importString,
                template);
    }

    @Test
    public void generatedConfigFileExists_alwaysOverwritten()
            throws IOException {
        File generatedConfigFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);
        final String importString = "Hello Fake generated configuration";
        FileUtils.write(generatedConfigFile, importString,
                StandardCharsets.UTF_8);

        new TaskUpdateVite(options, null).execute();

        String template = IOUtils.toString(generatedConfigFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertNotEquals("Generated file should have been overwritten",
                importString, template);
    }

    @Test
    public void usedSettings_matchThoseCreatedToSettingsFile()
            throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(options, null);
        task.execute();

        File generatedConfigFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(generatedConfigFile.toURI(),
                StandardCharsets.UTF_8);
        options.withFrontendDirectory(
                temporaryFolder.newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR))
                .withBuildDirectory("target").withJarFrontendResourcesFolder(
                        temporaryFolder.newFolder("resources"));

        TaskUpdateSettingsFile updateSettings = new TaskUpdateSettingsFile(
                options, "theme", new PwaConfiguration());
        updateSettings.execute();
        File settings = new File(temporaryFolder.getRoot(),
                "target/" + DEV_SETTINGS_FILE);
        ObjectNode settingsJson = JacksonUtils.readTree(
                IOUtils.toString(settings.toURI(), StandardCharsets.UTF_8));

        final Matcher matcher = Pattern
                .compile("settings\\.(?!json)([a-zA-z][a-zA-z0-9]*)")
                .matcher(template);
        StringBuilder faulty = new StringBuilder();
        while (matcher.find()) {
            if (!settingsJson.has(matcher.group(1))) {
                faulty.append(matcher.group(1)).append('\n');
            }
        }
        Assert.assertTrue(
                "Configuration uses settings keys\n" + faulty
                        + "that are not generated in settings file.",
                faulty.toString().isEmpty());
    }

    @Test
    public void generatedTemplate_reactAndHillaUsed_correctFileRouterImport()
            throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(options.withReact(true), null);
        try (MockedStatic<FrontendUtils> util = Mockito
                .mockStatic(FrontendUtils.class, Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> FrontendUtils.isHillaUsed(Mockito.any(),
                    Mockito.any())).thenReturn(true);
            task.execute();
        }

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertTrue("vitePluginFileSystemRouter should be imported.",
                template.contains("import vitePluginFileSystemRouter from '"
                        + TaskUpdateVite.FILE_SYSTEM_ROUTER_DEPENDENCY + "';"));
        Assert.assertTrue(
                "vitePluginFileSystemRouter({isDevMode: devMode}) should be used.",
                template.contains(
                        "vitePluginFileSystemRouter({isDevMode: devMode}),"));
    }

    @Test
    public void generatedTemplate_reactDisabled_correctFileRouterImport()
            throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(options.withReact(false),
                null);
        task.execute();

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertFalse("vitePluginFileSystemRouter should not be imported.",
                template.contains("import vitePluginFileSystemRouter from '"
                        + TaskUpdateVite.FILE_SYSTEM_ROUTER_DEPENDENCY + "';"));
        Assert.assertFalse("vitePluginFileSystemRouter() should be used.",
                template.contains("vitePluginFileSystemRouter(),"));

    }

    @Test
    public void generatedTemplate_extraFrontendExtension_addedToViteConfiguration()
            throws IOException {
        options.withFrontendExtraFileExtensions(
                Arrays.asList(".svg", ".ico", "png"));
        TaskUpdateVite task = new TaskUpdateVite(options, null);
        task.execute();

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);
        Pattern matchSelection = Pattern
                .compile("const projectFileExtensions = \\[(.*)];");
        Matcher matcher = matchSelection.matcher(template);
        Assert.assertTrue("No projectFileExtensions found", matcher.find());
        Assert.assertEquals(
                "Extra frontend extensions should be added to vite configuration, but was not.",
                "'.js', '.js.map', '.ts', '.ts.map', '.tsx', '.tsx.map', '.css', '.css.map', '.svg', '.ico', '.png'",
                matcher.group(1));
    }

    @Test
    public void generatedTemplate_noEraFrontendExtension_viteConfigurationWithoutExtraSelections()
            throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(options, null);
        task.execute();

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);
        Pattern matchSelection = Pattern
                .compile("const projectFileExtensions = \\[(.*)];");
        Matcher matcher = matchSelection.matcher(template);
        Assert.assertTrue("No projectFileExtensions found", matcher.find());
        Assert.assertEquals(
                "Extra frontend extensions should be added to vite configuration, but was not.",
                "'.js', '.js.map', '.ts', '.ts.map', '.tsx', '.tsx.map', '.css', '.css.map'",
                matcher.group(1));
    }
}
