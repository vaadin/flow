package com.vaadin.flow.tutorial;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vaadin Ltd.
 */
public class AsciiDocDocumentLinkChecker implements TutorialLineChecker {
    private static final String ASCII_DOC_DOCUMENT_LINK_SEPARATOR = "#,";
    private static final Pattern ASCII_DOC_LINK_PATTERN = Pattern
            .compile("<<(.*?)>>");

    private final Set<String> checkedTutorialPaths = new HashSet<>();

    @Override
    public Collection<String> verifyTutorialLine(Path tutorialPath,
            String tutorialName, String line) {
        if (!line.contains(ASCII_DOC_DOCUMENT_LINK_SEPARATOR)) {
            return Collections.emptyList();
        }
        return checkAsciiDocLinks(tutorialPath, line);
    }

    private Collection<String> checkAsciiDocLinks(Path tutorialPath,
            String line) {
        List<String> validationErrors = new ArrayList<>();
        for (String[] urlAndDescription : extractUrlsAndDescriptions(
                ASCII_DOC_LINK_PATTERN.matcher(line))) {
            Optional<String> validationError = checkUrlAndDescriptionPresence(
                    urlAndDescription, line);
            if (validationError.isPresent()) {
                validationErrors.add(validationError.get());
                continue;
            }

            String externalTutorialUrl = urlAndDescription[0];
            if (checkedTutorialPaths.add(externalTutorialUrl)) {
                Path externalTutorialPath = Paths.get(
                        tutorialPath.getParent().toString(), externalTutorialUrl
                                + TestTutorialCodeCoverage.ASCII_DOC_EXTENSION);
                if (!Files.isRegularFile(externalTutorialPath)) {
                    validationErrors.add(String.format(
                            "Could not locate file '%s' referenced in tutorial %s",
                            externalTutorialUrl, tutorialPath));
                }
            }
        }
        return validationErrors;
    }

    private Optional<String> checkUrlAndDescriptionPresence(
            String[] urlAndDescription, String line) {
        if (urlAndDescription.length > 2) {
            // No use to continue checks further: the test is implemented
            // incorrectly
            throw new IllegalStateException(String.format(
                    "Have received more than 2 elements in parsed asciidoc url, original line = %s",
                    line));
        } else if (urlAndDescription.length < 2
                || urlAndDescription[0].isEmpty()
                || urlAndDescription[1].isEmpty()) {
            boolean isUrlMissing = urlAndDescription.length == 0
                    || urlAndDescription[0].isEmpty();
            return Optional.of(String.format(
                    "Incorrect asciidoc url declaration: %s is missing, original line = '%s'",
                    isUrlMissing ? "url" : "description", line));
        }
        return Optional.empty();
    }

    private Collection<String[]> extractUrlsAndDescriptions(Matcher matcher) {
        List<String[]> result = new ArrayList<>();
        while (matcher.find()) {
            // Example of the url link: <<tutorial-component-basic#,Creating A
            // Simple Component Using the Element API>>
            String urlAndDescription = matcher.group(1);
            result.add(
                    urlAndDescription.split(ASCII_DOC_DOCUMENT_LINK_SEPARATOR));
        }
        return result;
    }
}
