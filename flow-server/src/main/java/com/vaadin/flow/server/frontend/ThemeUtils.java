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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Helpers related to theme handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ThemeUtils {

    private static final Pattern THEME_GENERATED_FILE_PATTERN = Pattern
            .compile("theme-([\\s\\S]+?)\\.generated\\.js");

    private ThemeUtils() {
        // Static helpers only
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ThemeUtils.class);
    }

    /**
     * Gets the custom theme name if the custom theme is used in the project.
     * <p>
     * Should be only used in the development mode.
     *
     * @param projectFolder
     *            the project root folder
     * @return custom theme name or empty optional if no theme is used
     * @throws IOException
     *             if I/O exceptions occur while trying to extract the theme
     *             name.
     */
    public static Optional<String> getThemeName(File projectFolder)
            throws IOException {
        File themeJs = new File(projectFolder, FrontendUtils.FRONTEND
                + FrontendUtils.GENERATED + FrontendUtils.THEME_IMPORTS_NAME);

        if (!themeJs.exists()) {
            return Optional.empty();
        }

        String themeJsContent = FileUtils.readFileToString(themeJs,
                StandardCharsets.UTF_8);
        Matcher matcher = THEME_GENERATED_FILE_PATTERN.matcher(themeJsContent);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        } else {
            throw new IllegalStateException(
                    "Couldn't extract theme name from theme imports file 'theme.js'");
        }
    }

    /**
     * Gets the theme annotation for the project.
     *
     * @param context
     *            the Vaadin context
     * @return the theme annotation or an empty optional
     */
    public static Optional<Theme> getThemeAnnotation(VaadinContext context) {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        Class<? extends AppShellConfigurator> shell = registry.getShell();
        if (shell == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(shell.getAnnotation(Theme.class));
    }

    public static Optional<JsonObject> getThemeJsonForTheme(
            File themesParentFolder, String themeName) throws IOException {
        File themeJsonFile = Path.of(themesParentFolder.getAbsolutePath(),
                Constants.APPLICATION_THEME_ROOT, themeName, "theme.json")
                .toFile();
        if (themeJsonFile.exists()) {
            String content = FileUtils.readFileToString(themeJsonFile,
                    StandardCharsets.UTF_8);
            content = content.replaceAll("\\r\\n", "\n");
            return Optional.of(Json.parse(content));
        }
        return Optional.empty();
    }

    public static Optional<JsonObject> getThemeJsonForTheme(Options options,
            ThemeDefinition themeDefinition) throws IOException {
        if (themeDefinition != null) {
            String themeName = themeDefinition.getName();
            return getThemeJsonForTheme(options.getFrontendDirectory(),
                    themeName);
        }
        return Optional.empty();
    }

    public static Optional<String> getParentThemeName(JsonObject themeJson) {
        if (themeJson != null) {
            if (themeJson.hasKey("parent")) {
                String parentThemeName = themeJson.getString("parent");
                return Optional.of(parentThemeName);
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getParentThemeName(File themesParentFolder,
            String themeName) throws IOException {
        Optional<JsonObject> themeJson = getThemeJsonForTheme(
                themesParentFolder, themeName);
        if (themeJson.isPresent()) {
            return getParentThemeName(themeJson.get());
        }
        return Optional.empty();
    }

}
