package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;

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

    public static String inlineImports(File themeFolder, File cssFile,
            String pathToJar) throws IOException {

        String content = getStylesheetContent(pathToJar, cssFile);

        Matcher urlMatcher = urlPattern.matcher(content);
        content = urlMatcher.replaceAll(result -> {
            String url = getNonNullGroup(result, 2, 3);
            if (url == null || url.trim().endsWith(".css")) {
                // These are handled below
                return urlMatcher.group();
            }

            File potentialFile = new File(cssFile.getParentFile(), url.trim());

            boolean potentialFileExists = false;
            try {
                potentialFileExists = potentialFileExistsInThemes(potentialFile,
                        pathToJar);
            } catch (IOException e) {
                getLogger().warn(
                        "Unable to inline import {}, because couldn't resolve URL paths",
                        result.group(), e);
            }

            if (potentialFileExists) {
                // e.g. background-image: url("./foo/bar.png") should become
                // url("VAADIN/themes/<theme-name>>/foo/bar.png IF the file is
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
                return "url('VAADIN/themes/" + relativePath + "')";
            }

            return urlMatcher.group();
        });
        Matcher importMatcher = importPattern.matcher(content);
        content = importMatcher.replaceAll(result -> {
            // Oh the horror
            // Group 3,4,5 are url() urls with different quotes
            // Group 7,8 are strings with different quotes
            // Group 9 is layer info
            String layerOrMediaQueryInfo = result.group(9);
            if (layerOrMediaQueryInfo != null
                    && !layerOrMediaQueryInfo.isBlank()) {
                return result.group();
            }
            String url = getNonNullGroup(result, 3, 4, 5, 7, 8);
            if (url == null || !url.trim().endsWith(".css")) {
                return result.group();
            }

            File potentialFile = new File(cssFile.getParentFile(), url.trim());
            boolean potentialFileExists = false;
            try {
                potentialFileExists = potentialFileExistsInThemes(potentialFile,
                        pathToJar);
            } catch (IOException e) {
                getLogger().warn(
                        "Unable to inline import {}, because couldn't resolve URL paths",
                        result.group(), e);
            }

            if (potentialFileExists) {
                try {
                    return inlineImports(themeFolder, potentialFile, pathToJar);
                } catch (IOException e) {
                    getLogger()
                            .warn("Unable to inline import: " + result.group());
                }
            }
            return result.group();
        });

        return content;
    }

    private static boolean potentialFileExistsInThemes(File potentialFile,
            String pathToJar) throws IOException {
        if (pathToJar != null) {
            URL potentialFileUrl = getCssResourceUrl(pathToJar, potentialFile);
            return !(potentialFileUrl.toString()
                    .contains(Constants.RESOURCES_JAR_DEFAULT)
                    || potentialFileUrl.toString()
                            .contains(Constants.VAADIN_WEBAPP_RESOURCES));
        } else {
            return potentialFile.exists();
        }
    }

    private static String getStylesheetContent(String basePath, File cssFile)
            throws IOException {
        if (basePath != null) {
            URL resourceUrl = getCssResourceUrl(basePath, cssFile);
            return IOUtils.toString(resourceUrl, StandardCharsets.UTF_8);
        } else {
            return FileUtils.readFileToString(cssFile, StandardCharsets.UTF_8);
        }
    }

    private static URL getCssResourceUrl(String pathToJar,
            File resourceRelativePath) throws IOException {
        String path = resourceRelativePath.toString();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        try {
            return new URL(pathToJar + "!" + path);
        } catch (MalformedURLException e) {
            throw new IOException(
                    "Couldn't get the URL for stylesheet resource", e);
        }
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

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CssBundler.class);
    }

}
