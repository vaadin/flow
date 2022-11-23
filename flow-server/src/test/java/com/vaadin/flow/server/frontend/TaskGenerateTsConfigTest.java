/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.ExecutionFailedException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TaskGenerateTsConfigTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;
    private TaskGenerateTsConfig taskGenerateTsConfig;

    private FeatureFlags featureFlags;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        featureFlags = Mockito.mock(FeatureFlags.class);
        taskGenerateTsConfig = new TaskGenerateTsConfig(npmFolder,
                featureFlags);
    }

    @Test
    public void should_generateTsConfig_TsConfigNotExist() throws Exception {
        taskGenerateTsConfig.execute();
        Assert.assertFalse(
                "Should generate tsconfig.json when "
                        + "tsconfig.json doesn't exist",
                taskGenerateTsConfig.shouldGenerate());
        Assert.assertTrue("The generated tsconfig.json should not exist",
                taskGenerateTsConfig.getGeneratedFile().exists());
        Assert.assertEquals(
                "The generated content should be equals the default content",
                taskGenerateTsConfig.getFileContent(),
                IOUtils.toString(
                        taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }

    @Test
    public void viteShouldNotUseEs2019() throws Exception {
        taskGenerateTsConfig.execute();
        Assert.assertFalse("The config file should not use es2019", IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""));
    }

    @Test
    public void webpackShouldUseEs2019() throws Exception {
        Mockito.when(featureFlags.isEnabled((Feature) Mockito.any()))
                .thenAnswer(req -> {
                    if (req.getArgument(0) == FeatureFlags.WEBPACK) {
                        return true;
                    }
                    return false;
                });

        taskGenerateTsConfig.execute();
        Assert.assertTrue("The config file should use es2019", IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""));
    }

    @Test
    public void viteShouldUpgradeFromEs2019() throws Exception {
        AtomicBoolean useWebpack = new AtomicBoolean(true);
        Mockito.when(featureFlags.isEnabled((Feature) Mockito.any()))
                .thenAnswer(req -> {
                    if (req.getArgument(0) == FeatureFlags.WEBPACK) {
                        return useWebpack.get();
                    }
                    return false;
                });

        taskGenerateTsConfig.execute(); // Write a file with es2019
        Assert.assertTrue("The config file should use es2019", IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""));
        useWebpack.set(false);
        taskGenerateTsConfig.execute();
        Assert.assertFalse(
                "Vite should have upgraded the config file to not use es2019",
                IOUtils.toString(
                        taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                        .contains("\"target\": \"es2019\""));

    }

    @Test
    public void switchToWebpackShouldDowngradeToEs2019() throws Exception {
        AtomicBoolean useWebpack = new AtomicBoolean(false);
        Mockito.when(featureFlags.isEnabled((Feature) Mockito.any()))
                .thenAnswer(req -> {
                    if (req.getArgument(0) == FeatureFlags.WEBPACK) {
                        return useWebpack.get();
                    }
                    return false;
                });

        taskGenerateTsConfig.execute(); // Write a file without es2019
        Assert.assertFalse("The config file should not use es2019", IOUtils
                .toString(taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                .contains("\"target\": \"es2019\""));
        useWebpack.set(true);
        taskGenerateTsConfig.execute();
        Assert.assertTrue(
                "Webpack should have downgraded the config file to use es2019",
                IOUtils.toString(
                        taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8)
                        .contains("\"target\": \"es2019\""));

    }

    @Test
    public void should_notGenerateTsConfig_TsConfigExist() throws Exception {
        Files.createFile(new File(npmFolder, "tsconfig.json").toPath());
        taskGenerateTsConfig.execute();
        Assert.assertFalse(
                "Should not generate tsconfig.json when tsconfig.json exists",
                taskGenerateTsConfig.shouldGenerate());
        Assert.assertTrue("The tsconfig.json should already exist",
                taskGenerateTsConfig.getGeneratedFile().exists());
    }

    @Test
    public void tsConfigUpdated_remindsUpdateVersionAndTemplates()
            throws IOException {
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

        Assert.assertEquals("tsconfig.json content has been updated. "
                + "Please also: 1. Increment version in tsconfig.json (\"version\" property) "
                + "2. create a new tsconfig-vX.Y.json template in flow-server resources and put the old content there "
                + "3. update vaadinVersion array in TaskGenerateTsConfig with X.Y "
                + "4. put a new content in tsconfig-reference.json in tests",
                testTsConfig, tsConfigLatest);

    }

    @Test
    public void tsConfigHasLatestVersion_noUpdates()
            throws IOException, ExecutionFailedException {
        File tsconfig = new File(npmFolder, "tsconfig.json");
        Files.createFile(tsconfig.toPath());
        FileUtils.writeStringToFile(tsconfig, "{\"version\": \"23.3.0\"}",
                UTF_8);
        taskGenerateTsConfig.execute();

        String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);

        String expected = IOUtils.toString(
                Objects.requireNonNull(TaskGenerateTsConfigTest.class
                        .getClassLoader()
                        .getResourceAsStream("tsconfig-latest-version.json")),
                StandardCharsets.UTF_8);

        Assert.assertEquals(expected, tsConfigString);
    }

    @Test
    public void tsConfigHasCustomCodes_updatesAndThrows() throws IOException {
        File tsconfig = writeTestTsConfigContent(
                "tsconfig-custom-content.json");
        try {
            taskGenerateTsConfig.execute();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(
                    "TypeScript config file 'tsconfig.json' has been updated to the latest"));
            String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);
            Assert.assertTrue(tsConfigString.contains(
                    "\"@vaadin/flow-frontend\": [\"generated/jar-resources\"],"));
            return;
        }
        Assert.fail("Expected exception to be thrown");
    }

    @Test
    public void defaultTsConfig_updatesSilently()
            throws IOException, ExecutionFailedException {
        File tsconfig = writeTestTsConfigContent("tsconfig-default.json");
        taskGenerateTsConfig.execute();
        String tsConfigString = FileUtils.readFileToString(tsconfig, UTF_8);
        Assert.assertTrue(tsConfigString.contains(
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
}
