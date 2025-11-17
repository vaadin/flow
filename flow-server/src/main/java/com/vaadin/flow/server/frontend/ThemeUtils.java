/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.shared.ApplicationConstants.VAADIN_STATIC_FILES_PATH;

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
        ApplicationConfiguration config = ApplicationConfiguration.get(context);
        if (config.isProductionMode()) {
            return getThemeAnnotation(context).map(Theme::value);
        } else {
            File themeJs;
            File frontendFolder = config.getFrontendFolder();
            if (frontendFolder.isAbsolute()) {
                themeJs = Paths
                        .get(frontendFolder.getPath(), FrontendUtils.GENERATED,
                                FrontendUtils.THEME_IMPORTS_NAME)
                        .toFile();
            } else {
                themeJs = Paths.get(config.getProjectFolder().getPath(),
                        frontendFolder.getPath(), FrontendUtils.GENERATED,
                        FrontendUtils.THEME_IMPORTS_NAME).toFile();
            }
            if (!themeJs.exists()) {
                return Optional.empty();
            }

            try {
                String themeJsContent = java.nio.file.Files.readString(
                        themeJs.toPath(), StandardCharsets.UTF_8);
                Matcher matcher = THEME_GENERATED_FILE_PATTERN
                        .matcher(themeJsContent);
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                } else {
                    throw new IllegalStateException(
                            "Couldn't extract theme name from theme imports file 'theme.js'");
                }
            } catch (IOException e) {
                getLogger().error(
                        "Couldn't read theme generated file to get the theme name",
                        e);
                return Optional.empty();
            }
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

    public static Optional<JsonNode> getThemeJson(String themeName,
            AbstractConfiguration config) {
        String content = null;
        try {
            if (config.isProductionMode()) {
                URL themeJsonUrl = ThemeUtils
                        .getThemeResourceFromPrecompiledProductionBundle(
                                Paths.get(Constants.APPLICATION_THEME_ROOT,
                                        themeName, "theme.json").toString());
                if (themeJsonUrl != null) {
                    try (var in = themeJsonUrl.openStream()) {
                        content = new String(in.readAllBytes(),
                                StandardCharsets.UTF_8);
                    }
                }
            } else {
                File frontendFolder = FrontendUtils
                        .getProjectFrontendDir(config);
                File themeFolder = getThemeFolder(frontendFolder, themeName);
                File themeJsonFile = new File(themeFolder, "theme.json");
                if (themeJsonFile.exists()) {
                    content = java.nio.file.Files
                            .readString(themeJsonFile.toPath(),
                                    StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            getLogger().error(
                    "Unable to read theme.json file of theme=" + themeName, e);
        }

        return content != null ? Optional.of(JacksonUtils.readTree(content))
                : Optional.empty();
    }

    /**
     * Gets the URL of the theme resource located in the pre-compiled production
     * bundle JAR or in the external packaged theme JAR.
     *
     * @param themeAssetPath
     *            theme resource path relative to 'themes' folder, e.g.
     *            'my-theme/styles.css'
     * @return URL to theme resource if the resource was found,
     *         <code>null</code> otherwise
     */
    public static URL getThemeResourceFromPrecompiledProductionBundle(
            String themeAssetPath) {
        // lookup in the prod bundle, where themes are copied from project's
        URL resourceUrl = ThemeUtils.class.getClassLoader().getResource(
                FrontendUtils.getUnixPath(Paths.get(VAADIN_WEBAPP_RESOURCES,
                        VAADIN_STATIC_FILES_PATH, themeAssetPath)));
        if (resourceUrl == null) {
            // lookup in the JARs for packaged themes
            resourceUrl = ThemeUtils.class.getClassLoader().getResource(
                    Constants.RESOURCES_JAR_DEFAULT + themeAssetPath);
        }
        return resourceUrl;
    }

    public static Optional<JsonNode> getThemeJson(String themeName,
            File frontendFolder) {
        File themeFolder = getThemeFolder(frontendFolder, themeName);
        File themeJsonFile = new File(themeFolder, "theme.json");

        if (themeJsonFile.exists()) {
            String content;
            try {
                content = java.nio.file.Files.readString(
                        themeJsonFile.toPath(), StandardCharsets.UTF_8);
                return Optional.of(JacksonUtils.readTree(content));
            } catch (IOException e) {
                getLogger().error(
                        "Unable to read theme json from " + themeJsonFile, e);
            }
        }
        return Optional.empty();

    }

    public static Optional<String> getParentThemeName(JsonNode themeJson) {
        if (themeJson != null) {
            if (themeJson.has("parent")) {
                String parentThemeName = themeJson.get("parent").textValue();
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

        Optional<JsonNode> themeJson = getThemeJson(themeName, config);
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
