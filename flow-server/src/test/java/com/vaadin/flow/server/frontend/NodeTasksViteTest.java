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
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.frontend.NodeTestComponents.ExampleExperimentalComponent;
import com.vaadin.flow.server.frontend.NodeTestComponents.FlagView;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.VITE_GENERATED_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class NodeTasksViteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String USER_DIR = "user.dir";

    private static String globalUserDirValue;
    private static String globalFrontendDirValue;
    private static String globalGeneratedDirValue;

    private String userDir;

    @Before
    public void setup() {
        userDir = temporaryFolder.getRoot().getAbsolutePath();
        System.setProperty(USER_DIR, userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);
        System.clearProperty(PARAM_GENERATED_DIR);
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
    public void should_ExcludeExperimentalComponent_WhenFeatureDisabled()
            throws Exception {
        Class<?>[] classes = { FlagView.class,
                ExampleExperimentalComponent.class };

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        ClassFinder finder = NodeUpdateTestUtil.getClassFinder(classes);
        Mockito.doReturn(finder).when(mockedLookup).lookup(ClassFinder.class);

        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));

        assertEquals(1, finder.getAnnotatedClasses(JsModule.class).size());
        assertEquals(1, finder.getAnnotatedClasses(JavaScript.class).size());

        builder.build().execute();
        File importsFile = Paths
                .get(userDir, TARGET, DEFAULT_GENERATED_DIR, IMPORTS_NAME)
                .toFile();
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        assertFalse(content
                .contains("@vaadin/example-flag/experimental-module-1.js"));
        assertFalse(content
                .contains("@vaadin/example-flag/experimental-module-2.js"));
        assertFalse(content.contains("experimental-Connector.js"));
    }

    @Test
    public void should_IncludeExperimentalComponent_WhenFeatureEnabled()
            throws Exception {
        Class<?>[] classes = { FlagView.class,
                ExampleExperimentalComponent.class };

        File propertiesDir = temporaryFolder.newFolder();
        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME),
                "com.vaadin.experimental.exampleFeatureFlag=true\n",
                StandardCharsets.UTF_8);

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        ClassFinder finder = NodeUpdateTestUtil.getClassFinder(classes);
        Mockito.doReturn(finder).when(mockedLookup).lookup(ClassFinder.class);

        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));

        builder.build().execute();
        File importsFile = Paths
                .get(userDir, TARGET, DEFAULT_GENERATED_DIR, IMPORTS_NAME)
                .toFile();
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        assertTrue(content
                .contains("@vaadin/example-flag/experimental-module-1.js"));
        assertTrue(content
                .contains("@vaadin/example-flag/experimental-module-2.js"));
        assertTrue(content.contains("experimental-Connector.js"));
    }

    @Test
    public void should_UseDefaultFolders() throws Exception {
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));

        Assert.assertEquals(
                new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File) getFieldValue(builder, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                Paths.get(userDir, TARGET, DEFAULT_GENERATED_DIR).toFile()
                        .getAbsolutePath(),
                ((File) getFieldValue(builder, "generatedFolder"))
                        .getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(
                Paths.get(userDir, TARGET, DEFAULT_GENERATED_DIR, IMPORTS_NAME)
                        .toFile().exists());
    }

    @Test
    public void should_generateServiceWorkerWhenPwa() throws Exception {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));

        Assert.assertEquals(
                new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File) getFieldValue(builder, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                new File(new File(userDir, TARGET), DEFAULT_GENERATED_DIR)
                        .getAbsolutePath(),
                ((File) getFieldValue(builder, "generatedFolder"))
                        .getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(new File(userDir, Paths
                .get(TARGET, DEFAULT_GENERATED_DIR, IMPORTS_NAME).toString())
                        .exists());
    }

    @Test
    public void should_BeAbleToCustomizeFolders() throws Exception {
        System.setProperty(PARAM_FRONTEND_DIR, "my_custom_sources_folder");
        System.setProperty(PARAM_GENERATED_DIR, "my/custom/generated/folder");

        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));

        Assert.assertEquals(
                new File(userDir, "my_custom_sources_folder").getAbsolutePath(),
                ((File) getFieldValue(builder, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                new File(userDir, "my/custom/generated/folder")
                        .getAbsolutePath(),
                ((File) getFieldValue(builder, "generatedFolder"))
                        .getAbsolutePath());

        builder.build().execute();
        Assert.assertTrue(
                new File(userDir, "my/custom/generated/folder/" + IMPORTS_NAME)
                        .exists());
    }

    @Ignore("useClientSideIndexFileForBootstrapping is not written to vite config, bug?")
    @Test
    public void should_SetIsClientBootstrapMode_When_EnableClientSideBootstrapMode()
            throws ExecutionFailedException, IOException {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false)
                .enableImportsUpdate(true).runNpmInstall(false)
                .withEmbeddableWebComponents(false).useV14Bootstrap(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"))
                .withFrontendGeneratedFolder(new File(userDir,
                        DEFAULT_PROJECT_FRONTEND_GENERATED_DIR));
        builder.build().execute();
        String viteGeneratedContent = Files
                .lines(new File(userDir, VITE_GENERATED_CONFIG).toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "useClientSideIndexFileForBootstrapping should be true",
                viteGeneratedContent.contains(
                        "const useClientSideIndexFileForBootstrapping = true;"));
    }

    @Test
    public void should_GenerateTsConfigAndTsDefinitions_When_Vaadin14BootstrapMode()
            throws ExecutionFailedException {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).useV14Bootstrap(true)
                .enableImportsUpdate(true).runNpmInstall(false)
                .withEmbeddableWebComponents(false).useV14Bootstrap(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));
        builder.build().execute();

        Assert.assertTrue(new File(userDir, "tsconfig.json").exists());
        Assert.assertTrue(new File(userDir, "types.d.ts").exists());
    }

    @Test
    public void should_useHillaGeneartor_whenAvailable()
            throws ExecutionFailedException {
        verifyEndpointGeneratorWithHillaTask(true);
    }

    @Test
    public void should_useOldGenerator_whenHillaGeneratorNotAvailable()
            throws ExecutionFailedException {
        verifyEndpointGeneratorWithHillaTask(false);
    }

    private void verifyEndpointGeneratorWithHillaTask(boolean withHillaTask)
            throws ExecutionFailedException {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withFrontendGeneratedFolder(new File(userDir))
                .withEndpointSourceFolder(new File(userDir))
                .withEndpointGeneratedOpenAPIFile(new File(userDir))
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"));

        EndpointGeneratorTaskFactory endpointGeneratorFactory = mock(
                EndpointGeneratorTaskFactory.class);
        TaskGenerateOpenAPI mockGenerateOpenAPI = mock(
                TaskGenerateOpenAPI.class);
        TaskGenerateEndpoint mockGenerateEndpoint = mock(
                TaskGenerateEndpoint.class);
        Mockito.doReturn(mockGenerateOpenAPI).when(endpointGeneratorFactory)
                .createTaskGenerateOpenAPI(any(), any(), any(), any());
        Mockito.doReturn(mockGenerateEndpoint).when(endpointGeneratorFactory)
                .createTaskGenerateEndpoint(any(), any(), any(), any());
        Mockito.doReturn(endpointGeneratorFactory).when(mockedLookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        TaskGenerateHilla hillaTask = withHillaTask
                ? mock(TaskGenerateHilla.class)
                : null;

        Mockito.doReturn(hillaTask).when(mockedLookup)
                .lookup(TaskGenerateHilla.class);

        builder.build().execute();

        if (withHillaTask) {
            Mockito.verify(hillaTask, times(1)).execute();
        }

        Mockito.verify(endpointGeneratorFactory,
                withHillaTask ? never() : times(1))
                .createTaskGenerateEndpoint(any(), any(), any(), any());
        Mockito.verify(endpointGeneratorFactory,
                withHillaTask ? never() : times(1))
                .createTaskGenerateOpenAPI(any(), any(), any(), any());
        Mockito.verify(mockGenerateOpenAPI, withHillaTask ? never() : times(1))
                .execute();
        Mockito.verify(mockGenerateEndpoint, withHillaTask ? never() : times(1))
                .execute();
    }

    private static void setPropertyIfPresent(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        }
    }

    private Object getFieldValue(Object obj, String name) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }
}
