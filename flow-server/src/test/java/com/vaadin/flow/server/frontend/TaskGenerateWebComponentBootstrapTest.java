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

import com.vaadin.flow.server.ExecutionFailedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_WEB_COMPONENT_NAME;

public class TaskGenerateWebComponentBootstrapTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendDirectory;
    private File generatedImports;
    private TaskGenerateWebComponentBootstrap taskGenerateWebComponentBootstrap;

    @Before
    public void setup() throws Exception {
        frontendDirectory = temporaryFolder.newFolder(DEFAULT_FRONTEND_DIR);
        File generatedFolder = temporaryFolder.newFolder(TARGET, FRONTEND);
        generatedImports = new File(generatedFolder,
                IMPORTS_WEB_COMPONENT_NAME);
        generatedImports.getParentFile().mkdirs();
        generatedImports.createNewFile();
        taskGenerateWebComponentBootstrap = new TaskGenerateWebComponentBootstrap(
                frontendDirectory, generatedImports);
    }

    @Test
    public void should_importGeneratedImports()
            throws ExecutionFailedException {
        taskGenerateWebComponentBootstrap.execute();
        String content = taskGenerateWebComponentBootstrap.getFileContent();
        Assert.assertTrue(content.contains(
                "import '../../target/frontend/generated-flow-webcomponent-imports.js'"));
    }

    @Test
    public void should_importAndInitializeFlowClient()
            throws ExecutionFailedException {
        taskGenerateWebComponentBootstrap.execute();
        String content = taskGenerateWebComponentBootstrap.getFileContent();
        Assert.assertTrue(content.contains(
                "import { init } from '" + FrontendUtils.JAR_RESOURCES_IMPORT
                        + "FlowClient.js';\n" + "init()"));
    }
}
