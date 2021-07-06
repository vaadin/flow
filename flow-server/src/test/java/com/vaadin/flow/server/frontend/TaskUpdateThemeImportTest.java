/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.Constants.APPLICATION_THEME_ROOT;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.TaskUpdateThemeImport.APPLICATION_META_INF_RESOURCES;
import static com.vaadin.flow.server.frontend.TaskUpdateThemeImport.APPLICATION_STATIC_RESOURCES;

@NotThreadSafe
public class TaskUpdateThemeImportTest {

    private static final String CUSTOM_THEME_NAME = "custom-theme";
    private static final String CUSTOM_VARIANT_NAME = "custom-variant";
    private static final String CUSTOM_THEME_PATH = String.join("/",
            APPLICATION_THEME_ROOT, CUSTOM_THEME_NAME);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File projectRoot;
    private File npmFolder;
    private File frontendDirectory;
    private File themeImportFile;
    private Class<? extends AbstractTheme> dummyThemeClass;
    private ThemeDefinition customTheme;
    private TaskUpdateThemeImport taskUpdateThemeImport;

    @Before
    public void setUp() throws IOException {
        projectRoot = temporaryFolder.getRoot();
        npmFolder = temporaryFolder.getRoot();
        frontendDirectory = new File(projectRoot, DEFAULT_FRONTEND_DIR);

        File generated = new File(frontendDirectory, "generated");
        themeImportFile = new File(generated, "theme.js");

        dummyThemeClass = Mockito.mock(AbstractTheme.class).getClass();
        customTheme = new ThemeDefinition(dummyThemeClass, CUSTOM_VARIANT_NAME,
                CUSTOM_THEME_NAME);
        taskUpdateThemeImport = new TaskUpdateThemeImport(npmFolder,
                customTheme, frontendDirectory);
    }

    @Test
    public void taskExecuted_customThemeWithNonExistingThemeFolder_throwsException() {

        File faultyFrontendDirectory = new File(projectRoot,
                DEFAULT_FRONTEND_DIR);

        TaskUpdateThemeImport taskUpdateThemeImportWithNonExistentThemeFolder =
                new TaskUpdateThemeImport(npmFolder, customTheme,
                        faultyFrontendDirectory);

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class,
                taskUpdateThemeImportWithNonExistentThemeFolder::execute);

        Assert.assertTrue(e.getMessage().contains(String.format(
                "Discovered @Theme annotation with theme name '%s', "
                        + "but could not find the theme directory in the "
                        + "project or available as a jar dependency.",
                CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeWithThemeFolderInFrontend_ensuresThemeGeneratedJsCreatedSuccessfully()
            throws Exception {

        File themesDir = new File(frontendDirectory, APPLICATION_THEME_ROOT);
        File aCustomThemeDir = new File(themesDir, CUSTOM_THEME_NAME);

        boolean customThemeDirCreatedSuccessfully = aCustomThemeDir.mkdirs();

        Assert.assertTrue(String.format(
                "%s directory should be created at '%s%s/%s' but failed.",
                CUSTOM_THEME_NAME, DEFAULT_FRONTEND_DIR, APPLICATION_THEME_ROOT,
                CUSTOM_THEME_NAME), customThemeDirCreatedSuccessfully);

        Assert.assertFalse(
                "\"theme.js\" should not exist before"
                        + " executing TaskUpdateThemeImport.",
                themeImportFile.exists());

        taskUpdateThemeImport.execute();

        Assert.assertTrue(
                "\"theme.js\" should be created as the "
                        + "result of executing TaskUpdateThemeImport.",
                themeImportFile.exists());
    }

    @Test
    public void taskExecuted_customThemeWithThemeFolderInMetaInf_ensuresThemeGeneratedJsCreatedSuccessfully()
            throws Exception {

        File correctMetaInfResourcesDirectory = new File(projectRoot,
                APPLICATION_META_INF_RESOURCES);
        File themesDir = new File(correctMetaInfResourcesDirectory,
                APPLICATION_THEME_ROOT);
        File aCustomThemeDir = new File(themesDir, CUSTOM_THEME_NAME);

        boolean customThemeDirCreatedSuccessfully = aCustomThemeDir.mkdirs();

        Assert.assertTrue(String.format(
                "%s directory should be created at '%s/%s/%s' but failed.",
                CUSTOM_THEME_NAME, APPLICATION_META_INF_RESOURCES,
                APPLICATION_THEME_ROOT, CUSTOM_THEME_NAME),
                customThemeDirCreatedSuccessfully);

        Assert.assertFalse(
                "\"theme.js\" should not exist before"
                        + " executing TaskUpdateThemeImport.",
                themeImportFile.exists());

        taskUpdateThemeImport.execute();

        Assert.assertTrue(
                "\"theme.js\" should be created as the "
                        + "result of executing TaskUpdateThemeImport.",
                themeImportFile.exists());
    }

    @Test
    public void taskExecuted_customThemeWithThemeFolderInStatic_ensuresThemeGeneratedJsCreatedSuccessfully()
            throws Exception {

        File correctStaticResourcesDirectory = new File(projectRoot,
                APPLICATION_STATIC_RESOURCES);
        File themesDir = new File(correctStaticResourcesDirectory,
                APPLICATION_THEME_ROOT);
        File aCustomThemeDir = new File(themesDir, CUSTOM_THEME_NAME);

        boolean customThemeDirCreatedSuccessfully = aCustomThemeDir.mkdirs();

        Assert.assertTrue(String.format(
                "%s directory should be created at '%s/%s/%s' but failed.",
                CUSTOM_THEME_NAME, APPLICATION_STATIC_RESOURCES,
                APPLICATION_THEME_ROOT, CUSTOM_THEME_NAME),
                customThemeDirCreatedSuccessfully);

        Assert.assertFalse(
                "\"theme.js\" should not exist before"
                        + " executing TaskUpdateThemeImport.",
                themeImportFile.exists());

        taskUpdateThemeImport.execute();

        Assert.assertTrue(
                "\"theme.js\" should be created as the "
                        + "result of executing TaskUpdateThemeImport.",
                themeImportFile.exists());
    }

    @Test
    public void taskExecuted_customThemeWithThemeFolderInClasspath_ensuresThemeGeneratedJsCreatedSuccessfully()
            throws Exception {

        File generatedDir = new File(projectRoot, DEFAULT_GENERATED_DIR);
        File themesDir = new File(generatedDir, APPLICATION_THEME_ROOT);
        File aCustomThemeDir = new File(themesDir, CUSTOM_THEME_NAME);

        boolean customThemeDirCreatedSuccessfully = aCustomThemeDir.mkdirs();

        Assert.assertTrue(String.format(
                "%s directory should be created at '%s%s/%s' but failed.",
                CUSTOM_THEME_NAME, DEFAULT_GENERATED_DIR,
                APPLICATION_THEME_ROOT, CUSTOM_THEME_NAME),
                customThemeDirCreatedSuccessfully);

        Assert.assertFalse(
                "\"theme.js\" should not exist before"
                        + " executing TaskUpdateThemeImport.",
                themeImportFile.exists());

        taskUpdateThemeImport.execute();

        Assert.assertTrue(
                "\"theme.js\" should be created as the "
                        + "result of executing TaskUpdateThemeImport.",
                themeImportFile.exists());
    }

    @Test
    public void runTaskWithTheme_createsThemeFile_afterRunWithoutTheme_removesThemeFile()
        throws Exception {

        File themesDir = new File(frontendDirectory, APPLICATION_THEME_ROOT);
        File aCustomThemeDir = new File(themesDir, CUSTOM_THEME_NAME);

        boolean customThemeDirCreatedSuccessfully = aCustomThemeDir.mkdirs();

        Assert.assertTrue(String
            .format("%s directory should be created at '%s%s/%s' but failed.",
                CUSTOM_THEME_NAME, DEFAULT_FRONTEND_DIR, APPLICATION_THEME_ROOT,
                CUSTOM_THEME_NAME), customThemeDirCreatedSuccessfully);

        Assert.assertFalse("\"theme.js\" should not exist before"
            + " executing TaskUpdateThemeImport.", themeImportFile.exists());

        taskUpdateThemeImport.execute();

        Assert.assertTrue("\"theme.js\" should be created as the "
                + "result of executing TaskUpdateThemeImport.",
            themeImportFile.exists());

        taskUpdateThemeImport = new TaskUpdateThemeImport(npmFolder, null,
            frontendDirectory);

        taskUpdateThemeImport.execute();

        Assert.assertFalse("\"theme.js\" should not exist before"
            + " executing TaskUpdateThemeImport.", themeImportFile.exists());
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInBothFrontendAndInClasspath_throwsException() {

        File themeDir = new File(frontendDirectory, CUSTOM_THEME_PATH);
        Assert.assertTrue(themeDir.mkdirs());

        String jsrThemePath = DEFAULT_GENERATED_DIR
                + CUSTOM_THEME_PATH;
        File classPathThemeDir = new File(projectRoot, jsrThemePath);
        Assert.assertTrue(classPathThemeDir.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage()
                .contains(String.format(
                        "Theme '%s' should not exist inside a "
                                + "jar and in the project at the same time.",
                        CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInBothMetaInfResourcesAndInClasspath_throwsException() {

        String metaInfResources = String.join("/",
                APPLICATION_META_INF_RESOURCES, CUSTOM_THEME_PATH);
        File themeDir = new File(projectRoot, metaInfResources);
        Assert.assertTrue(themeDir.mkdirs());

        String jsrThemePath = DEFAULT_GENERATED_DIR
                + CUSTOM_THEME_PATH;
        File classPathThemeDir = new File(projectRoot, jsrThemePath);
        Assert.assertTrue(classPathThemeDir.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage()
                .contains(String.format(
                        "Theme '%s' should not exist inside a "
                                + "jar and in the project at the same time.",
                        CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInBothStaticResourcesAndInClasspath_throwsException() {

        String staticResources = String.join("/", APPLICATION_STATIC_RESOURCES,
                CUSTOM_THEME_PATH);
        File themeDir = new File(projectRoot, staticResources);
        Assert.assertTrue(themeDir.mkdirs());

        String classPathThemePath = DEFAULT_GENERATED_DIR
                + CUSTOM_THEME_PATH;
        File classPathThemeDir = new File(projectRoot, classPathThemePath);
        Assert.assertTrue(classPathThemeDir.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage()
                .contains(String.format(
                        "Theme '%s' should not exist inside a "
                                + "jar and in the project at the same time.",
                        CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInBothStaticAndMetaInfResources_throwsException() {

        String staticResources = String.join("/", APPLICATION_STATIC_RESOURCES,
                CUSTOM_THEME_PATH);
        File themeDirInStatic = new File(projectRoot, staticResources);
        Assert.assertTrue(themeDirInStatic.mkdirs());

        String metaInfResources = String.join("/",
                APPLICATION_META_INF_RESOURCES, CUSTOM_THEME_PATH);
        File themeDirInMetaInf = new File(projectRoot, metaInfResources);
        Assert.assertTrue(themeDirInMetaInf.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage().contains(String.format(
                "Discovered Theme folder for theme '%s' "
                        + "in more than one place in the project. Please "
                        + "make sure there is only one theme folder with name '%s' "
                        + "exists in the your project. ",
                CUSTOM_THEME_NAME, CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInBothFrontendAndMetaInfResources_throwsException() {

        File themeDir = new File(frontendDirectory, CUSTOM_THEME_PATH);
        Assert.assertTrue(themeDir.mkdirs());

        String metaInfResources = String.join("/",
                APPLICATION_META_INF_RESOURCES, CUSTOM_THEME_PATH);
        File themeDirInMetaInf = new File(projectRoot, metaInfResources);
        Assert.assertTrue(themeDirInMetaInf.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage().contains(String.format(
            "Discovered Theme folder for theme '%s' "
                    + "in more than one place in the project. Please "
                    + "make sure there is only one theme folder with name '%s' "
                    + "exists in the your project. ",
            CUSTOM_THEME_NAME, CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInBothFrontendAndStaticResources_throwsException() {

        File themeDir = new File(frontendDirectory, CUSTOM_THEME_PATH);
        Assert.assertTrue(themeDir.mkdirs());

        String staticResources = String.join("/", APPLICATION_STATIC_RESOURCES,
                CUSTOM_THEME_PATH);
        File themeDirInStatic = new File(projectRoot, staticResources);
        Assert.assertTrue(themeDirInStatic.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage().contains(String.format(
            "Discovered Theme folder for theme '%s' "
                    + "in more than one place in the project. Please "
                    + "make sure there is only one theme folder with name '%s' "
                    + "exists in the your project. ",
            CUSTOM_THEME_NAME, CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInFrontendAndStaticAndMetaInfResources_throwsException() {

        File themeDir = new File(frontendDirectory, CUSTOM_THEME_PATH);
        Assert.assertTrue(themeDir.mkdirs());

        String staticResources = String.join("/", APPLICATION_STATIC_RESOURCES,
                CUSTOM_THEME_PATH);
        File themeDirInStatic = new File(projectRoot, staticResources);
        Assert.assertTrue(themeDirInStatic.mkdirs());

        String metaInfResources = String.join("/",
                APPLICATION_META_INF_RESOURCES, CUSTOM_THEME_PATH);
        File themeDirInMetaInf = new File(projectRoot, metaInfResources);
        Assert.assertTrue(themeDirInMetaInf.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage().contains(String.format(
            "Discovered Theme folder for theme '%s' "
                    + "in more than one place in the project. Please "
                    + "make sure there is only one theme folder with name '%s' "
                    + "exists in the your project. ",
            CUSTOM_THEME_NAME, CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeFolderExistsInClassPathAndStaticAndMetaInfResources_throwsException() {

        String classPathThemePath = DEFAULT_GENERATED_DIR
                + CUSTOM_THEME_PATH;
        File classPathThemeDir = new File(projectRoot, classPathThemePath);
        Assert.assertTrue(classPathThemeDir.mkdirs());

        String staticResources = String.join("/", APPLICATION_STATIC_RESOURCES,
                CUSTOM_THEME_PATH);
        File themeDirInStatic = new File(projectRoot, staticResources);
        Assert.assertTrue(themeDirInStatic.mkdirs());

        String metaInfResources = String.join("/",
                APPLICATION_META_INF_RESOURCES, CUSTOM_THEME_PATH);
        File themeDirInMetaInf = new File(projectRoot, metaInfResources);
        Assert.assertTrue(themeDirInMetaInf.mkdirs());

        ExecutionFailedException e = Assert.assertThrows(
                ExecutionFailedException.class, taskUpdateThemeImport::execute);

        Assert.assertTrue(e.getMessage() + " did not match expected", e.getMessage()
                .contains(String.format(
                        "Theme '%s' should not exist inside a "
                                + "jar and in the project at the same time.",
                        CUSTOM_THEME_NAME)));
    }
}
