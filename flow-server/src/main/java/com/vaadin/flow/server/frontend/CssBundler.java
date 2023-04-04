package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CssBundler {

    private static Pattern urlMatcher = Pattern
            .compile("@import\\s*(url\\(\\s*)(\\'|\\\")?(\\S*)(\\2\\s*\\));?");

    public static String inlineImports(File cssFile) throws IOException {
        String content = FileUtils.readFileToString(cssFile,
                StandardCharsets.UTF_8);
        Matcher matcher = urlMatcher.matcher(content);
        return matcher.replaceAll(result -> {
            String url = result.group(3);
            File potentialFile = new File(cssFile.getParentFile(), url);
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

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CssBundler.class);
    }

}
