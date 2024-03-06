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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
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
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.installer.Platform.ALPINE_RELEASE_FILE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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

        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder());

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

        File propertiesDir = temporaryFolder.newFolder();
        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME),
                "com.vaadin.experimental.exampleFeatureFlag=true\n",
                StandardCharsets.UTF_8);

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        ClassFinder finder = NodeUpdateTestUtil.getClassFinder(classes);
        Mockito.doReturn(finder).when(mockedLookup).lookup(ClassFinder.class);

        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .setJavaResourceFolder(propertiesDir)
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder());

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
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder());

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
    public void should_generateServiceWorkerWhenPwa() throws Exception {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder());

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
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder());

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

    private File getJarFrontendResourcesFolder() {
        return new File(userDir, "frontend/" + FrontendUtils.GENERATED
                + FrontendUtils.JAR_RESOURCES_FOLDER);
    }

    @Test
    public void should_GenerateTsConfigAndTsDefinitions_When_Vaadin14BootstrapMode()
            throws ExecutionFailedException {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false)
                .useV14Bootstrap(false).withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder());
        new NodeTasks(options).execute();

        Assert.assertTrue(new File(userDir, "tsconfig.json").exists());
        Assert.assertTrue(new File(userDir, "types.d.ts").exists());
    }

    @Test
    public void should_failWithMessage_When_Vaadin14BootstrapModeAndViteAreUsed() {
        Lookup mockedLookup = mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Options options = new Options(mockedLookup, new File(userDir), TARGET)
                .useV14Bootstrap(true);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                () -> new NodeTasks(options).execute());
        MatcherAssert.assertThat(exception.getMessage(),
                CoreMatchers.containsString(
                        "Vite build tool is not supported when 'useDeprecatedV14Bootstrapping' is used"));
    }

    @Test
    public void should_failWithExecutionFailedException_when_customWebpackConfigFileExist()
            throws IOException {
        try (MockedStatic<Paths> paths = Mockito.mockStatic(Paths.class)) {
            File webpackConfigFile = new File(userDir, WEBPACK_CONFIG);
            Path webpackConfigFilePath = webpackConfigFile.toPath();
            paths.when(() -> Paths.get(webpackConfigFile.getPath()))
                    .thenReturn(webpackConfigFilePath);
            Path targetPath = new File(userDir,
                    TARGET + "/" + DEFAULT_GENERATED_DIR).toPath();
            paths.when(() -> Paths.get(TARGET, DEFAULT_GENERATED_DIR))
                    .thenReturn(targetPath);
            paths.when(() -> Paths.get(new File(userDir).getPath(),
                    WEBPACK_CONFIG)).thenReturn(webpackConfigFilePath);
            paths.when(() -> Paths.get(ALPINE_RELEASE_FILE_PATH))
                    .thenReturn(Path.of(ALPINE_RELEASE_FILE_PATH));

            String content = "const merge = require('webpack-merge');\n"
                    + "const flowDefaults = require('./webpack.generated.js');\n"
                    + "\n"
                    + "module.exports = merge(flowDefaults, {\"some-config\": 1\n"
                    + "\n" + "});\n";
            Files.writeString(webpackConfigFilePath, content);

            Lookup mockedLookup = Mockito.mock(Lookup.class);
            Mockito.doReturn(
                    new DefaultClassFinder(this.getClass().getClassLoader()))
                    .when(mockedLookup).lookup(ClassFinder.class);
            Options options = new Options(mockedLookup, new File(userDir),
                    TARGET).enablePackagesUpdate(false)
                    .enableImportsUpdate(true).runNpmInstall(false)
                    .withEmbeddableWebComponents(false)
                    .withJarFrontendResourcesFolder(
                            getJarFrontendResourcesFolder());

            ExecutionFailedException exception = Assert.assertThrows(
                    ExecutionFailedException.class,
                    () -> new NodeTasks(options).execute());
            Assert.assertTrue(exception.getMessage().contains(
                    "Webpack related config file 'webpack.config.js' is detected in your"));
        }
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
