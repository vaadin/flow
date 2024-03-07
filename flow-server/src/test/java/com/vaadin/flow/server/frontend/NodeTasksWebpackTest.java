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
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
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
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class NodeTasksWebpackTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String USER_DIR = "user.dir";

    private static String globalUserDirValue;
    private static String globalFrontendDirValue;
    private static String globalGeneratedDirValue;

    private String userDir;

    private File propertiesDir;

    @Before
    public void setup() throws Exception {
        userDir = temporaryFolder.getRoot().getAbsolutePath();
        System.setProperty(USER_DIR, userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);
        System.clearProperty(PARAM_GENERATED_DIR);

        propertiesDir = temporaryFolder.newFolder();

        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME),
                "com.vaadin.experimental.webpackForFrontendBuild=true\n",
                StandardCharsets.UTF_8);
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

        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir);

        assertEquals(1, finder.getAnnotatedClasses(JsModule.class).size());
        assertEquals(1, finder.getAnnotatedClasses(JavaScript.class).size());

        new NodeTasks(options).execute();
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

        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME),
                "com.vaadin.experimental.exampleFeatureFlag=true\n"
                        + "com.vaadin.experimental.webpackForFrontendBuild=true\n",
                StandardCharsets.UTF_8);

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        ClassFinder finder = NodeUpdateTestUtil.getClassFinder(classes);
        Mockito.doReturn(finder).when(mockedLookup).lookup(ClassFinder.class);

        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir);

        new NodeTasks(options).execute();
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
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir);

        Assert.assertEquals(
                new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File) getFieldValue(options, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                Paths.get(userDir, TARGET, DEFAULT_GENERATED_DIR).toFile()
                        .getAbsolutePath(),
                ((File) getFieldValue(options, "generatedFolder"))
                        .getAbsolutePath());

        new NodeTasks(options).execute();
        Assert.assertTrue(
                Paths.get(userDir, TARGET, DEFAULT_GENERATED_DIR, IMPORTS_NAME)
                        .toFile().exists());
    }

    @Test
    public void should_notExecuteTaskNotifyWebpackConfExistenceWhileUsingVite_whenWebpackIsInUse()
            throws Exception {

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir);

        NodeTasks nodeTasks = new NodeTasks(options);

        Assert.assertFalse(
                "TaskNotifyWebpackConfExistenceWhileUsingVite should not be in the list of node tasks when webpack is used.",
                isTaskAddedToBeExecuted(nodeTasks,
                        TaskNotifyWebpackConfExistenceWhileUsingVite.class));
    }

    @Test
    public void should_generateServiceWorkerWhenPwa() throws Exception {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir);

        Assert.assertEquals(
                new File(userDir, DEFAULT_FRONTEND_DIR).getAbsolutePath(),
                ((File) getFieldValue(options, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                new File(new File(userDir, TARGET), DEFAULT_GENERATED_DIR)
                        .getAbsolutePath(),
                ((File) getFieldValue(options, "generatedFolder"))
                        .getAbsolutePath());

        new NodeTasks(options).execute();
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
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir);

        Assert.assertEquals(
                new File(userDir, "my_custom_sources_folder").getAbsolutePath(),
                ((File) getFieldValue(options, "frontendDirectory"))
                        .getAbsolutePath());
        Assert.assertEquals(
                new File(userDir, "my/custom/generated/folder")
                        .getAbsolutePath(),
                ((File) getFieldValue(options, "generatedFolder"))
                        .getAbsolutePath());

        new NodeTasks(options).execute();
        Assert.assertTrue(
                new File(userDir, "my/custom/generated/folder/" + IMPORTS_NAME)
                        .exists());
    }

    @Test
    public void should_SetIsClientBootstrapMode_When_EnableClientSideBootstrapMode()
            throws ExecutionFailedException, IOException {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false)
                .withWebpack(new File(userDir, TARGET + "webapp"),
                        new File(userDir, TARGET + "classes"))
                .enableImportsUpdate(true).runNpmInstall(false)
                .withEmbeddableWebComponents(false).useV14Bootstrap(false)
                .withJarFrontendResourcesFolder(new File(
                        new File(userDir,
                                DEFAULT_PROJECT_FRONTEND_GENERATED_DIR),
                        FrontendUtils.JAR_RESOURCES_FOLDER))
                .withFrontendGeneratedFolder(new File(userDir,
                        DEFAULT_PROJECT_FRONTEND_GENERATED_DIR))
                .setJavaResourceFolder(propertiesDir);
        new NodeTasks(options).execute();
        String webpackGeneratedContent = Files
                .lines(new File(userDir, WEBPACK_GENERATED).toPath())
                .collect(Collectors.joining("\n"));
        Assert.assertTrue(
                "useClientSideIndexFileForBootstrapping should be true",
                webpackGeneratedContent.contains(
                        "const useClientSideIndexFileForBootstrapping = true;"));
    }

    @Test
    public void should_GenerateTsConfigAndTsDefinitions_When_Vaadin14BootstrapMode()
            throws ExecutionFailedException {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).useV14Bootstrap(true)
                .enableImportsUpdate(true).runNpmInstall(false)
                .withEmbeddableWebComponents(false).useV14Bootstrap(false)
                .setJavaResourceFolder(propertiesDir);
        new NodeTasks(options).execute();

        Assert.assertTrue(new File(userDir, "tsconfig.json").exists());
        Assert.assertTrue(new File(userDir, "types.d.ts").exists());
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

    private boolean isTaskAddedToBeExecuted(NodeTasks nodeTasks,
            Class<?> taskClass) throws Exception {
        List<FallibleCommand> commands = (List<FallibleCommand>) getFieldValue(
                nodeTasks, "commands");
        return commands.stream().anyMatch(fallibleCommand -> fallibleCommand
                .getClass().equals(taskClass));
    }
}
