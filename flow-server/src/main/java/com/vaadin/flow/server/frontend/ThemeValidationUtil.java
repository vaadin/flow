package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.ThemeDefinition;

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
    private static final String FRONTEND_HASHES_KEY = "frontendHashes";

    public static boolean themeConfigurationChanged(Options options,
            JsonNode statsJson,
            FrontendDependenciesScanner frontendDependencies) {
        Map<String, JsonNode> themeJsonContents = new HashMap<>();

        if (options.getJarFiles() != null) {
            options.getJarFiles().stream().filter(File::exists)
                    .filter(file -> !file.isDirectory())
                    .forEach(jarFile -> getPackagedThemeJsonContents(jarFile,
                            themeJsonContents));
        }

        Optional<String> maybeThemeName = Optional
                .ofNullable(frontendDependencies.getThemeDefinition())
                .map(ThemeDefinition::getName).filter(name -> !name.isBlank());
        Optional<JsonNode> projectThemeJson = maybeThemeName
                .flatMap(themeName -> ThemeUtils.getThemeJson(themeName,
                        options.getFrontendDirectory()));
        String projectThemeName = maybeThemeName.orElse(null);

        JsonNode statsThemeJson = statsJson.get("themeJsonContents");
        if (statsThemeJson == null && (!themeJsonContents.isEmpty()
                || projectThemeJson.isPresent())) {
            getLogger().info(
                    "Found newly added theme configurations in 'theme.json'.");
            return true;
        }

        if (projectThemeJson.isPresent()) {
            String key;
            if (statsThemeJson.has(projectThemeName)) {
                key = projectThemeName;
            } else if (!options.isProductionMode()
                    && statsThemeJson.has(Constants.DEV_BUNDLE_NAME)) {
                key = Constants.DEV_BUNDLE_NAME;
            } else if (options.isProductionMode()
                    && statsThemeJson.has(Constants.PROD_BUNDLE_NAME)) {
                key = Constants.PROD_BUNDLE_NAME;
            } else {
                getLogger().info(
                        "Found newly added configuration for project theme '{}' in 'theme.json'.",
                        projectThemeName);
                return true;
            }

            collectThemeJsonContentsInFrontend(options, themeJsonContents, key,
                    projectThemeJson.get());
        }

        for (Map.Entry<String, JsonNode> themeContent : themeJsonContents
                .entrySet()) {
            if (hasNewAssetsOrImports(statsThemeJson, themeContent)) {
                getLogger().info(
                        "Found new configuration for theme '{}' in 'theme.json'.",
                        themeContent.getKey());
                return true;
            } else if (statsThemeJson.has(themeContent.getKey())) {
                List<String> missedKeys = new ArrayList<>();
                JsonNode content = JacksonUtils.readTree(
                        statsThemeJson.get(themeContent.getKey()).textValue());
                if (!objectIncludesEntry(content, themeContent.getValue(),
                        missedKeys)) {
                    getLogger().info(
                            "Custom theme '{}' has imports/assets removed or added in 'theme.json' not represented in the bundle ('stats.json').",
                            themeContent.getKey());
                    logMissedEntries(missedKeys);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if theme has legacy Shadow DOM stylesheets in
     * {@literal <theme>/components} folder and if their content has changed.
     *
     * @param options
     *            Flow plugin options
     * @param statsJson
     *            the stats.json for the application bundle.
     * @param frontendDependencies
     *            frontend dependencies scanner to lookup for theme settings
     * @return {@literal true} if the theme has legacy Shadow DOM stylesheets,
     *         and they are not included on the application bundle, otherwise
     *         {@literal false}.
     */
    public static boolean themeShadowDOMStylesheetsChanged(Options options,
            JsonNode statsJson,
            FrontendDependenciesScanner frontendDependencies) {
        File frontendDirectory = options.getFrontendDirectory();
        // Scan the theme hierarchy and collect all <theme>/components folders
        Set<Path> themeComponentsDirs = Optional
                .ofNullable(frontendDependencies.getThemeDefinition())
                .map(ThemeDefinition::getName).filter(name -> !name.isBlank())
                .map(themeName -> {
                    Map<String, JsonNode> themeJsonContents = new HashMap<>();
                    ThemeUtils.getThemeJson(themeName, frontendDirectory)
                            .ifPresent(
                                    themeJson -> collectThemeJsonContentsInFrontend(
                                            options, themeJsonContents,
                                            themeName, themeJson));
                    return themeJsonContents.keySet().stream()
                            .map(name -> ThemeUtils
                                    .getThemeFolder(frontendDirectory, name))
                            .map(dir -> new File(dir, "components"))
                            .filter(File::exists).map(File::toPath)
                            .collect(Collectors.toSet());
                }).orElse(null);
        if (themeComponentsDirs != null) {
            Map<String, String> hashesWithNoComponentCssMatches = new HashMap<>();
            if (statsJson.has(FRONTEND_HASHES_KEY)) {
                JsonNode json = statsJson.get(FRONTEND_HASHES_KEY);
                JacksonUtils.getKeys(json).stream()
                        // Only considers bundled resources located in
                        // '[generated/jar-resources/]themes/<themeName>/components'
                        .filter(path -> themeComponentsDirs.stream()
                                .anyMatch(dir -> frontendDirectory.toPath()
                                        .resolve(path).startsWith(dir)))
                        .forEach(key -> hashesWithNoComponentCssMatches.put(key,
                                json.get(key).textValue()));
            }

            List<String> themeComponentsCssFiles = new ArrayList<>();
            for (Path dir : themeComponentsDirs) {
                FileUtils.listFiles(dir.toFile(), new String[] { "css" }, true)
                        .stream()
                        .filter(themeFile -> isFrontendResourceChangedOrMissingInBundle(
                                hashesWithNoComponentCssMatches,
                                frontendDirectory, themeFile))
                        .map(f -> frontendDirectory.toPath()
                                .relativize(f.toPath()).toString()
                                .replaceAll("\\\\", "/"))
                        .collect(Collectors
                                .toCollection(() -> themeComponentsCssFiles));
            }
            if (!themeComponentsCssFiles.isEmpty()) {
                BundleValidationUtil.logChangedFiles(themeComponentsCssFiles,
                        "Detected new or changed theme components CSS files");
            }
            if (!hashesWithNoComponentCssMatches.isEmpty()) {
                BundleValidationUtil.logChangedFiles(
                        new ArrayList<>(
                                hashesWithNoComponentCssMatches.keySet()),
                        "Detected removed theme components CSS files");
            }
            return !(themeComponentsCssFiles.isEmpty()
                    && hashesWithNoComponentCssMatches.isEmpty());
        }
        return false;
    }

    private static boolean hasNewAssetsOrImports(JsonNode contentsInStats,
            Map.Entry<String, JsonNode> themeContent) {
        JsonNode json = themeContent.getValue();
        int keys = JacksonUtils.getKeys(json).size();
        boolean moreThanOneKey = keys > 1;
        boolean noParentEntry = keys == 1 && !json.has("parent");
        // do not re-bundle immediately if theme.json is empty or has only
        // parent reference
        return !contentsInStats.has(themeContent.getKey())
                && (moreThanOneKey || noParentEntry);
    }

    private static void collectThemeJsonContentsInFrontend(Options options,
            Map<String, JsonNode> themeJsonContents, String themeName,
            JsonNode themeJson) {
        Optional<String> parentThemeInFrontend = ThemeUtils
                .getParentThemeName(themeJson);
        if (parentThemeInFrontend.isPresent()) {
            String parentThemeName = parentThemeInFrontend.get();
            Optional<JsonNode> parentThemeJson = ThemeUtils.getThemeJson(
                    parentThemeName, options.getFrontendDirectory());
            parentThemeJson.ifPresent(
                    jsonObject -> collectThemeJsonContentsInFrontend(options,
                            themeJsonContents, parentThemeName, jsonObject));
        }

        themeJsonContents.put(themeName, themeJson);
    }

    static boolean objectIncludesEntry(JsonNode jsonFromBundle,
            JsonNode projectJson, Collection<String> missedKeys) {
        JsonNodeType bundleJsonNodeType = jsonFromBundle.getNodeType();
        JsonNodeType projectJsonNodeTypeType = projectJson.getNodeType();
        assert bundleJsonNodeType.equals(projectJsonNodeTypeType);

        if (bundleJsonNodeType == JsonNodeType.NULL) {
            return true;
        } else if (bundleJsonNodeType == JsonNodeType.BOOLEAN) {
            return JacksonUtils.booleanEqual(jsonFromBundle, projectJson);
        } else if (bundleJsonNodeType == JsonNodeType.NUMBER) {
            return JacksonUtils.numbersEqual(jsonFromBundle, projectJson);
        } else if (bundleJsonNodeType == JsonNodeType.STRING) {
            return JacksonUtils.stringEqual(jsonFromBundle, projectJson);
        } else if (bundleJsonNodeType == JsonNodeType.ARRAY) {
            ArrayNode jsonArrayFromBundle = (ArrayNode) jsonFromBundle;
            ArrayNode jsonArrayFromProject = (ArrayNode) projectJson;
            return compareArrays(missedKeys, jsonArrayFromBundle,
                    jsonArrayFromProject);
        } else if (bundleJsonNodeType == JsonNodeType.OBJECT) {
            return compareObjects(missedKeys, jsonFromBundle, projectJson);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported JsonNodeType: " + bundleJsonNodeType);
        }
    }

    private static void logMissedEntries(List<String> missedKeys) {
        Collections.reverse(missedKeys);
        BundleValidationUtil.logChangedFiles(missedKeys,
                "Detected missed or added entries:");
    }

    private static void getPackagedThemeJsonContents(File jarFileToLookup,
            Map<String, JsonNode> packagedThemeHashes) {
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
                JsonNode jsonContent = null;
                jsonContent = JacksonUtils.readTree(content);
                packagedThemeHashes.put(themeName, jsonContent);
            }
        }
    }

    private static boolean compareObjects(Collection<String> missedKeys,
            JsonNode jsonObjectFromBundle, JsonNode projectJsonNode) {
        boolean allEntriesFound = true;

        for (String projectEntryKey : JacksonUtils.getKeys(projectJsonNode)) {
            JsonNode projectEntry = projectJsonNode.get(projectEntryKey);
            // ignore parent theme, because having a parent theme doesn't
            // need a new bundle per se
            if (projectEntry.getNodeType() == JsonNodeType.STRING
                    && "parent".equals(projectEntryKey)) {
                continue;
            }
            boolean entryFound = false;
            for (String bundleEntryKey : JacksonUtils
                    .getKeys(jsonObjectFromBundle)) {
                JsonNode bundleEntry = jsonObjectFromBundle.get(bundleEntryKey);
                if (bundleEntry.getNodeType() == projectEntry.getNodeType()
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
            ArrayNode jsonArrayFromBundle, ArrayNode jsonArrayFromProject) {

        boolean allEntriesFound = checkMissedKeys(missedKeys,
                jsonArrayFromBundle, jsonArrayFromProject);

        // making sure that from the other jsonArray we also check and compare
        // the entries
        // to make sure both arrays have the exact same entries
        // without this it could happen that:
        // jsonArrayFromBundle = [1,2,3]
        // jsonArrayFromProject = [1,2]
        // and the check would pass
        if (jsonArrayFromBundle.size() != jsonArrayFromProject.size()) {
            allEntriesFound = allEntriesFound && checkMissedKeys(missedKeys,
                    jsonArrayFromProject, jsonArrayFromBundle);
        }

        return allEntriesFound;
    }

    private static boolean checkMissedKeys(Collection<String> missedKeys,
            ArrayNode arrayIterating, ArrayNode arrayComparing) {
        boolean allEntriesFound = true;

        for (int arrayComparingIndex = 0; arrayComparingIndex < arrayComparing
                .size(); arrayComparingIndex++) {
            JsonNode arrayComparingEntry = arrayComparing
                    .get(arrayComparingIndex);
            boolean entryFound = false;
            for (int arrayIteratingIndex = 0; arrayIteratingIndex < arrayIterating
                    .size(); arrayIteratingIndex++) {
                JsonNode arrayIteratingEntry = arrayIterating
                        .get(arrayIteratingIndex);
                if (arrayIteratingEntry.getNodeType() == arrayComparingEntry
                        .getNodeType()
                        && objectIncludesEntry(arrayIteratingEntry,
                                arrayComparingEntry, missedKeys)) {
                    entryFound = true;
                    break;
                }
            }
            if (!entryFound) {
                missedKeys.add(arrayComparingEntry.toString());
            }
            allEntriesFound = allEntriesFound && entryFound;
        }
        return allEntriesFound;
    }

    private static boolean isFrontendResourceChangedOrMissingInBundle(
            Map<String, String> bundledHashes, File frontendFolder,
            File frontendResource) {
        String relativePath = frontendFolder.toPath()
                .relativize(frontendResource.toPath()).toString()
                .replaceAll("\\\\", "/");
        boolean presentInBundle = bundledHashes.containsKey(relativePath);
        if (presentInBundle) {
            final String contentHash;
            try {
                contentHash = BundleValidationUtil.calculateHash(
                        FileUtils.readFileToString(frontendResource,
                                StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return !bundledHashes.remove(relativePath).equals(contentHash);
        }
        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ThemeValidationUtil.class);
    }
}
