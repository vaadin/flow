/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;

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

    private String userDir;

    private File propertiesDir;

    @Mock
    private EndpointGeneratorTaskFactory endpointGeneratorTaskFactory;

    @Mock
    private TaskGenerateOpenAPI taskGenerateOpenAPI;

    @Mock
    private TaskGenerateEndpoint taskGenerateEndpoint;

    @Before
    public void setup() throws Exception {
        userDir = temporaryFolder.getRoot().getAbsolutePath();
        System.setProperty(USER_DIR, userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);

        propertiesDir = temporaryFolder.newFolder();
    }

    @BeforeClass
    public static void setupBeforeClass() {
        globalUserDirValue = System.getProperty(USER_DIR);
        globalFrontendDirValue = System.getProperty(PARAM_FRONTEND_DIR);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        setPropertyIfPresent(USER_DIR, globalUserDirValue);
        setPropertyIfPresent(PARAM_FRONTEND_DIR, globalFrontendDirValue);
    }

    private Options createOptions() {
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(lookup).lookup(ClassFinder.class);
        return new Options(lookup, new File(userDir)).withBuildDirectory(TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .withRunNpmInstall(false).withEmbeddableWebComponents(false)
                .withJarFrontendResourcesFolder(new File(userDir,
                        FrontendUtils.GENERATED
                                + FrontendUtils.JAR_RESOURCES_FOLDER))
                .withFrontendGeneratedFolder(new File(userDir))
                .setJavaResourceFolder(propertiesDir);
    }

    @Test
    public void should_useHillaEngine_whenEnabled()
            throws ExecutionFailedException, IOException {
        Options options = createOptions();
        Mockito.doReturn(taskGenerateOpenAPI).when(endpointGeneratorTaskFactory)
                .createTaskGenerateOpenAPI(any());
        Mockito.doReturn(taskGenerateEndpoint)
                .when(endpointGeneratorTaskFactory)
                .createTaskGenerateEndpoint(any());
        Mockito.doReturn(endpointGeneratorTaskFactory).when(options.getLookup())
                .lookup(EndpointGeneratorTaskFactory.class);

        new NodeTasks(options).execute();
        verifyHillaEngine(true);
    }

    @Test
    public void should_notHillaEngine_whenDisabled()
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
        Mockito.verify(endpointGeneratorTaskFactory,
                expected ? times(1) : never())
                .createTaskGenerateEndpoint(any());
        Mockito.verify(endpointGeneratorTaskFactory,
                expected ? times(1) : never()).createTaskGenerateOpenAPI(any());
        Mockito.verify(taskGenerateOpenAPI, expected ? times(1) : never())
                .execute();
        Mockito.verify(taskGenerateEndpoint, expected ? times(1) : never())
                .execute();
    }
}
