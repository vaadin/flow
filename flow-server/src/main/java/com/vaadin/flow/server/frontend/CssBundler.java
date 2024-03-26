package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static String inlineImports(File themeFolder, File cssFile)
            throws IOException {
        String content = FileUtils.readFileToString(cssFile,
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
                        return Matcher.quoteReplacement(
                                inlineImports(themeFolder, potentialFile));
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
