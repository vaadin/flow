package com.vaadin.flow.tutorial;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Vaadin Ltd.
 */
class CodeFileChecker implements TutorialLineChecker {
    private static final String CODE_DECLARATION_LINE = "----";

    private final String codeBlockIdentifier;
    private final Map<String, Set<String>> allowedLinesMap;

    private boolean blockStarted;
    private boolean inBlock;

    CodeFileChecker(String codeBlockIdentifier,
            Map<String, Set<String>> allowedLinesMap) {
        this.codeBlockIdentifier = codeBlockIdentifier;
        this.allowedLinesMap = allowedLinesMap;
    }

    @Override
    public Collection<String> verifyTutorialLine(Path tutorialPath,
            String tutorialName, String line) {
        Optional<String> validationResult = Optional.empty();
        if (blockStarted) {
            validationResult = validateBlockStart(tutorialName, line);
        } else if (inBlock) {
            validationResult = validateBlockLine(tutorialName, line);
        } else if (codeBlockIdentifier.equals(line)) {
            blockStarted = true;
        }
        return validationResult.map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    private Optional<String> validateBlockLine(String tutorialName,
            String line) {
        if (line.equals(CODE_DECLARATION_LINE)) {
            inBlock = false;
        } else {
            Set<String> allowedLines = allowedLinesMap.get(tutorialName);
            if (allowedLines == null) {
                return Optional.of(String.format(
                        "Tutorial %s has the code block, but has no corresponding code files",
                        tutorialName));
            }

            if (!allowedLines
                    .contains(TestTutorialCodeCoverage.trimWhitespace(line))) {
                return Optional.of(String.format(
                        "Tutorial %s contains the code line '%s' that is not present in any of the corresponding code files",
                        tutorialName, line));
            }
        }
        return Optional.empty();
    }

    private Optional<String> validateBlockStart(String tutorialName,
            String line) {
        if (!CODE_DECLARATION_LINE.equals(line)) {
            return Optional.of(String.format(
                    "Tutorial %s has incorrect code block declaration: code block should be surrounded with '%s', got '%s' instead",
                    tutorialName, CODE_DECLARATION_LINE, line));
        }
        blockStarted = false;
        inBlock = true;
        return Optional.empty();
    }
}
