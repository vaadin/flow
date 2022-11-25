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

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;

public class TaskRunPnpmInstallTest {

        private static final String PINNED_VERSION = "3.2.17";

        protected NodeUpdater nodeUpdater;

        protected File npmFolder;

        protected ClassFinder finder;

        protected File generatedPath;

        @Rule
        public TemporaryFolder temporaryFolder = new TemporaryFolder();

        @Before
        public void setUp() throws IOException {
                npmFolder = temporaryFolder.newFolder();
                generatedPath = new File(npmFolder, "generated");
                generatedPath.mkdir();
                finder = Mockito.mock(ClassFinder.class);
                nodeUpdater = new NodeUpdater(finder,
                                Mockito.mock(FrontendDependencies.class), npmFolder,
                                generatedPath, TARGET, Mockito.mock(FeatureFlags.class)) {

                        @Override
                        public void execute() {
                        }

                        @Override
                        Logger log() {
                                return LoggerFactory.getLogger(getClass());
                        }

                };
        }

        @Test
        public void a1() throws Exception {
                tst();
        }

        @Test
        public void a2() throws Exception {
                tst();
        }

        @Test
        public void a3() throws Exception {
                tst();
        }

        @Test
        public void a4() throws Exception {
                tst();
        }

        @Test
        public void a5() throws Exception {
                tst();
        }

        @Test
        public void a6() throws Exception {
                tst();
        }

        @Test
        public void a7() throws Exception {
                tst();
        }

        @Test
        public void a8() throws Exception {
                tst();
        }

        @Test
        public void a9() throws Exception {
                tst();
        }

        @Test
        public void a10() throws Exception {
                tst();
        }

        @Test
        public void b1() throws Exception {
                tst();
        }

        @Test
        public void b2() throws Exception {
                tst();
        }

        @Test
        public void b3() throws Exception {
                tst();
        }

        @Test
        public void b4() throws Exception {
                tst();
        }

        @Test
        public void b5() throws Exception {
                tst();
        }

        @Test
        public void b6() throws Exception {
                tst();
        }

        @Test
        public void b7() throws Exception {
                tst();
        }

        @Test
        public void b8() throws Exception {
                tst();
        }

        @Test
        public void b9() throws Exception {
                tst();
        }

        @Test
        public void b10() throws Exception {
                tst();
        }

        public void tst()
                        throws IOException, ExecutionFailedException {
                File packageJson = new File(nodeUpdater.npmFolder, PACKAGE_JSON);
                packageJson.createNewFile();

                // Write package json file: dialog doesn't pin its Overlay
                // version which
                // is transitive dependency.
                FileUtils.write(packageJson,
                                "{\"dependencies\": {"
                                                + "\"@vaadin/vaadin-dialog\": \"2.2.1\"}}",
                                StandardCharsets.UTF_8);

                runNpmInstall();
                // // Platform defines a pinned version
                // TaskRunNpmInstall task = createTask(
                // "{ \"@vaadin/vaadin-overlay\":\"" + PINNED_VERSION + "\"}");
                // task.execute();
        }

        private void runNpmInstall() throws ExecutionFailedException {
                String baseDir = nodeUpdater.npmFolder.getAbsolutePath();

                FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
                settings.setNodeDownloadRoot(URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
                settings.setForceAlternativeNode(false);
                settings.setUseGlobalPnpm(false);
                settings.setAutoUpdate(false);
                settings.setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION);
                FrontendTools tools = new FrontendTools(settings);
                // tools.validateNodeAndNpmVersion();


                List<String> npmExecutable;
                List<String> npmInstallCommand;

                try {
                        // TaskRunNpmInstall.validateInstalledNpm(tools);
                        npmExecutable = tools.getPnpmExecutable();
                        npmInstallCommand = new ArrayList<>(npmExecutable);

                } catch (IllegalStateException exception) {
                        throw new ExecutionFailedException(exception.getMessage(),
                                        exception);
                }

                npmInstallCommand.add("--ignore-scripts");
                npmInstallCommand.add("install");

                String toolName = "pnpm";

                Process process = null;
                try {
                        process = runNpmCommand(npmInstallCommand,
                                        nodeUpdater.npmFolder);

                        // logger.debug("Output of `{}`:", commandString);
                        StringBuilder toolOutput = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(process.getInputStream(),
                                                        StandardCharsets.UTF_8))) {
                                String stdoutLine;
                                while ((stdoutLine = reader.readLine()) != null) {
                                        // logger.debug(stdoutLine);
                                        toolOutput.append(stdoutLine)
                                                        .append(System.lineSeparator());
                                }
                        }

                        int errorCode = process.waitFor();

                        if (errorCode != 0) {
                                throw new ExecutionFailedException(
                                                SharedUtil.capitalize(toolName)
                                                                + " install has exited with non zero status. "
                                                                + "Some dependencies are not installed. Check "
                                                                + toolName + " command output");
                        }
                } catch (InterruptedException | IOException e) {
                        if (e instanceof InterruptedException) {
                                // Restore interrupted state
                                Thread.currentThread().interrupt();
                        }
                        throw new ExecutionFailedException(
                                        "Command '" + toolName + " install' failed to finish", e);
                } finally {
                        if (process != null) {
                                process.destroyForcibly();
                        }
                }

        }

        private Process runNpmCommand(List<String> command, File workingDirectory)
                        throws IOException {
                ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
                builder.environment().put("ADBLOCK", "1");
                builder.environment().put("NO_UPDATE_NOTIFIER", "1");
                builder.directory(workingDirectory);
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
                builder.redirectError(ProcessBuilder.Redirect.INHERIT);

                Process process = builder.start();

                // This will allow to destroy the process which does IO regardless
                // whether it's executed in the same thread or another (may be
                // daemon) thread
                Runtime.getRuntime()
                                .addShutdownHook(new Thread(process::destroyForcibly));

                return process;
        }

        protected TaskRunNpmInstall createTask(String versionsContent) {
                return new TaskRunNpmInstall(createAndRunNodeUpdater(versionsContent),
                                true, false, FrontendTools.DEFAULT_NODE_VERSION,
                                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT), false,
                                false, new ArrayList<>());
        }

        private NodeUpdater createAndRunNodeUpdater(String versionsContent) {
                NodeUpdater nodeUpdater = createNodeUpdater(versionsContent);
                try {
                        nodeUpdater.execute();
                } catch (Exception e) {
                        throw new IllegalStateException(
                                        "NodeUpdater failed to genereate the versions.json file");
                }

                return nodeUpdater;
        }

        private NodeUpdater createNodeUpdater(String versionsContent) {
                return new NodeUpdater(finder, Mockito.mock(FrontendDependencies.class),
                                npmFolder, generatedPath, TARGET,
                                Mockito.mock(FeatureFlags.class)) {

                        @Override
                        public void execute() {
                                try {
                                        versionsPath = generateVersionsJson(Json.createObject());
                                } catch (Exception e) {
                                        versionsPath = null;
                                }
                        }

                        @Override
                        protected String generateVersionsJson(JsonObject packageJson)
                                        throws IOException {
                                try {
                                        if (versionsContent != null) {
                                                FileUtils.write(new File(npmFolder, "versions.json"),
                                                                versionsContent, StandardCharsets.UTF_8);
                                        }
                                } catch (IOException e) {
                                        throw new RuntimeException(e);
                                }
                                return "./versions.json";
                        }
                };
        }

}
