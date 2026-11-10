/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.Constants.TARGET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NodeTasksHillaTest {

    @TempDir
    File temporaryFolder;

    private static final String USER_DIR = "user.dir";

    private static String globalUserDirValue;
    private static String globalFrontendDirValue;

    private String userDir;

    private File propertiesDir;

    private AutoCloseable closeable;

    @Mock
    private EndpointGeneratorTaskFactory endpointGeneratorTaskFactory;

    @Mock
    private TaskGenerateOpenAPI taskGenerateOpenAPI;

    @Mock
    private TaskGenerateEndpoint taskGenerateEndpoint;

    @BeforeEach
    void setup() throws Exception {
        closeable = org.mockito.MockitoAnnotations.openMocks(this);
        userDir = temporaryFolder.getAbsolutePath();
        System.setProperty(USER_DIR, userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);

        propertiesDir = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
    }

    @AfterEach
    void releaseMocks() throws Exception {
        closeable.close();
    }

    @BeforeAll
    static void setupBeforeClass() {
        globalUserDirValue = System.getProperty(USER_DIR);
        globalFrontendDirValue = System.getProperty(PARAM_FRONTEND_DIR);
    }

    @AfterAll
    static void tearDownAfterClass() {
        setPropertyIfPresent(USER_DIR, globalUserDirValue);
        setPropertyIfPresent(PARAM_FRONTEND_DIR, globalFrontendDirValue);
    }

    private Options createOptions() {
        Lookup lookup = mock(Lookup.class);
        doReturn(new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(lookup).lookup(ClassFinder.class);
        File npmFolder = new File(userDir);
        return new Options(lookup, npmFolder).withBuildDirectory(TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .withRunNpmInstall(false).withEmbeddableWebComponents(false)
                .withJarFrontendResourcesFolder(new File(userDir,
                        FrontendUtils.GENERATED
                                + FrontendUtils.JAR_RESOURCES_FOLDER))
                .withFrontendGeneratedFolder(new File(userDir))
                .withBuildResultFolders(npmFolder, npmFolder)
                .setJavaResourceFolder(propertiesDir);
    }

    @Test
    void should_useHillaEngine_whenEnabled()
            throws ExecutionFailedException, IOException {
        Options options = createOptions();
        doReturn(taskGenerateOpenAPI).when(endpointGeneratorTaskFactory)
                .createTaskGenerateOpenAPI(any());
        doReturn(taskGenerateEndpoint).when(endpointGeneratorTaskFactory)
                .createTaskGenerateEndpoint(any());
        doReturn(endpointGeneratorTaskFactory).when(options.getLookup())
                .lookup(EndpointGeneratorTaskFactory.class);

        try (MockedStatic<FrontendBuildUtils> util = mockStatic(
                FrontendBuildUtils.class, Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> FrontendBuildUtils.isHillaUsed(any(), any()))
                    .thenReturn(true);

            new NodeTasks(options).execute();
        }

        verifyHillaEngine(true);
    }

    @Test
    void should_notHillaEngine_whenDisabled()
            throws ExecutionFailedException, IOException {
        Options options = createOptions();
        new NodeTasks(options).execute();
        verifyHillaEngine(false);
    }

    private static void setPropertyIfPresent(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        }
    }

    private void verifyHillaEngine(boolean expected)
            throws ExecutionFailedException {
        verify(endpointGeneratorTaskFactory, expected ? times(1) : never())
                .createTaskGenerateEndpoint(any());
        verify(endpointGeneratorTaskFactory, expected ? times(1) : never())
                .createTaskGenerateOpenAPI(any());
        verify(taskGenerateOpenAPI, expected ? times(1) : never()).execute();
        verify(taskGenerateEndpoint, expected ? times(1) : never()).execute();
    }
}
