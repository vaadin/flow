/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.MockLogger;

import static com.vaadin.flow.server.frontend.TaskGenerateTsConfig.ERROR_MESSAGE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class TaskGenerateTsConfigTest {
    private static final CharSequence DEFAULT_ES_TARGET = "es2023";
    private static final CharSequence NEWER_ES_TARGET = "es2024";

    static private String LATEST_VERSION = "9.1";

    @TempDir
    File temporaryFolder;

    private File npmFolder;
    private TaskGenerateTsConfig taskGenerateTsConfig;

    private FeatureFlags featureFlags;

    @BeforeEach
    void setUp() throws IOException {
        npmFolder = Files.createTempDirectory(temporaryFolder.toPath(), "tmp")
                .toFile();
        featureFlags = Mockito.mock(FeatureFlags.class);
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withFeatureFlags(featureFlags);

        taskGenerateTsConfig = new TaskGenerateTsConfig(options);
        taskGenerateTsConfig.warningEmitted = false;
    }

    @Test
    void should_generateTsConfig_TsConfigNotExist() throws Exception {
        taskGenerateTsConfig.execute();
        assertFalse(taskGenerateTsConfig.shouldGenerate(),
                "Should generate tsconfig.json when "
                        + "tsconfig.json doesn't exist");
        assertTrue(taskGenerateTsConfig.getGeneratedFile().exists(),
                "The generated tsconfig.json should not exist");
        assertEquals(taskGenerateTsConfig.getFileContent(),
                IOUtils.toString(
                        taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8),
                "The generated content should be equals the default content");
    }

    @Test
    void viteShouldNotUseEs2019() throws Exception {
        taskGenerateTsConfig.execute();
        assertFalse(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""),
                "The config file should not use es2019");
    }

    @Test
    void viteShouldUpgradeFromEs2019() throws Exception {
        // Write a file with es2019
        taskGenerateTsConfig.execute();
        String content = IOUtils.toString(
                taskGenerateTsConfig.getGeneratedFile().toURI(),
                StandardCharsets.UTF_8);
        content = content.replace(DEFAULT_ES_TARGET, "es2019");
        try (FileWriter fw = new FileWriter(
                taskGenerateTsConfig.getGeneratedFile(),
                StandardCharsets.UTF_8)) {
            fw.write(content);
        }
        assertTrue(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""),
                "The config file should use es2019");
        taskGenerateTsConfig.execute();
        assertFalse(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""),
                "Vite should have upgraded the config file to not use es2019");

    }

    @Test
    void viteShouldUpgradeFromEs2020() throws Exception {
        // Write a file with es2019
        taskGenerateTsConfig.execute();
        String content = IOUtils.toString(
                taskGenerateTsConfig.getGeneratedFile().toURI(),
                StandardCharsets.UTF_8);
        content = content.replace(DEFAULT_ES_TARGET, "es2020");
        try (FileWriter fw = new FileWriter(
                taskGenerateTsConfig.getGeneratedFile(),
                StandardCharsets.UTF_8)) {
            fw.write(content);
        }
        assertTrue(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2020\""),
                "The config file should use es2020");
        taskGenerateTsConfig.execute();
        assertFalse(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2020\""),
                "Vite should have upgraded the config file to not use es2020");

    }

    @Test
    void viteShouldNotDowngradeFromNewerEsVersion() throws Exception {
        // Write a file with es2020
        taskGenerateTsConfig.execute();
        String content = IOUtils.toString(
                taskGenerateTsConfig.getGeneratedFile().toURI(),
                StandardCharsets.UTF_8);
        content = content.replace(DEFAULT_ES_TARGET, NEWER_ES_TARGET);
        try (FileWriter fw = new FileWriter(
                taskGenerateTsConfig.getGeneratedFile(),
                StandardCharsets.UTF_8)) {
            fw.write(content);
        }
        assertTrue(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"" + NEWER_ES_TARGET + "\""),
                "The config file should use " + NEWER_ES_TARGET);
        taskGenerateTsConfig.execute();
        assertTrue(IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"" + NEWER_ES_TARGET + "\""),
                "Vite should not have changed the config file");

    }

    @Test
    void should_notGenerateTsConfig_TsConfigExist() throws Exception {
        Path tsconfig = Files
                .createFile(new File(npmFolder, "tsconfig.json").toPath());
        Files.writeString(tsconfig, "text", UTF_8);
        taskGenerateTsConfig.execute();
        assertFalse(taskGenerateTsConfig.shouldGenerate(),
                "Should not generate tsconfig.json when tsconfig.json exists");
        assertTrue(taskGenerateTsConfig.getGeneratedFile().exists(),
                "The tsconfig.json should already exist");
    }

    @Test
    void tsConfigUpdated_remindsUpdateVersionAndTemplates() throws IOException {
        File tsconfig = new File(npmFolder, "tsconfig.json");
        Files.createFile(tsconfig.toPath());
        FileUtils.writeStringToFile(tsconfig, "{}", UTF_8);
        try {
            taskGenerateTsConfig.execute();
        } catch (Exception ignore) {

        }

        String tsConfigLatest = FileUtils.readFileToString(tsconfig, UTF_8);

        String testTsConfig = IOUtils.toString(
                Objects.requireNonNull(TaskGenerateTsConfigTest.class
                        .getClassLoader()
                        .getResourceAsStream("tsconfig-reference.json")),
                StandardCharsets.UTF_8);

        assertEquals(testTsConfig, tsConfigLatest,
                "tsconfig.json content has been updated. "
                        + "Please also: 1. Increment version in tsconfig.json (\"_version\" property) "
                        + "2. create a new tsconfig-vX.Y.json template in flow-server resources and put the old content there "
                        + "3. update vaadinVersion array in TaskGenerateTsConfig with X.Y "
                        + "4. put a new content in tsconfig-reference.json in tests "
                        + "5. update LATEST_VERSION in TaskGenerateTsConfigTest");

    }

    @Test
    void tsConfigHasLatestVersion_noUpdates()
            throws IOException, ExecutionFailedException {
        File tsconfig = new File(npmFolder, "tsconfig.json");
        Files.createFile(tsconfig.toPath());
        FileUtils.writeStringToFile(tsconfig,
                "{\"flow_version\": \"" + LATEST_VERSION + "\"}", UTF_8);
        taskGenerateTsConfig.execute();

        String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);

        String expected = IOUtils
                .toString(
                        Objects.requireNonNull(TaskGenerateTsConfigTest.class
                                .getClassLoader().getResourceAsStream(
                                        "tsconfig-latest-version.json")),
                        StandardCharsets.UTF_8)
                .replace("latest", LATEST_VERSION);

        assertEquals(expected, tsConfigString);
    }

    @Test
    void tsConfigHasCustomCodes_updatesAndLogsWarning()
            throws IOException, ExecutionFailedException {
        File tsconfig = writeTestTsConfigContent(
                "tsconfig-custom-content.json");
        MockLogger logger = new MockLogger();
        try (MockedStatic<AbstractTaskClientGenerator> client = Mockito
                .mockStatic(AbstractTaskClientGenerator.class,
                        Mockito.CALLS_REAL_METHODS)) {
            client.when(() -> AbstractTaskClientGenerator.log())
                    .thenReturn(logger);
            taskGenerateTsConfig.execute();
        }
        String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);
        assertTrue(tsConfigString.contains(
                "\"@vaadin/flow-frontend\": [\"generated/jar-resources\"],"));
        assertTrue(logger.getLogs().contains(ERROR_MESSAGE));
    }

    @Test
    void warningIsLoggedOnlyOncePerRun()
            throws IOException, ExecutionFailedException {
        File tsconfig = writeTestTsConfigContent(
                "tsconfig-custom-content.json");
        MockLogger logger = new MockLogger();
        try (MockedStatic<AbstractTaskClientGenerator> client = Mockito
                .mockStatic(AbstractTaskClientGenerator.class,
                        Mockito.CALLS_REAL_METHODS)) {
            client.when(() -> AbstractTaskClientGenerator.log())
                    .thenReturn(logger);
            taskGenerateTsConfig.execute();
            assertTrue(logger.getLogs().contains(ERROR_MESSAGE));
            logger.clearLogs();
            tsconfig.delete();
            writeTestTsConfigContent("tsconfig-custom-content.json");
            taskGenerateTsConfig.execute();
            assertFalse(logger.getLogs().contains(ERROR_MESSAGE));
        }
    }

    @Test
    void defaultTsConfig_updatesSilently()
            throws IOException, ExecutionFailedException {
        File tsconfig = writeTestTsConfigContent("tsconfig-default.json");
        taskGenerateTsConfig.execute();
        String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);
        assertTrue(tsConfigString.contains(
                "\"@vaadin/flow-frontend\": [\"generated/jar-resources\"],"));
    }

    @Test
    void olderTsConfig_updatesSilently()
            throws IOException, ExecutionFailedException {
        File tsconfig = writeTestTsConfigContent("tsconfig-older.json");
        taskGenerateTsConfig.execute();
        String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);
        assertTrue(tsConfigString.contains(
                "\"@vaadin/flow-frontend\": [\"generated/jar-resources\"],"));
    }

    private File writeTestTsConfigContent(String s) throws IOException {
        File tsconfig = new File(npmFolder, "tsconfig.json");
        Files.createFile(tsconfig.toPath());
        String content = IOUtils.toString(
                Objects.requireNonNull(TaskGenerateTsConfigTest.class
                        .getClassLoader().getResourceAsStream(s)),
                StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(tsconfig, content, UTF_8);
        return tsconfig;
    }

    @Test
    void testIsOlder() {
        assertTrue(TaskGenerateTsConfig.isOlder("es2019", "es2020"));
        assertTrue(TaskGenerateTsConfig.isOlder("es2020", "es2021"));
        assertFalse(TaskGenerateTsConfig.isOlder("es2020", "es2020"));
        assertFalse(TaskGenerateTsConfig.isOlder("es2020", "es2019"));
        assertFalse(TaskGenerateTsConfig.isOlder("es2021", "es2019"));

        assertTrue(TaskGenerateTsConfig.isOlder("2019", "2021"));
    }
}
