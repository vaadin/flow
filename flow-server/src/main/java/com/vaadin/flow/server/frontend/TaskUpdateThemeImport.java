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
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.Constants.APPLICATION_THEME_ROOT;
import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;
import static com.vaadin.flow.server.frontend.FrontendUtils.THEME_IMPORTS_D_TS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.THEME_IMPORTS_NAME;

/**
 * Task generating the theme definition file 'theme.js' for importing
 * application theme into the generated frontend directory.
 *
 * Default directory is ' ./frontend/generated'
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public class TaskUpdateThemeImport implements FallibleCommand {

    public static final String APPLICATION_META_INF_RESOURCES = "src/main/resources/META-INF/resources";
    public static final String APPLICATION_STATIC_RESOURCES = "src/main/resources/static";
    private static final String EXPORT_MODULES_DEF = "export declare const applyTheme: (target: Node) => void;";

    private final File themeImportFile;
    private final File themeImportFileDefinition;
    private final ThemeDefinition theme;
    private final File frontendDirectory;
    private final File npmFolder;

    TaskUpdateThemeImport(File npmFolder, ThemeDefinition theme,
            File frontendDirectory) {
        this.theme = theme;
        this.frontendDirectory = frontendDirectory;
        this.npmFolder = npmFolder;
        File frontendGeneratedFolder = new File(frontendDirectory, GENERATED);
        themeImportFile = new File(frontendGeneratedFolder, THEME_IMPORTS_NAME);
        themeImportFileDefinition = new File(frontendGeneratedFolder,
                THEME_IMPORTS_D_TS_NAME);
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (theme == null || theme.getName().isEmpty()) {
            if (themeImportFile.exists()) {
                themeImportFile.delete();
                themeImportFileDefinition.delete();
            }
            return;
        }

        verifyThemeDirectoryExistence();

        if (!themeImportFile.getParentFile().mkdirs()) {
            LoggerFactory.getLogger(getClass()).debug(
                    "Didn't create folders as they probably already exist. "
                            + "If there is a problem check access rights for folder {}",
                    themeImportFile.getParentFile().getAbsolutePath());
        }

        try {
            FileUtils.write(themeImportFile, String.format(
                    "import {applyTheme as _applyTheme} from './theme-%s.generated.js';%n"
                            + "export const applyTheme = _applyTheme;%n",
                    theme.getName()), StandardCharsets.UTF_8);
            FileUtils.write(themeImportFileDefinition, EXPORT_MODULES_DEF,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExecutionFailedException(
                    "Unable to write theme import file", e);
        }
    }

    private void verifyThemeDirectoryExistence()
            throws ExecutionFailedException {

        String themeName = theme.getName();
        String themePath = String.join("/", APPLICATION_THEME_ROOT, themeName);

        List<String> appThemePossiblePaths = getAppThemePossiblePaths(
                themePath);
        List<File> existingAppThemeDirectories = appThemePossiblePaths.stream()
                .map(path -> new File(npmFolder, path)).filter(File::exists)
                .collect(Collectors.toList());

        if (existingAppThemeDirectories.isEmpty()) {
            String errorMessage = "Discovered @Theme annotation with theme "
                    + "name '%s', but could not find the theme directory "
                    + "in the project or available as a jar dependency. "
                    + "Check if you forgot to create the folder under '%s' "
                    + "or have mistyped the theme or folder name for '%s'.";
            throw new ExecutionFailedException(
                    String.format(errorMessage, themeName,
                            new File(frontendDirectory, APPLICATION_THEME_ROOT)
                                    .getPath(),
                            themeName));
        }
        if (existingAppThemeDirectories.size() >= 2) {

            boolean themeFoundInJar = existingAppThemeDirectories.stream()
                    .map(File::getPath)
                    .anyMatch(path -> path.contains(
                            Paths.get(FrontendUtils.FLOW_NPM_PACKAGE_NAME)
                                    .toString()));

            if (themeFoundInJar) {
                String errorMessage = "Theme '%s' should not exist inside a "
                        + "jar and in the project at the same time.%n"
                        + "Extending another theme is possible by adding "
                        + "{ \"parent\": \"your-parent-theme\" } entry to the "
                        + "'theme.json' file inside your theme folder.";
                throw new ExecutionFailedException(
                        String.format(errorMessage, themeName));
            } else {
                String errorMessage = "Discovered Theme folder for theme '%s' "
                        + "in more than one place in the project. Please "
                        + "make sure there is only one theme folder with name "
                        + "'%s' exists in the your project. "
                        + "The recommended place to put the theme folder "
                        + "inside the project is '%s'";
                throw new ExecutionFailedException(String.format(errorMessage,
                        themeName, themeName,
                        new File(frontendDirectory, APPLICATION_THEME_ROOT)
                                .getPath()));
            }
        }
    }

    private List<String> getAppThemePossiblePaths(String themePath) {
        String frontendTheme = String.join("/", npmFolder.toPath()
                .relativize(frontendDirectory.toPath()).toString(), themePath);

        String themePathInMetaInfResources = String.join("/",
                APPLICATION_META_INF_RESOURCES, themePath);

        String themePathInStaticResources = String.join("/",
                APPLICATION_STATIC_RESOURCES, themePath);

        String themePathInClassPathResources = String.join("",
                FrontendUtils.NODE_MODULES, FrontendUtils.FLOW_NPM_PACKAGE_NAME,
                themePath);

        return Arrays.asList(frontendTheme, themePathInMetaInfResources,
                themePathInStaticResources, themePathInClassPathResources);
    }
}
