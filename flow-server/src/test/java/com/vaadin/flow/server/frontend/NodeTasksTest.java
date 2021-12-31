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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

public class NodeTasksTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String USER_DIR = "user.dir";

    private static String globalUserDirValue;
    private static String globalFrontendDirValue;
    private static String globalGeneratedDirValue;

    private String userDir;
    private VaadinContext context;

    @Before
    public void setup() {
        userDir = temporaryFolder.getRoot().getAbsolutePath();
        System.setProperty(USER_DIR, userDir);
        System.clearProperty(PARAM_FRONTEND_DIR);
        System.clearProperty(PARAM_GENERATED_DIR);
        context = new MockVaadinContext();
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
    public void should_UseDefaultFolders() throws Exception {
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false);

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
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false);

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

        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).enableImportsUpdate(true)
                .runNpmInstall(false).withEmbeddableWebComponents(false);

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

    @Test
    public void should_SetIsClientBootstrapMode_When_EnableClientSideBootstrapMode()
            throws ExecutionFailedException, IOException {
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false)
                .withWebpack(new File(userDir, TARGET + "webapp"),
                        new File(userDir, TARGET + "classes"), WEBPACK_CONFIG,
                        WEBPACK_GENERATED)
                .enableImportsUpdate(true).runNpmInstall(false)
                .withEmbeddableWebComponents(false).useV14Bootstrap(false)
                .withFlowResourcesFolder(
                        new File(userDir, TARGET + "flow-frontend"))
                .withFusionClientAPIFolder(new File(userDir,
                        DEFAULT_PROJECT_FRONTEND_GENERATED_DIR));
        builder.build().execute();
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
        Lookup mockedLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(
                new DefaultClassFinder(this.getClass().getClassLoader()))
                .when(mockedLookup).lookup(ClassFinder.class);
        Builder builder = new Builder(mockedLookup, new File(userDir), TARGET)
                .enablePackagesUpdate(false).useV14Bootstrap(true)
                .enableImportsUpdate(true).runNpmInstall(false)
                .withEmbeddableWebComponents(false).useV14Bootstrap(false);
        builder.build().execute();

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
}
