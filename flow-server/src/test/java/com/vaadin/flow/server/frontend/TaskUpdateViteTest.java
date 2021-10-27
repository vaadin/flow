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

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.frontend.TaskUpdateSettingsFile.DEV_SETTINGS_FILE;

public class TaskUpdateViteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatedTemplate_correctSettingsPath() throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(temporaryFolder.getRoot(),
                "build");
        task.execute();

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertTrue("Settings file folder was not correctly updated.",
                template.contains("./build/" + DEV_SETTINGS_FILE));
    }

    @Test
    public void templateExists_correctSettingsPath_fileNotWritten()
            throws IOException {
        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_CONFIG);
        final String importString = "import settings from './build/"
                + DEV_SETTINGS_FILE + "';";
        FileUtils.write(configFile, importString, StandardCharsets.UTF_8);

        new TaskUpdateVite(temporaryFolder.getRoot(), "build").execute();

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertEquals("Settings file content was changed", importString,
                template);
    }

    @Test
    public void templateExists_faultySettingsPath_onlyPathUpdated()
            throws IOException {
        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_CONFIG);
        final String importString = "import settings from './target/"
                + DEV_SETTINGS_FILE + "';";
        FileUtils.write(configFile, importString, StandardCharsets.UTF_8);

        new TaskUpdateVite(temporaryFolder.getRoot(), "build").execute();

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);

        Assert.assertEquals("Settings file content was added.",
                "import settings from './build/" + DEV_SETTINGS_FILE + "';",
                template);
    }

    @Test
    public void usedSettings_matchThoseCreatedToSettingsFile()
            throws IOException {
        TaskUpdateVite task = new TaskUpdateVite(temporaryFolder.getRoot(),
                "build");
        task.execute();

        File configFile = new File(temporaryFolder.getRoot(),
                FrontendUtils.VITE_CONFIG);

        String template = IOUtils.toString(configFile.toURI(),
                StandardCharsets.UTF_8);
        NodeTasks.Builder builder = new NodeTasks.Builder(
                Mockito.mock(Lookup.class), temporaryFolder.getRoot(),
                temporaryFolder.newFolder("generated"),
                temporaryFolder.newFolder("frontend"), "target");

        TaskUpdateSettingsFile updateSettings = new TaskUpdateSettingsFile(
                builder, "theme");
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
