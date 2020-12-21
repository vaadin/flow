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
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.Constants.APPLICATION_META_INF_RESOURCES;
import static com.vaadin.flow.server.Constants.APPLICATION_STATIC_RESOURCES;
import static com.vaadin.flow.server.Constants.APPLICATION_THEME_ROOT;

/**
 * Task for generating the theme-generated.js file for importing application
 * theme.
 *
 * @since
 */
public class TaskUpdateThemeImport implements FallibleCommand {

    private File themeImportFile;
    private ThemeDefinition theme;
    private File frontendDirectory;

    TaskUpdateThemeImport(File npmFolder, ThemeDefinition theme,
                          File frontendDirectory) {
        File nodeModules = new File(npmFolder, FrontendUtils.NODE_MODULES);
        File flowFrontend = new File(nodeModules,
            FrontendUtils.FLOW_NPM_PACKAGE_NAME);
        this.themeImportFile = new File(new File(flowFrontend, "theme"),
            "theme-generated.js");
        this.theme = theme;
        this.frontendDirectory = frontendDirectory;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (theme == null || theme.getName().isEmpty()) {
            return;
        }

        verifyThemeDirectoryExistence(theme.getName(), frontendDirectory);

        if (!themeImportFile.getParentFile().mkdirs()) {
            LoggerFactory.getLogger(getClass()).debug(
                "Didn't create folders as they probably already exist. "
                    + "If there is a problem check access rights for folder {}",
                themeImportFile.getParentFile().getAbsolutePath());
        }

        try {
            FileUtils.write(themeImportFile, String.format(
                "import {applyTheme as _applyTheme} from 'themes/%s/%s.generated.js';%n"
                    + "export const applyTheme = _applyTheme;%n",
                theme.getName(), theme.getName()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExecutionFailedException(
                "Unable to write theme import file", e);
        }
    }

    private void verifyThemeDirectoryExistence(String themeName,
                                               File frontendDirectory)
            throws ExecutionFailedException {

        File mainThemesDir = new File (frontendDirectory, APPLICATION_THEME_ROOT);
        File mainCustomThemeDir = new File (mainThemesDir, themeName);

        Stream<File> otherFoldersToSearch = getOtherFoldersToSearchForAppTheme(
                themeName, frontendDirectory);

        if (!mainCustomThemeDir.exists()) {
            if (otherFoldersToSearch.noneMatch(File::exists)) {
                String errorMessage = "Discovered @Theme annotation with " +
                        "theme name '%s', but could not find the theme " +
                        "directory in the project or available as a jar " +
                        "dependency. Check if you forgot to create the " +
                        "folder under './%s/%s/' or have mistyped the theme " +
                        "or folder name for '%s'.";

                throw new ExecutionFailedException(String.format(errorMessage,
                        themeName, frontendDirectory.getName(),
                        APPLICATION_THEME_ROOT, themeName));
            }
        } else {
            if (otherFoldersToSearch.anyMatch(File::exists)) {
                String errorMessage = "Discovered Theme folder for theme " +
                        "'%s' in more than one place in the project. Please " +
                        "remove the duplicate(s) and try again. The " +
                        "recommended place to put the theme folder inside " +
                        "the project is './%s/%s/'";

                throw new ExecutionFailedException(String.format(errorMessage,
                        themeName, frontendDirectory.getName(),
                        APPLICATION_THEME_ROOT));
            }
        }
    }

    private Stream<File> getOtherFoldersToSearchForAppTheme(String themeName,
                                                    File frontendDirectory) {
        File projectRootDir = frontendDirectory.getParentFile();

        File metaInfDir = new File(projectRootDir,
                APPLICATION_META_INF_RESOURCES);
        File metaInfThemesDir = new File(metaInfDir, APPLICATION_THEME_ROOT);
        File metaInfCustomThemeDir = new File(metaInfThemesDir, themeName);

        File staticDir = new File(projectRootDir, APPLICATION_STATIC_RESOURCES);
        File staticThemesDir = new File(staticDir, APPLICATION_THEME_ROOT);
        File staticCustomThemeDir = new File(staticThemesDir, themeName);

        File classpathThemesDir = new File(
                themeImportFile.getParentFile().getParent(),
                APPLICATION_THEME_ROOT);
        File classpathCustomThemeDir = new File(classpathThemesDir, themeName);

        return Arrays.stream(new File[] { metaInfCustomThemeDir,
                staticCustomThemeDir, classpathCustomThemeDir });
    }
}
