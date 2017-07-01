package com.vaadin.flow.tutorial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vaadin Ltd.
 */
public class AsciiDocDocumentLinkChecker implements TutorialLineChecker {
    private static final String ASCII_DOC_DOCUMENT_LINK_SEPARATOR = "#,";
    private static final Pattern ASCII_DOC_LINK_PATTERN = Pattern
            .compile("<<(.*?)>>");

    @Override
    public Collection<String> verifyTutorialLine(String tutorialName, String line) {
        if (!line.contains(ASCII_DOC_DOCUMENT_LINK_SEPARATOR)) {
            return Collections.emptyList();
        }
        //TODO kirill
        return Collections.emptyList();
    }

    private Collection<String> extractRegexGroupMatches(Matcher matcher) {
        List<String> matchingResults = new ArrayList<>();
        while (matcher.find()) {
            matchingResults.add(matcher.group(1));
        }
        return matchingResults;
    }

    private void checkAsciiDocLinks(String line) {
        // TODO kirill
        extractRegexGroupMatches(ASCII_DOC_LINK_PATTERN.matcher(line));
    }
}
