package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.connect.generator.VaadinConnectClientGenerator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskGenerateConnectClientTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File applicationPropertiesFile;
    private TaskGenerateConnectClient task;
    private File generatedClientFile;

    @Before
    public void setUp() throws IOException {
        applicationPropertiesFile = temporaryFolder
                .newFile("application.properties");

        generatedClientFile = new File(temporaryFolder.newFolder(),
                VaadinConnectClientGenerator.DEFAULT_GENERATED_CONNECT_CLIENT_NAME);

        FileUtils.deleteQuietly(generatedClientFile);
    }

    @Test
    public void test() throws Exception {
        assertFalse(generatedClientFile.exists());
        task = new TaskGenerateConnectClient(applicationPropertiesFile,
                generatedClientFile);
        task.execute();
        assertTrue(generatedClientFile.exists());

        String output = FileUtils.readFileToString(generatedClientFile,
                "UTF-8");
        assertTrue(output
                .contains("import {ConnectClient} from '@vaadin/connect';"));
        assertTrue(output.contains(
                "const client = new ConnectClient({endpoint: '/connect'});"));
        assertTrue(output.contains("export default client;"));
    }
}
