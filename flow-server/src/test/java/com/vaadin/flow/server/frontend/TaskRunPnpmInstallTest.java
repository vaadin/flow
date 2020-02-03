/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.ExecutionFailedException;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;

public class TaskRunPnpmInstallTest extends TaskRunNpmInstallTest {

    private static final String PINNED_VERSION = "3.2.17";

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        FrontendUtils.ensurePnpm(getNodeUpdater().npmFolder.getAbsolutePath());
    }

    @Test
    public void runPnpmInstall_overlayVersionIsPinnedViaPlatform_installedOverlayVersionIsSpecifiedByPlatform()
            throws IOException, ExecutionFailedException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file: dialog doesn't pin its Overlay version which
        // is transitive dependency.
        FileUtils.write(packageJson,
                "{\"dependencies\": {"
                        + "\"@vaadin/vaadin-dialog\": \"2.2.1\"}}",
                StandardCharsets.UTF_8);

        // Platform defines a pinned version
        TaskRunNpmInstall task = createTask(
                "{ \"@vaadin/vaadin-overlay\":\"" + PINNED_VERSION + "\"}");
        task.execute();

        File overlayPackageJson = new File(getNodeUpdater().nodeModulesFolder,
                "@vaadin/vaadin-overlay/package.json");

        // The resulting version should be the one specified via platform
        // versions file
        JsonObject overlayPackage = Json.parse(FileUtils
                .readFileToString(overlayPackageJson, StandardCharsets.UTF_8));
        Assert.assertEquals(PINNED_VERSION,
                overlayPackage.getString("version"));
    }

    @Override
    protected String getToolName() {
        return "pnpm";
    }

    @Override
    protected TaskRunNpmInstall createTask() {
        return new TaskRunNpmInstall(getNodeUpdater(), true) {
            @Override
            protected String generateVersionsJson() {
                return null;
            }
        };
    }

    protected TaskRunNpmInstall createTask(String versionsContent) {
        return new TaskRunNpmInstall(getNodeUpdater(), true) {
            @Override
            protected String generateVersionsJson() {
                try {
                    FileUtils.write(
                            new File(getNodeUpdater().npmFolder,
                                    "versions.json"),
                            versionsContent, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return "./versions.json";
            }
        };
    }
}
