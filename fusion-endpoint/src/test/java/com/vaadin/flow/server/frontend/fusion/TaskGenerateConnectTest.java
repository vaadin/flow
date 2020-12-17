package com.vaadin.flow.server.frontend.fusion;

import java.io.File;
import java.io.IOException;

import com.vaadin.flow.server.frontend.TaskGenerateConnect;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
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
                "../../connect/generator/openapi/esmodule-generator" +
                        "-TwoEndpointsThreeMethods.json")
                .getPath());
    }

    @Test
    public void should_generate_Two_TypeScriptFiles() throws Exception {
        File ts1 = new File(outputDirectory, "FooBarEndpoint.ts");
        File ts2 = new File(outputDirectory, "FooFooEndpoint.ts");
        File client = new File(outputDirectory, "connect-client.default.ts");
        File frontendDirectory = outputDirectory.getParentFile();

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());
        assertFalse(client.exists());

        taskGenerateConnectTs = new TaskGenerateConnectImpl(properties, openApiJson, outputDirectory, frontendDirectory);
        taskGenerateConnectTs.execute();

        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
        assertTrue(client.exists());

        String output = FileUtils.readFileToString(client, "UTF-8");
        assertTrue(output
                .contains("import {ConnectClient} from '@vaadin/flow-frontend/Connect';"));
        assertTrue(output.contains(
                "const client = new ConnectClient({prefix: 'connect'});"));
        assertTrue(output.contains("export default client;"));
    }

    @Test
    public void should_use_custom_endpoint_name_when_connect_client_exists() throws Exception {
        File ts1 = new File(outputDirectory, "FooBarEndpoint.ts");
        File ts2 = new File(outputDirectory, "FooFooEndpoint.ts");
        File client = new File(outputDirectory, "connect-client.default.ts");
        File frontendDirectory = outputDirectory.getParentFile();
        File customConnectClient = temporaryFolder
                .newFile("connect-client.ts");

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());
        assertFalse(client.exists());
        assertTrue(customConnectClient.exists());

        taskGenerateConnectTs = new TaskGenerateConnectImpl(properties, openApiJson, outputDirectory, frontendDirectory);
        taskGenerateConnectTs.execute();

        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
        assertTrue(client.exists());

        String outputEndpoinTs1 = FileUtils.readFileToString(ts1, "UTF-8");
        String outputEndpoinTs2 = FileUtils.readFileToString(ts2, "UTF-8");
        assertThat(outputEndpoinTs1, containsString("import client from '../connect-client'"));
        assertThat(outputEndpoinTs2, containsString("import client from '../connect-client'"));
    }
}
