/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;

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
                "flow-generated-imports.js");
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
                "import '../../target/frontend/flow-generated-imports.js'"));
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
