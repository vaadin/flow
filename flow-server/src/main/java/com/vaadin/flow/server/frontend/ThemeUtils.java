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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.theme.Theme;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;

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
     * @param context
     *            the vaadin context
     * @return custom theme name or empty optional if no theme is used
     */
    public static Optional<String> getThemeName(VaadinContext context) {
        return getThemeAnnotation(context).map(Theme::value);
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

    public static Optional<JsonObject> getThemeJson(String themeName,
            AbstractConfiguration config) {
        String content = null;
        try {
            if (config.isProductionMode()) {
                URL themeJsonUrl = getThemeResourceFromBundle(themeName,
                        "theme.json");
                if (themeJsonUrl == null) {
                    themeJsonUrl = getThemeResourceFromJar(themeName,
                            "theme.json");
                }
                if (themeJsonUrl != null) {
                    content = IOUtils.toString(themeJsonUrl,
                            StandardCharsets.UTF_8);
                }
            } else {
                File frontendFolder = FrontendUtils
                        .getProjectFrontendDir(config);
                File themeFolder = getThemeFolder(frontendFolder, themeName);
                File themeJsonFile = new File(themeFolder, "theme.json");
                if (themeJsonFile.exists()) {
                    content = FileUtils.readFileToString(themeJsonFile,
                            StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            getLogger().error(
                    "Unable to read theme.json file of theme=" + themeName, e);
        }

        return content != null ? Optional.of(Json.parse(content))
                : Optional.empty();
    }

    public static URL getThemeResourceFromJar(String themeName,
            String fileName) {
        return ThemeUtils.class.getClassLoader()
                .getResource(Constants.RESOURCES_THEME_JAR_DEFAULT + themeName
                        + "/" + fileName);
    }

    public static URL getThemeResourceFromBundle(String themeName,
            String fileName) {
        return ThemeUtils.class.getClassLoader()
                .getResource(VAADIN_WEBAPP_RESOURCES + "VAADIN/static/"
                        + Constants.APPLICATION_THEME_ROOT + "/" + themeName
                        + "/" + fileName);
    }

    public static Optional<JsonObject> getThemeJson(String themeName,
            Options options) {
        File themeJsonFile;
        File frontendFolder = options.getFrontendDirectory();
        File themeFolder = getThemeFolder(frontendFolder, themeName);
        themeJsonFile = new File(themeFolder, "theme.json");

        if (themeJsonFile.exists()) {
            String content;
            try {
                content = FileUtils.readFileToString(themeJsonFile,
                        StandardCharsets.UTF_8);
                return Optional.of(Json.parse(content));
            } catch (IOException e) {
                getLogger().error(
                        "Unable to read theme json from " + themeJsonFile, e);
            }
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

    /**
     * Gets the active themes in parent to child order, starting from the
     * application theme.
     *
     * @param context
     *            the vaadin context
     * @param config
     *            the application configuration
     * @return a list of active themes, in parent to child order
     */
    public static List<String> getActiveThemes(VaadinContext context) {
        Optional<String> applicationTheme = getThemeName(context);
        if (!applicationTheme.isPresent()) {
            return Collections.emptyList();
        }

        List<String> themes = new ArrayList<>();

        ApplicationConfiguration config = ApplicationConfiguration.get(context);
        findActiveThemes(applicationTheme.get(), themes, config);
        Collections.reverse(themes);
        return themes;
    }

    /**
     * Finds the folder for the given theme.
     * <p>
     * Assumes the folder exists and throws an exception if it does not.
     *
     * @param frontendFolder
     *            the project frontend folder
     * @param themeName
     *            the theme name
     * @return the folder for the theme, containing styles.css
     * @throws IllegalArgumentException
     *             if the theme folder was not found
     */
    public static File getThemeFolder(File frontendFolder, String themeName)
            throws IllegalArgumentException {

        File packagedThemesFolder = new File(
                FrontendUtils.getJarResourcesFolder(frontendFolder),
                Constants.APPLICATION_THEME_ROOT);
        File projectThemesFolder = new File(frontendFolder,
                Constants.APPLICATION_THEME_ROOT);

        File themeInProject = new File(projectThemesFolder, themeName);
        if (themeInProject.exists()) {
            return themeInProject;
        }

        File themeFromJar = new File(packagedThemesFolder, themeName);
        if (themeFromJar.exists()) {
            return themeFromJar;
        }

        throw new IllegalArgumentException("The theme folder for the '"
                + themeName + "' theme was not found. It should be either in "
                + themeInProject + " or in " + themeFromJar);
    }

    private static void findActiveThemes(String themeName, List<String> themes,
            AbstractConfiguration config) {
        themes.add(themeName);

        Optional<JsonObject> themeJson = getThemeJson(themeName, config);
        if (themeJson.isPresent()) {
            Optional<String> parentTheme = getParentThemeName(themeJson.get());
            if (parentTheme.isPresent()) {
                findActiveThemes(parentTheme.get(), themes, config);
            }
        }
    }

    public static String getThemeFilePath(String themeName, String fileName) {
        return Constants.VAADIN_MAPPING + Constants.APPLICATION_THEME_ROOT + "/"
                + themeName + "/" + fileName;
    }
}
