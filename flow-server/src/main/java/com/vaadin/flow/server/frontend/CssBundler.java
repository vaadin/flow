package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CssBundler {

    private static final String WHITE_SPACE = "\\s*";

    private static final String STRING = "(.*)";

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

    private static Pattern urlMatcher = Pattern.compile("@import" + WHITE_SPACE
            + URL_OR_STRING + MAYBE_LAYER_OR_MEDIA_QUERY + WHITE_SPACE + ";");

    public static String inlineImports(File cssFile) throws IOException {
        String content = FileUtils.readFileToString(cssFile,
                StandardCharsets.UTF_8);
        Matcher matcher = urlMatcher.matcher(content);
        return matcher.replaceAll(result -> {
            // Oh the horror
            // Group 3,4,5 are url() urls with different quotes
            // Group 7,8 are strings with different quotes
            // Group 9 is layer info
            String layerOrMediaQueryInfo = result.group(9);
            if (layerOrMediaQueryInfo != null
                    && !layerOrMediaQueryInfo.trim().isEmpty()) {
                return result.group();
            }
            String url = getNonNullGroup(result, 3, 4, 5, 7, 8);
            if (url == null) {
                return result.group();
            }

            File potentialFile = new File(cssFile.getParentFile(), url.trim());
            if (potentialFile.exists()) {
                try {
                    return inlineImports(potentialFile);
                } catch (IOException e) {
                    getLogger()
                            .warn("Unable to inline import: " + result.group());
                }
            }
            return result.group();
        });

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
