/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    // Selects how url(...) references are rewritten when inlining @import
    // statements. The right choice depends on how the bundled CSS is later
    // delivered to the browser:
    //
    // THEMES — used for application themes; url() targets are rewritten to
    // absolute "VAADIN/themes/<theme>/..." paths because themes are
    // served from a known fixed location.
    //
    // STATIC_RESOURCES — used in dev mode for @StyleSheet files served from
    // public roots (META-INF/resources, webapp, ...). The dev-tools
    // live-reload client (vaadin-dev-tools.ts onUpdate) injects the
    // bundled content into an inline <style> tag and removes the
    // original <link>. An inline <style> has no URL of its own, so the
    // browser resolves any relative url() against the *page URL*, which
    // would point to the wrong folder. To stay correct we rewrite to
    // absolute paths rooted at the servlet context (e.g.
    // "/myapp/relurl-test/images/dot.svg").
    //
    // STATIC_RESOURCES_RELATIVE — used in prod by TaskProcessStylesheetCss.
    // The bundled CSS is written back in-place under META-INF/resources
    // and served via the original <link href>, so the browser still
    // fetches it from the entry file's URL. Relative url() therefore
    // resolves against the entry's folder and we just need to express
    // each url() relative to that folder. We can't use STATIC_RESOURCES
    // here because the deployment context path is not known at build
    // time.
    private enum BundleFor {
        THEMES, STATIC_RESOURCES, STATIC_RESOURCES_RELATIVE
    }

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
    private static final Pattern IMPORT_PATTERN = Pattern
            .compile("@import" + WHITE_SPACE + URL_OR_STRING
                    + MAYBE_LAYER_OR_MEDIA_QUERY + WHITE_SPACE + ";");
    private static final Pattern PROTOCOL_PATTER_FOR_URLS = Pattern
            .compile("(?i)^(https?|data|ftp|file):.*");
    private static final Pattern URL_PATTERN = Pattern.compile(URL);

    /**
     * Recurse over CSS import and inlines all ot them into a single CSS block
     * in themes folder under {@code src/main/frontend/themes/<theme-name>}.
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
    public static String inlineImportsForThemes(File themeFolder, File cssFile,
            JsonNode themeJson) throws IOException {
        return inlineImports(themeFolder, cssFile,
                getThemeAssetsAliases(themeJson), BundleFor.THEMES, null, null);
    }

    /**
     * Recurse over CSS import and inlines all of them into a single CSS block.
     * <p>
     *
     * Unresolvable imports are put on the top of the resulting code, because
     * {@code @import} statements must come before any other CSS instruction,
     * otherwise the import is ignored by the browser.
     * <p>
     *
     * This overload supports resolving imports from node_modules in addition to
     * relative paths.
     *
     * @param themeFolder
     *            location of theme folder on the filesystem. May be null if not
     *            processing theme files.
     * @param cssFile
     *            the CSS file to process.
     * @param themeJson
     *            the theme configuration, usually stored in
     *            {@literal theme.json} file. May be null.
     * @param nodeModulesFolder
     *            the node_modules folder for resolving npm package imports. May
     *            be null if node_modules resolution is not needed.
     * @return the processed stylesheet content, with inlined imports and
     *         rewritten URLs.
     * @throws IOException
     *             if filesystem resources can not be read.
     */
    public static String inlineImports(File themeFolder, File cssFile,
            JsonNode themeJson, File nodeModulesFolder) throws IOException {
        return inlineImports(themeFolder, cssFile,
                getThemeAssetsAliases(themeJson), null, "", nodeModulesFolder);
    }

    /**
     * Inlines imports for CSS files located under public static resources (e.g.
     * META-INF/resources).
     *
     * @param baseFolder
     *            base folder the imports and url() references are relative to
     * @param cssFile
     *            the CSS file to process
     * @param contextPath
     *            that url() are rewritten to and rebased onto
     * @return the processed stylesheet content with inlined imports only
     * @throws IOException
     *             if filesystem resources cannot be read
     */
    public static String inlineImportsForPublicResources(File baseFolder,
            File cssFile, String contextPath) throws IOException {
        return inlineImports(baseFolder, cssFile, new HashSet<>(),
                BundleFor.STATIC_RESOURCES, contextPath, null);
    }

    /**
     * Inlines imports for CSS files located under public static resources (e.g.
     * META-INF/resources) at build time, rewriting relative {@code url(...)}
     * references so they are expressed relative to the entry CSS file's folder.
     * <p>
     * Unlike {@link #inlineImportsForPublicResources(File, File, String)}, this
     * variant does not prepend a servlet context path. The resulting URLs are
     * purely relative, which is required when the CSS is processed at build
     * time and the deployment context path is not yet known.
     *
     * @param baseFolder
     *            folder of the entry CSS file; inlined {@code url(...)}
     *            references are rewritten to paths relative to this folder.
     * @param cssFile
     *            the CSS file to process.
     * @param nodeModulesFolder
     *            the node_modules folder for resolving npm package imports, or
     *            {@code null} if node_modules resolution is not needed.
     * @return the processed stylesheet content, with inlined imports and
     *         rewritten URLs.
     * @throws IOException
     *             if filesystem resources cannot be read.
     */
    public static String inlineImportsForStaticResourcesRelative(
            File baseFolder, File cssFile, File nodeModulesFolder)
            throws IOException {
        return inlineImports(baseFolder, cssFile, new HashSet<>(),
                BundleFor.STATIC_RESOURCES_RELATIVE, null, nodeModulesFolder);
    }

    /**
     * Internal implementation that can optionally skip URL rewriting.
     *
     * @param baseFolder
     *            base folder used for resolving relative paths, e.g. imports
     *            and url() references
     * @param cssFile
     *            the CSS file to process
     * @param assetAliases
     *            theme asset aliases (only used when rewriting URLs)
     * @param bundleFor
     *            defines a way how bundler resolves url(...), e.g. whether
     *            {@link com.vaadin.flow.theme.Theme} location is used
     *            ({@code src/main/frontend/themes/}) and whether to rewrite
     *            url(...) references to VAADIN/themes paths
     * @param contextPath
     *            that url() are rewritten to and rebased onto
     * @param nodeModulesFolder
     *            the node_modules folder for resolving npm package imports. May
     *            be null if node_modules resolution is not needed.
     */
    private static String inlineImports(File baseFolder, File cssFile,
            Set<String> assetAliases, BundleFor bundleFor, String contextPath,
            File nodeModulesFolder) throws IOException {
        List<String> cyclicImportWarnings = new ArrayList<>();
        String result = inlineImports(baseFolder, cssFile, assetAliases,
                bundleFor, contextPath, nodeModulesFolder, new HashSet<>(),
                cyclicImportWarnings);

        // Log single summary if cycles were detected
        if (!cyclicImportWarnings.isEmpty()) {
            getLogger().warn(
                    "Cyclic CSS @import statements detected and skipped:{}  - {}",
                    System.lineSeparator(),
                    String.join(System.lineSeparator() + "  - ",
                            cyclicImportWarnings));
        }
        return result;
    }

    /**
     * Internal implementation with cycle detection support.
     *
     * @param baseFolder
     *            base folder used for resolving relative paths
     * @param cssFile
     *            the CSS file to process
     * @param assetAliases
     *            theme asset aliases (only used when rewriting URLs)
     * @param bundleFor
     *            defines a way how bundler resolves url(...)
     * @param contextPath
     *            that url() are rewritten to and rebased onto
     * @param nodeModulesFolder
     *            the node_modules folder for resolving npm package imports
     * @param visitedFiles
     *            set of canonical paths already processed (for cycle detection)
     * @param cyclicImportWarnings
     *            list to collect cycle warnings for summary logging
     */
    private static String inlineImports(File baseFolder, File cssFile,
            Set<String> assetAliases, BundleFor bundleFor, String contextPath,
            File nodeModulesFolder, Set<String> visitedFiles,
            List<String> cyclicImportWarnings) throws IOException {

        // Track current file as visited using canonical path
        String canonicalPath = cssFile.getCanonicalPath();
        if (!visitedFiles.add(canonicalPath)) {
            // Already processed - cycle detected, will be logged in summary
            return "";
        }

        String content = Files.readString(cssFile.toPath());
        if (bundleFor == BundleFor.THEMES) {
            Matcher urlMatcher = URL_PATTERN.matcher(content);
            content = rewriteCssUrlsForThemes(baseFolder, cssFile, assetAliases,
                    urlMatcher);
        } else if (bundleFor == BundleFor.STATIC_RESOURCES) {
            content = rewriteCssUrlsForStaticResources(baseFolder, cssFile,
                    contextPath, content);
        } else if (bundleFor == BundleFor.STATIC_RESOURCES_RELATIVE) {
            content = rewriteCssUrlsForStaticResourcesRelative(baseFolder,
                    cssFile, content);
        }
        content = StringUtil.removeComments(content, true);
        List<String> unhandledImports = new ArrayList<>();
        Matcher importMatcher = IMPORT_PATTERN.matcher(content);
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
                File potentialFile = resolveImportPath(sanitizedUrl,
                        cssFile.getParentFile(), nodeModulesFolder);
                if (potentialFile != null && potentialFile.exists()) {
                    try {
                        // Check for cycle before recursing
                        String importedPath = potentialFile.getCanonicalPath();
                        if (visitedFiles.contains(importedPath)) {
                            // Cycle detected - file already inlined, just skip
                            cyclicImportWarnings.add(String.format(
                                    "'%s' imports already processed '%s'",
                                    cssFile.toPath().normalize()
                                            .toAbsolutePath(),
                                    potentialFile.toPath().normalize()
                                            .toAbsolutePath()));
                            return "";
                        }
                        return Matcher.quoteReplacement(inlineImports(
                                baseFolder, potentialFile, assetAliases,
                                bundleFor, contextPath, nodeModulesFolder,
                                visitedFiles, cyclicImportWarnings));
                    } catch (IOException e) {
                        getLogger().warn("Unable to inline import: {}",
                                result.group());
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

    private static String rewriteCssUrlsForStaticResources(File baseFolder,
            File cssFile, String contextPath, String content) {
        // Bundled CSS is delivered as inline <style> in dev mode, so url()s
        // need to be absolute paths rooted at the servlet context.
        return rewriteCssUrls(baseFolder, cssFile, content,
                target -> rebaseToContextPath(baseFolder, contextPath, target));
    }

    private static String rewriteCssUrlsForStaticResourcesRelative(
            File baseFolder, File cssFile, String content) {
        // Bundled CSS is written back to disk and served from the entry
        // file's URL in prod mode, so url()s can be expressed relative to
        // that entry folder.
        Path baseNormalized = baseFolder.toPath().normalize().toAbsolutePath();
        return rewriteCssUrls(baseFolder, cssFile, content,
                target -> baseNormalized.relativize(target).toString()
                        .replace('\\', '/'));
    }

    /**
     * Common url() rewriting pipeline used by both static-resource bundling
     * strategies. Walks every {@code url(...)} in {@code content}, skips ones
     * that should be left untouched (empty, absolute, protocol-prefixed,
     * unresolvable, outside {@code baseFolder}, or pointing at a missing file),
     * and lets the caller decide how to format the kept ones via
     * {@code targetToUrl}.
     */
    private static String rewriteCssUrls(File baseFolder, File cssFile,
            String content,
            java.util.function.Function<Path, String> targetToUrl) {
        Path baseNormalized = baseFolder.toPath().normalize().toAbsolutePath();
        Matcher urlMatcher = URL_PATTERN.matcher(content);
        return urlMatcher.replaceAll(result -> {
            String url = getNonNullGroup(result, 2, 3, 4);
            if (url == null || url.trim().endsWith(".css")) {
                // @import-style url()s are handled by import inlining, not
                // here.
                return Matcher.quoteReplacement(urlMatcher.group());
            }
            String sanitized = sanitizeUrl(url);
            if (sanitized == null) {
                return Matcher.quoteReplacement(urlMatcher.group());
            }
            String trimmed = sanitized.trim();
            // Only handle relative URLs: not empty, no leading slash, no
            // known protocol prefix. Match only known protocols as absolute
            // to avoid false positives like "my:file.css".
            if (trimmed.isEmpty() || trimmed.startsWith("/")
                    || PROTOCOL_PATTER_FOR_URLS.matcher(trimmed).matches()) {
                return Matcher.quoteReplacement(urlMatcher.group());
            }
            try {
                Path target = cssFile.getParentFile().toPath().resolve(trimmed)
                        .normalize().toAbsolutePath();
                if (!target.startsWith(baseNormalized)) {
                    // Target is outside the entry's base folder (e.g. an
                    // npm-package CSS referencing a sibling file). Don't
                    // invent a path for it.
                    return Matcher.quoteReplacement(urlMatcher.group());
                }
                if (!Files.exists(target)) {
                    // Don't rewrite something we can't confirm.
                    return Matcher.quoteReplacement(urlMatcher.group());
                }
                return Matcher.quoteReplacement(
                        "url('" + targetToUrl.apply(target) + "')");
            } catch (Exception e) {
                getLogger().debug("Unable to resolve url: {}",
                        urlMatcher.group());
            }
            return Matcher.quoteReplacement(urlMatcher.group());
        });
    }

    private static String rewriteCssUrlsForThemes(File baseFolder, File cssFile,
            Set<String> assetAliases, Matcher urlMatcher) {
        String content;
        content = urlMatcher.replaceAll(result -> {
            String url = getNonNullGroup(result, 2, 3, 4);
            if (url == null || url.trim().endsWith(".css")) {
                // These are handled below
                return Matcher.quoteReplacement(urlMatcher.group());
            }
            File potentialFile = new File(cssFile.getParentFile(), url.trim());
            if (potentialFile.exists()) {
                // @formatter:off
                // e.g., background-image: url("./foo/bar.png") should become
                // url("VAADIN/themes/<theme-name>/foo/bar.png, IF the file
                // is inside the theme folder. Otherwise, "./foo/bar.png"
                // can also refer to a file in
                // src/main/resources/META-INF/resources/foo/bar.png, and
                // then we don't need a rewrite. Also, the imports are
                // relative to the folder containing theCSS file we are
                // processing, not always relative to the theme folder.
                // @formatter:on
                String relativePath = baseFolder.getParentFile().toPath()
                        .relativize(potentialFile.toPath()).toString()
                        .replaceAll("\\\\", "/");
                return Matcher.quoteReplacement(
                        "url('VAADIN/themes/" + relativePath + "')");
            } else if (isPotentialThemeAsset(baseFolder, assetAliases,
                    potentialFile)) {
                // @formatter:off
                // a reference to a theme asset, e.g., with a theme.json config:
                // {
                //     "assets": {
                //         "@some/pkg": {
                //             "svgs/regular/**": "my/icons"
                //         }
                //     }
                // }
                // background-image: url("./my/icons/bar.png") should become
                // url("VAADIN/themes/<theme-name>/my/icons/bar.png
                // @formatter:on
                String relativePath = baseFolder.getParentFile().toPath()
                        .relativize(potentialFile.toPath()).toString()
                        .replaceAll("\\\\", "/");
                return Matcher.quoteReplacement(
                        "url('VAADIN/themes/" + relativePath + "')");
            }

            return Matcher.quoteReplacement(urlMatcher.group());
        });
        return content;
    }

    private static String rebaseToContextPath(File baseFolder,
            String contextPath, Path target) {
        String publicRelativePath = baseFolder.toPath().relativize(target)
                .toString().replaceAll("\\\\", "/");
        String context = contextPath == null ? "" : contextPath.trim();
        if (!context.isEmpty() && !context.startsWith("/")) {
            context = "/" + context;
        }
        if (context.endsWith("/")) {
            context = context.substring(0, context.length() - 1);
        }
        return (context.isEmpty() ? "" : context) + "/" + publicRelativePath;
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

    /**
     * Resolve import path to a file. First check relative to the CSS file's,
     * then check node_modules for non-relative path.
     *
     * @param importPath
     *            the import path from the CSS file
     * @param cssFileDir
     *            the directory containing the CSS file
     * @param nodeModulesFolder
     *            the node_modules folder, may be null
     * @return the resolved file, or null if not found
     */
    private static File resolveImportPath(String importPath, File cssFileDir,
            File nodeModulesFolder) {
        // First, try relative to the CSS file's directory
        File relativeFile = new File(cssFileDir, importPath);
        if (relativeFile.exists()) {
            return relativeFile;
        }

        // If not a relative path (doesn't start with ./ or ../) and
        // node_modules is available, try resolving from node_modules
        if (nodeModulesFolder != null && !importPath.startsWith("./")
                && !importPath.startsWith("../")) {
            File nodeModulesFile = new File(nodeModulesFolder, importPath);
            if (nodeModulesFile.exists()) {
                return nodeModulesFile;
            }
        }

        return null;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CssBundler.class);
    }

    /**
     * Minify CSS content by removing comments and unnecessary whitespace.
     * <p>
     * This method performs basic CSS minification:
     * <ul>
     * <li>Remove CSS comments</li>
     * <li>Collapse multiple whitespace characters</li>
     * <li>Remove whitespace around special characters like braces and
     * colons</li>
     * <li>Remove trailing semicolons before closing braces</li>
     * </ul>
     *
     * @param css
     *            the CSS content to minify
     * @return the minified CSS content
     */
    public static String minifyCss(String css) {
        // Remove CSS comments /* ... */
        css = StringUtil.removeComments(css, true);
        // Collapse whitespace
        css = css.replaceAll("\\s+", " ");
        // Remove spaces around special characters
        css = css.replaceAll("\\s*([{};,>~])\\s*", "$1");
        // Remove trailing semicolons before }
        css = css.replaceAll(";}", "}");
        return css.trim();
    }

}
