package com.vaadin.flow.tutorial;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Vaadin Ltd.
 */
public class CodeFileChecker implements TutorialLineChecker {
    private static final String CODE_DECLARATION_LINE = "----";

    private final String blockIdentifier;
    private final Map<String, Set<String>> allowedLinesMap;

    private boolean blockStarted;
    private boolean inBlock;

    CodeFileChecker(String codeBlockIdentifier,
            Map<String, Set<String>> allowedLinesMap) {
        this.blockIdentifier = codeBlockIdentifier;
        this.allowedLinesMap = allowedLinesMap;
    }

    @Override
    public Collection<String> verifyTutorialLine(String tutorialName,
                                                 String line) {
        Set<String> allowedLines = allowedLinesMap.get(tutorialName);
        if (blockStarted) {
            if (!CODE_DECLARATION_LINE.equals(line)) {
                return Collections.singletonList(String.format(
                        "Tutorial %s has incorrect code block declaration: code block should be surrounded with '%s', got '%s' instead",
                        tutorialName, CODE_DECLARATION_LINE, line));
            }
            blockStarted = false;
            inBlock = true;
        } else if (inBlock) {
            if (line.equals(CODE_DECLARATION_LINE)) {
                inBlock = false;
            } else {
                String trimmedLine = trimWhitespace(line);
                if (!allowedLines.contains(trimmedLine)) {
                    return Collections.singletonList(String.format(
                            "Tutorial %s contains the code line '%s' that is not present in any of the corresponding code files",
                            tutorialName, line));
                }
            }
        } else if (blockIdentifier.equals(line)) {
            blockStarted = true;
        }
        return Collections.emptyList();
    }

    private static String trimWhitespace(String codeLine) {
        return codeLine.replaceAll("\\s", "");
    }
}
