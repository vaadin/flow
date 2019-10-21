package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskGenerateConnectTsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskGenerateConnectTs taskGenerateConnectTs;
    private File outputDirectory;
    private File openApiJson;

    @Before
    public void setUp() throws IOException {
        outputDirectory = temporaryFolder.newFolder();
        openApiJson = new File(getClass().getResource(
                "../connect/generator/openapi/esmodule-generator-TwoServicesThreeMethods.json")
                .getPath());
    }

    @Test
    public void should_generate_Two_TypeScriptFiles() throws Exception {
        File ts1 = new File(outputDirectory, "FooBarService.ts");
        File ts2 = new File(outputDirectory, "FooFooService.ts");

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());

        taskGenerateConnectTs = new TaskGenerateConnectTs(openApiJson,
                outputDirectory);
        taskGenerateConnectTs.execute();

        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
    }

}
