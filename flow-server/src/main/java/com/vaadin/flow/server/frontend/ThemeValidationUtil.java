package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Theme handling methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 */
public class ThemeValidationUtil {

    private static final Pattern THEME_PATH_PATTERN = Pattern
            .compile("themes\\/([\\s\\S]+?)\\/theme.json");

    public static boolean themeConfigurationChanged(Options options,
            JsonObject statsJson,
            FrontendDependenciesScanner frontendDependencies) {
        Map<String, JsonObject> themeJsonContents = new HashMap<>();

        if (options.getJarFiles() != null) {
            options.getJarFiles().stream().filter(File::exists)
                    .filter(file -> !file.isDirectory())
                    .forEach(jarFile -> getPackagedThemeJsonContents(jarFile,
                            themeJsonContents));
        }

        Optional<String> maybeThemeName = Optional
                .ofNullable(frontendDependencies.getThemeDefinition())
                .map(def -> def.getName()).filter(name -> !name.isBlank());
        Optional<JsonObject> projectThemeJson = maybeThemeName
                .flatMap(themeName -> ThemeUtils.getThemeJson(
                        options.getFrontendDirectory(), themeName));
        String projectThemeName = maybeThemeName.orElse(null);

        JsonObject statsThemeJson = statsJson.getObject("themeJsonContents");
        if (statsThemeJson == null && (!themeJsonContents.isEmpty()
                || projectThemeJson.isPresent())) {
            getLogger().info(
                    "Found newly added theme configurations in 'theme.json'.");
            return true;
        }

        if (projectThemeJson.isPresent()) {
            String key;
            if (statsThemeJson.hasKey(projectThemeName)) {
                key = projectThemeName;
            } else if (statsThemeJson.hasKey(Constants.DEV_BUNDLE_NAME)) {
                key = Constants.DEV_BUNDLE_NAME;
            } else {
                getLogger().info(
                        "Found newly added configuration for project theme '{}' in 'theme.json'.",
                        projectThemeName);
                return true;
            }

            collectThemeJsonContentsInFrontend(options, themeJsonContents, key,
                    projectThemeJson.get());
        }

        for (Map.Entry<String, JsonObject> themeContent : themeJsonContents
                .entrySet()) {
            if (hasNewAssetsOrImports(statsThemeJson, themeContent)) {
                getLogger().info(
                        "Found new configuration for theme '{}' in 'theme.json'.",
                        themeContent.getKey());
                return true;
            } else if (statsThemeJson.hasKey(themeContent.getKey())) {
                List<String> missedKeys = new ArrayList<>();
                JsonObject content = Json
                        .parse(statsThemeJson.getString(themeContent.getKey()));
                if (!objectIncludesEntry(content, themeContent.getValue(),
                        missedKeys)) {
                    getLogger().info(
                            "Custom theme '{}' has imports/assets in 'theme.json' not present in the bundle",
                            themeContent.getKey());
                    logMissedEntries(missedKeys);
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasNewAssetsOrImports(JsonObject contentsInStats,
            Map.Entry<String, JsonObject> themeContent) {
        JsonObject json = themeContent.getValue();
        boolean moreThanOneKey = json.keys().length > 1;
        boolean noParentEntry = json.keys().length == 1
                && !json.hasKey("parent");
        // do not re-bundle immediately if theme.json is empty or has only
        // parent reference
        return !contentsInStats.hasKey(themeContent.getKey())
                && (moreThanOneKey || noParentEntry);
    }

    private static void collectThemeJsonContentsInFrontend(Options options,
            Map<String, JsonObject> themeJsonContents, String themeName,
            JsonObject themeJson) {
        Optional<String> parentThemeInFrontend = ThemeUtils
                .getParentThemeName(themeJson);
        if (parentThemeInFrontend.isPresent()) {
            String parentThemeName = parentThemeInFrontend.get();
            Optional<JsonObject> parentThemeJson = ThemeUtils.getThemeJson(
                    options.getFrontendDirectory(), parentThemeName);
            if (parentThemeJson.isPresent()) {
                collectThemeJsonContentsInFrontend(options, themeJsonContents,
                        parentThemeName, parentThemeJson.get());
            }
        }

        themeJsonContents.put(themeName, themeJson);
    }

    private static boolean objectIncludesEntry(JsonValue jsonFromBundle,
            JsonValue projectJson, Collection<String> missedKeys) {
        JsonType bundleJsonType = jsonFromBundle.getType();
        JsonType projectJsonObjectTypeType = projectJson.getType();
        assert bundleJsonType.equals(projectJsonObjectTypeType);

        if (bundleJsonType == JsonType.NULL) {
            return true;
        } else if (bundleJsonType == JsonType.BOOLEAN) {
            return JsonUtils.booleanEqual(jsonFromBundle, projectJson);
        } else if (bundleJsonType == JsonType.NUMBER) {
            return JsonUtils.numbersEqual(jsonFromBundle, projectJson);
        } else if (bundleJsonType == JsonType.STRING) {
            return JsonUtils.stringEqual(jsonFromBundle, projectJson);
        } else if (bundleJsonType == JsonType.ARRAY) {
            JsonArray jsonArrayFromBundle = (JsonArray) jsonFromBundle;
            JsonArray projectJsonArray = (JsonArray) projectJson;
            return compareArrays(missedKeys, jsonArrayFromBundle,
                    projectJsonArray);
        } else if (bundleJsonType == JsonType.OBJECT) {
            JsonObject jsonObjectFromBundle = (JsonObject) jsonFromBundle;
            JsonObject projectJsonObject = (JsonObject) projectJson;
            return compareObjects(missedKeys, jsonObjectFromBundle,
                    projectJsonObject);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported JsonType: " + bundleJsonType);
        }
    }

    private static void logMissedEntries(List<String> missedKeys) {
        Collections.reverse(missedKeys);
        BundleValidationUtil.logChangedFiles(missedKeys,
                "Detected missed entries:");
    }

    private static void getPackagedThemeJsonContents(File jarFileToLookup,
            Map<String, JsonObject> packagedThemeHashes) {
        JarContentsManager jarContentsManager = new JarContentsManager();
        if (jarContentsManager.containsPath(jarFileToLookup,
                Constants.RESOURCES_THEME_JAR_DEFAULT)) {
            List<String> themeJsons = jarContentsManager.findFiles(
                    jarFileToLookup, Constants.RESOURCES_THEME_JAR_DEFAULT,
                    "theme.json");
            for (String themeJson : themeJsons) {
                byte[] byteContent = jarContentsManager
                        .getFileContents(jarFileToLookup, themeJson);
                String content = IOUtils.toString(byteContent, "UTF-8");
                content = content.replaceAll("\\r\\n", "\n");

                Matcher matcher = THEME_PATH_PATTERN.matcher(themeJson);
                if (!matcher.find()) {
                    throw new IllegalStateException(
                            "Packaged theme folders structure is incorrect, should have META-INF/resources/themes/[theme-name]/");
                }
                String themeName = matcher.group(1);
                JsonObject jsonContent = Json.parse(content);
                packagedThemeHashes.put(themeName, jsonContent);
            }
        }
    }

    private static boolean compareObjects(Collection<String> missedKeys,
            JsonObject jsonObjectFromBundle, JsonObject projectJsonObject) {
        boolean allEntriesFound = true;

        for (String projectEntryKey : projectJsonObject.keys()) {
            JsonValue projectEntry = projectJsonObject.get(projectEntryKey);
            // ignore parent theme, because having a parent theme doesn't
            // need a new bundle per se
            if (projectEntry.getType() == JsonType.STRING
                    && "parent".equals(projectEntryKey)) {
                continue;
            }
            boolean entryFound = false;
            for (String bundleEntryKey : jsonObjectFromBundle.keys()) {
                JsonValue bundleEntry = jsonObjectFromBundle
                        .get(bundleEntryKey);
                if (bundleEntry.getType() == projectEntry.getType()
                        && objectIncludesEntry(bundleEntry, projectEntry,
                                missedKeys)) {
                    entryFound = true;
                    break;
                }
            }
            if (!entryFound) {
                missedKeys.add(projectEntryKey);
            }
            allEntriesFound = allEntriesFound && entryFound;
        }
        return allEntriesFound;
    }

    private static boolean compareArrays(Collection<String> missedKeys,
            JsonArray jsonArrayFromBundle, JsonArray projectJsonArray) {
        boolean allEntriesFound = true;

        for (int projectArrayIndex = 0; projectArrayIndex < projectJsonArray
                .length(); projectArrayIndex++) {
            JsonValue projectArrayEntry = projectJsonArray
                    .get(projectArrayIndex);
            boolean entryFound = false;
            for (int bundleArrayIndex = 0; bundleArrayIndex < jsonArrayFromBundle
                    .length(); bundleArrayIndex++) {
                JsonValue bundleArrayEntry = jsonArrayFromBundle
                        .get(bundleArrayIndex);
                if (bundleArrayEntry.getType() == projectArrayEntry.getType()
                        && objectIncludesEntry(bundleArrayEntry,
                                projectArrayEntry, missedKeys)) {
                    entryFound = true;
                    break;
                }
            }
            if (!entryFound) {
                missedKeys.add(projectArrayEntry.toJson());
            }
            allEntriesFound = allEntriesFound && entryFound;
        }
        return allEntriesFound;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ThemeValidationUtil.class);
    }
}
