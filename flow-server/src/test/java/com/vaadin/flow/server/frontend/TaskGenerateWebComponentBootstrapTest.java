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

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FEATURE_FLAGS_FILE_NAME;

public class TaskGenerateWebComponentBootstrapTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendDirectory;
    private File generatedImports;
    private TaskGenerateWebComponentBootstrap taskGenerateWebComponentBootstrap;

    @Before
    public void setup() throws Exception {
        frontendDirectory = temporaryFolder.newFolder(DEFAULT_FRONTEND_DIR);
        generatedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);
        generatedImports.getParentFile().mkdirs();
        generatedImports.createNewFile();
        FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Mockito.when(scanner.getThemeDefinition()).thenReturn(null);
        Options options = new Options(Mockito.mock(Lookup.class), null)
                .withFrontendDirectory(frontendDirectory)
                .withFrontendDependenciesScanner(scanner);

        taskGenerateWebComponentBootstrap = new TaskGenerateWebComponentBootstrap(
                options);
    }

    @Test
    public void should_importGeneratedImports()
            throws ExecutionFailedException {
        taskGenerateWebComponentBootstrap.execute();
        String content = taskGenerateWebComponentBootstrap.getFileContent();
        Assert.assertTrue(content.contains("import 'Frontend/generated/flow/"
                + FrontendUtils.IMPORTS_WEB_COMPONENT_NAME + "'"));
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

    @Test
    public void should_importFeatureFlagTS() throws ExecutionFailedException {
        taskGenerateWebComponentBootstrap.execute();
        String content = taskGenerateWebComponentBootstrap.getFileContent();
        Assert.assertTrue(content.contains(
                String.format("import './%s';", FEATURE_FLAGS_FILE_NAME)));
    }

}
