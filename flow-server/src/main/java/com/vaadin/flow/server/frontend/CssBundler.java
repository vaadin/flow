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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * Utility methods to handle application theme CSS content.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class CssBundler {

    private static final String WHITE_SPACE = "\\s*";

    private static final String STRING = "(.*?)";

    private static final String CSS_STRING = "('" + STRING + "'|\"" + STRING
            + "\"|" + STRING + ")";
    private static final String QUOTED_CSS_STRING = "('" + STRING + "'|\""
            + STRING + "\")";

    private static final String URL = "url" + WHITE_SPACE + "\\(" + WHITE_SPACE
            + CSS_STRING + WHITE_SPACE + "\\)";

    private static final String URL_OR_STRING = "(" + URL + "|"
            + QUOTED_CSS_STRING + ")";

    private static final String LAYER = "(layer|layer" + WHITE_SPACE
            + "\\((.*?)\\))";
    private static final String MEDIA_QUERY = "(\\s+[a-zA-Z].*)?";
    private static final String MAYBE_LAYER_OR_MEDIA_QUERY = "(" + LAYER + "|"
            + MEDIA_QUERY + ")";

    /**
     * This regexp is based on
     * https://developer.mozilla.org/en-US/docs/Web/CSS/@import#formal_syntax
     * which states
     *
     * <pre>
     * @import = @import [ &lt;url> | &lt;string> ] [ layer | layer( &lt;layer-name> ) ]?
     *         &lt;import-conditions> ;
     * </pre>
     *
     */
    private static Pattern importPattern = Pattern
            .compile("@import" + WHITE_SPACE + URL_OR_STRING
                    + MAYBE_LAYER_OR_MEDIA_QUERY + WHITE_SPACE + ";");

    private static Pattern urlPattern = Pattern.compile(URL);

    /**
     * Recurse over CSS import and inlines all ot them into a single CSS block.
     * <p>
     *
     * Unresolvable imports are put on the top of the resulting code, because
     * {@code @import} statements must come before any other CSS instruction,
     * otherwise the import is ignored by the browser.
     * <p>
     *
     * Along with import resolution and code inline, URLs
     * ({@code url('image.png')} referencing theme resources or assets are
     * rewritten to be correctly served by Vaadin at runtime.
     *
     * @param themeFolder
     *            location of theme folder on the filesystem.
     * @param cssFile
     *            the CSS file to process.
     * @param themeJson
     *            the theme configuration, usually stored in
     *            {@literal theme.json} file.
     * @return the processed stylesheet content, with inlined imports and
     *         rewritten URLs.
     * @throws IOException
     *             if filesystem resources can not be read.
     */
    public static String inlineImports(File themeFolder, File cssFile,
            JsonNode themeJson) throws IOException {
        return inlineImports(themeFolder, cssFile,
                getThemeAssetsAliases(themeJson));
    }

    private static String inlineImports(File themeFolder, File cssFile,
            Set<String> assetAliases) throws IOException {
        String content = java.nio.file.Files.readString(cssFile.toPath(),
                StandardCharsets.UTF_8);

        Matcher urlMatcher = urlPattern.matcher(content);
        content = urlMatcher.replaceAll(result -> {
            String url = getNonNullGroup(result, 2, 3, 4);
            if (url == null || url.trim().endsWith(".css")) {
                // These are handled below
                return Matcher.quoteReplacement(urlMatcher.group());
            }
            File potentialFile = new File(cssFile.getParentFile(), url.trim());
            if (potentialFile.exists()) {
                // e.g. background-image: url("./foo/bar.png") should become
                // url("VAADIN/themes/<theme-name>/foo/bar.png IF the file is
                // inside the theme folder
                // Otherwise, "./foo/bar.png" can also refer to a file in
                // src/main/resources/META-INF/resources/foo/bar.png and then we
                // don't need a rewrite...

                // Also the imports are relative to the folder containing the
                // CSS file we are processing, not always relative to the theme
                // folder
                String relativePath = themeFolder.getParentFile().toPath()
                        .relativize(potentialFile.toPath()).toString()
                        .replaceAll("\\\\", "/");
                return Matcher.quoteReplacement(
                        "url('VAADIN/themes/" + relativePath + "')");
            } else if (isPotentialThemeAsset(themeFolder, assetAliases,
                    potentialFile)) {
                // a reference to a theme asset, e.g with a theme.json config
                // { "assets": {
                // "@some/pkg": { "svgs/regular/**": "my/icons" }
                // } }
                // background-image: url("./my/icons/bar.png") should become
                // url("VAADIN/themes/<theme-name>/my/icons/bar.png
                String relativePath = themeFolder.getParentFile().toPath()
                        .relativize(potentialFile.toPath()).toString()
                        .replaceAll("\\\\", "/");
                return Matcher.quoteReplacement(
                        "url('VAADIN/themes/" + relativePath + "')");
            }

            return Matcher.quoteReplacement(urlMatcher.group());
        });
        List<String> unhandledImports = new ArrayList<>();
        Matcher importMatcher = importPattern.matcher(content);
        content = importMatcher.replaceAll(result -> {
            // Oh the horror
            // Group 3,4,5 are url() urls with different quotes
            // Group 7,8 are strings with different quotes
            // Group 9 is layer info
            String layerOrMediaQueryInfo = result.group(9);
            if (layerOrMediaQueryInfo != null
                    && !layerOrMediaQueryInfo.isBlank()) {
                unhandledImports.add(result.group());
                return "";
            }
            String url = getNonNullGroup(result, 3, 4, 5, 7, 8);
            String sanitizedUrl = sanitizeUrl(url);
            if (sanitizedUrl != null && sanitizedUrl.endsWith(".css")) {
                File potentialFile = new File(cssFile.getParentFile(),
                        sanitizedUrl);
                if (potentialFile.exists()) {
                    try {
                        return Matcher.quoteReplacement(inlineImports(
                                themeFolder, potentialFile, assetAliases));
                    } catch (IOException e) {
                        getLogger().warn(
                                "Unable to inline import: " + result.group());
                    }
                }
            }

            unhandledImports.add(result.group());
            return "";
        });

        // Prepend unhandled @import statements at the top, as they would be
        // ignored by the browser if they appear after regular CSS rules
        if (!unhandledImports.isEmpty()) {
            content = String.join("\n", unhandledImports)
                    + (content.isEmpty() ? "" : "\n" + content);
        }
        return content;
    }

    // A theme asset must:
    // - be relative to the theme folder
    // - have a match in theme.json 'assets'
    private static boolean isPotentialThemeAsset(File themeFolder,
            Set<String> assetAliases, File potentialFile) {
        boolean potentialAsset = false;
        if (!assetAliases.isEmpty()) {
            Path themeFolderPath = themeFolder.toPath().normalize();
            try {
                Path normalized = themeFolderPath
                        .resolve(potentialFile.toPath()).normalize();
                if (normalized.startsWith(themeFolderPath)) {
                    // path is relative to theme folder, check if it matches an
                    // asset
                    String relativePath = themeFolderPath.relativize(normalized)
                            .toString().replaceAll("\\\\", "/");
                    potentialAsset = assetAliases.stream()
                            .anyMatch(relativePath::startsWith);
                    if (potentialAsset) {
                        getLogger().debug(
                                "Considering '{}' a potential asset of theme '{}'",
                                relativePath, themeFolder.getName());
                    }
                }
            } catch (IllegalArgumentException e) {
                getLogger().debug(
                        "Unresolvable path '{}'. Not considered as a asset of theme '{}'",
                        potentialFile, themeFolder.getName());
                return false;
            }
        }
        return potentialAsset;
    }

    private static Set<String> getThemeAssetsAliases(JsonNode themeJson) {
        JsonNode assets = themeJson != null && themeJson.has("assets")
                ? themeJson.get("assets")
                : null;
        Set<String> aliases = new HashSet<>();
        if (assets != null) {
            for (String nmpPackage : JacksonUtils.getKeys(assets)) {
                JsonNode packageAliases = assets.get(nmpPackage);
                for (String path : JacksonUtils.getKeys(packageAliases)) {
                    aliases.add(packageAliases.get(path).textValue() + "/");
                }
            }
        }
        return aliases;
    }

    private static String getNonNullGroup(MatchResult result, int... groupId) {
        for (int i : groupId) {
            String res = result.group(i);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.trim().split("\\?")[0];
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CssBundler.class);
    }

}
