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

import static com.vaadin.flow.server.frontend.FrontendUtils.APP_THEMES_FOLDER_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;

@NotThreadSafe
public class TaskUpdateThemeImportTest {

    private static final String CUSTOM_THEME_NAME = "custom-theme";
    private static final String CUSTOM_VARIANT_NAME = "custom-variant";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdateThemeImport taskUpdateThemeImport;
    private Class<? extends AbstractTheme> dummyThemeClass;
    private File projectRoot;
    private File npmFolder;

    @Before
    public void setUp() throws IOException {
        projectRoot = temporaryFolder.getRoot();
        npmFolder = temporaryFolder.getRoot();

        dummyThemeClass = Mockito.mock(AbstractTheme.class).getClass();
    }

    @Test
    public void taskExecuted_customThemeWithNonExistingThemeFolder_throwsException() {

        File faultyFrontendDirectory = new File(projectRoot,
                DEFAULT_FRONTEND_DIR);

        ThemeDefinition customTheme = new ThemeDefinition(dummyThemeClass,
                 CUSTOM_VARIANT_NAME, CUSTOM_THEME_NAME
        );

        taskUpdateThemeImport = new TaskUpdateThemeImport(npmFolder,
                customTheme, faultyFrontendDirectory);

        ExecutionFailedException e = Assert.assertThrows(
                                        ExecutionFailedException.class,
                                        () -> taskUpdateThemeImport.execute());

        Assert.assertTrue(e.getMessage().contains(
                String.format("Discovered @Theme(\"%s\") annotation but",
                        CUSTOM_THEME_NAME)));
    }

    @Test
    public void taskExecuted_customThemeWithCorrectThemeFolder_ensuresThemeGeneratedJsCreatedSuccessfully()
                                                            throws Exception {
        File correctFrontendDirectory = new File(projectRoot,
                DEFAULT_FRONTEND_DIR);
        File themesDir = new File(correctFrontendDirectory,
                APP_THEMES_FOLDER_NAME);
        File aCustomThemeDir = new File(themesDir, CUSTOM_THEME_NAME);

        boolean customThemeDirCreatedSuccessfully = aCustomThemeDir.mkdirs();

        Assert.assertTrue(String.format(
                "%s directory should be created at '%s%s/%s' but failed.",
                CUSTOM_THEME_NAME, DEFAULT_FRONTEND_DIR, APP_THEMES_FOLDER_NAME,
                CUSTOM_THEME_NAME),
                customThemeDirCreatedSuccessfully);

        ThemeDefinition customTheme = new ThemeDefinition(dummyThemeClass,
                CUSTOM_VARIANT_NAME, CUSTOM_THEME_NAME
        );

        taskUpdateThemeImport = new TaskUpdateThemeImport(npmFolder,
                customTheme, correctFrontendDirectory);

        File nodeModules = new File(npmFolder, FrontendUtils.NODE_MODULES);
        File flowFrontend = new File(nodeModules,
                FrontendUtils.FLOW_NPM_PACKAGE_NAME);
        File themeImportFile = new File(new File(flowFrontend, "theme"),
                "theme-generated.js");

        Assert.assertFalse("\"theme-generated.js\" should not exist before" +
                " executing TaskUpdateThemeImport.", themeImportFile.exists());

        taskUpdateThemeImport.execute();

        Assert.assertTrue("\"theme-generated.js\" should be created as the " +
                        "result of executing TaskUpdateThemeImport.",
                themeImportFile.exists());
    }

}
