package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskGenerateConnectTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskGenerateConnect taskGenerateConnectTs;
    private File properties;
    private File outputDirectory;
    private File openApiJson;

    @Before
    public void setUp() throws IOException {
        outputDirectory = temporaryFolder.newFolder();
        properties = temporaryFolder
                .newFile("application.properties");
        openApiJson = new File(getClass().getResource(
                "../connect/generator/openapi/esmodule-generator-TwoServicesThreeMethods.json")
                .getPath());
    }

    @Test
    public void should_generate_Two_TypeScriptFiles() throws Exception {
        File ts1 = new File(outputDirectory, "FooBarService.ts");
        File ts2 = new File(outputDirectory, "FooFooService.ts");
        File client = new File(outputDirectory, "connect-client.default.ts");

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());
        assertFalse(client.exists());

        taskGenerateConnectTs = new TaskGenerateConnect(properties,
                openApiJson, outputDirectory);

        taskGenerateConnectTs.execute();

        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
        assertTrue(client.exists());

        String output = FileUtils.readFileToString(client, "UTF-8");
        assertTrue(output
                .contains("import {ConnectClient} from '@vaadin/flow-frontend/Connect';"));
        assertTrue(output.contains(
                "const client = new ConnectClient({endpoint: 'connect'});"));
        assertTrue(output.contains("export default client;"));
    }

}
