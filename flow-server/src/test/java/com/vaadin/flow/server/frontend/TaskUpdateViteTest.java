package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.PwaConfiguration;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.frontend.TaskUpdateSettingsFile.DEV_SETTINGS_FILE;

public class TaskUpdateViteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatedTemplate_correctSettingsPath() throws IOException {
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("build");
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
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("build");

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

        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("build");

        new TaskUpdateVite(options, null).execute();

        String template = IOUtils.toString(generatedConfigFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertNotEquals("Generated file should have been overwritten",
                importString, template);
    }

    @Test
    public void usedSettings_matchThoseCreatedToSettingsFile()
            throws IOException {
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("build");

        TaskUpdateVite task = new TaskUpdateVite(options, null);
        task.execute();

        File generatedConfigFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_GENERATED_CONFIG);

        String template = IOUtils.toString(generatedConfigFile.toURI(),
                StandardCharsets.UTF_8);
        options.withFrontendDirectory(temporaryFolder.newFolder("frontend"))
                .withBuildDirectory("target").withJarFrontendResourcesFolder(
                        temporaryFolder.newFolder("resources"));

        TaskUpdateSettingsFile updateSettings = new TaskUpdateSettingsFile(
                options, "theme", new PwaConfiguration());
        updateSettings.execute();
        File settings = new File(temporaryFolder.getRoot(),
                "target/" + DEV_SETTINGS_FILE);
        JsonObject settingsJson = Json.parse(
                IOUtils.toString(settings.toURI(), StandardCharsets.UTF_8));

        final Matcher matcher = Pattern
                .compile("settings\\.(?!json)([a-zA-z]*)").matcher(template);
        StringBuilder faulty = new StringBuilder();
        while (matcher.find()) {
            if (!settingsJson.hasKey(matcher.group(1))) {
                faulty.append(matcher.group(1)).append('\n');
            }
        }
        Assert.assertTrue(
                "Configuration uses settings keys\n" + faulty
                        + "that are not generated in settings file.",
                faulty.toString().isEmpty());
    }
}
