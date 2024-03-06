/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.*;
import static org.mockito.Mockito.*;

public class NodeTasksHillaTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String USER_DIR = "user.dir";

    private static String globalUserDirValue;
    private static String globalFrontendDirValue;
    private static String globalGeneratedDirValue;

    private String userDir;

    private File propertiesDir;

    @Mock
    private Lookup lookup;

    @Mock
    private EndpointGeneratorTaskFactory endpointGeneratorTaskFactory;

    @Mock
    private TaskGenerateOpenAPI taskGenerateOpenAPI;

    @Mock
    private TaskGenerateEndpoint taskGenerateEndpoint;

    @Mock
    private TaskGenerateHilla taskGenerateHilla;

    private Options options;

    @Before
    public void setup() throws Exception {
        userDir = temporaryFolder.getRoot().getAbsolutePath();
        System.setProperty(USER_DIR, userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);
        System.clearProperty(PARAM_GENERATED_DIR);

        propertiesDir = temporaryFolder.newFolder();

        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(lookup).lookup(ClassFinder.class);
        Mockito.doReturn(taskGenerateOpenAPI).when(endpointGeneratorTaskFactory)
                .createTaskGenerateOpenAPI(any(), any(), any(), any());
        Mockito.doReturn(taskGenerateEndpoint)
                .when(endpointGeneratorTaskFactory)
                .createTaskGenerateEndpoint(any(), any(), any(), any());
        Mockito.doReturn(endpointGeneratorTaskFactory).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        Mockito.doReturn(taskGenerateHilla).when(lookup)
                .lookup(TaskGenerateHilla.class);

        options = new Options(lookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withJarFrontendResourcesFolder(new File(userDir,
                        FrontendUtils.GENERATED
                                + FrontendUtils.JAR_RESOURCES_FOLDER))
                .withFrontendGeneratedFolder(new File(userDir))
                .withEndpointSourceFolder(new File(userDir))
                .withEndpointGeneratedOpenAPIFile(new File(userDir))
                .setJavaResourceFolder(propertiesDir);
    }

    @BeforeClass
    public static void setupBeforeClass() {
        globalUserDirValue = System.getProperty(USER_DIR);
        globalFrontendDirValue = System.getProperty(PARAM_FRONTEND_DIR);
        globalGeneratedDirValue = System.getProperty(PARAM_GENERATED_DIR);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        setPropertyIfPresent(USER_DIR, globalUserDirValue);
        setPropertyIfPresent(PARAM_FRONTEND_DIR, globalFrontendDirValue);
        setPropertyIfPresent(PARAM_GENERATED_DIR, globalGeneratedDirValue);
    }

    @Test
    public void should_useHillaEngine_whenEnabled()
            throws ExecutionFailedException, IOException {
        runEndpointTasks(true);
        verifyHillaTask(true);
        verifyOldGenerator(false);
    }

    @Test
    public void should_useOldGenerator_whenHillaGeneratorNotEnabled()
            throws ExecutionFailedException, IOException {
        runEndpointTasks(false);
        verifyHillaTask(false);
        verifyOldGenerator(true);
    }

    @Test
    public void should_notHillaEngine_whenOpenAPIFileIsNull()
            throws ExecutionFailedException, IOException {
        options.withEndpointGeneratedOpenAPIFile(null);

        runEndpointTasks(true);
        verifyHillaTask(false);
        verifyOldGenerator(false);
    }

    @Test
    public void should_notUseOldGenerator_whenOpenAPIFileIsNull()
            throws ExecutionFailedException, IOException {
        options.withEndpointGeneratedOpenAPIFile(null);

        runEndpointTasks(false);
        verifyHillaTask(false);
        verifyOldGenerator(false);
    }

    private void runEndpointTasks(boolean withHillaTask)
            throws ExecutionFailedException, IOException {
        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME),
                String.format("com.vaadin.experimental.hillaEngine=%s\n",
                        withHillaTask),
                StandardCharsets.UTF_8);

        new NodeTasks(options).execute();
    }

    private static void setPropertyIfPresent(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        }
    }

    private void verifyHillaTask(boolean expected)
            throws ExecutionFailedException {
        Mockito.verify(taskGenerateHilla, expected ? times(1) : never())
                .execute();
    }

    private void verifyOldGenerator(boolean expected)
            throws ExecutionFailedException {
        Mockito.verify(endpointGeneratorTaskFactory,
                expected ? times(1) : never())
                .createTaskGenerateEndpoint(any(), any(), any(), any());
        Mockito.verify(endpointGeneratorTaskFactory,
                expected ? times(1) : never())
                .createTaskGenerateOpenAPI(any(), any(), any(), any());
        Mockito.verify(taskGenerateOpenAPI, expected ? times(1) : never())
                .execute();
        Mockito.verify(taskGenerateEndpoint, expected ? times(1) : never())
                .execute();
    }
}
