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
class AsciiDocLinkWithDescriptionChecker implements TutorialLineChecker {
    private final Set<String> checkedTutorialPaths = new HashSet<>();

    private final String linkSyntaxFragment;
    private final Pattern linkPattern;
    private final String fileExtension;

    AsciiDocLinkWithDescriptionChecker(String linkSyntaxFragment,
            Pattern linkPattern) {
        this(linkSyntaxFragment, linkPattern, null);
    }

    AsciiDocLinkWithDescriptionChecker(String linkSyntaxFragment,
            Pattern linkPattern, String linkFileExtension) {
        this.linkSyntaxFragment = linkSyntaxFragment;
        this.linkPattern = linkPattern;
        this.fileExtension = linkFileExtension == null ? "" : linkFileExtension;
    }

    @Override
    public Collection<String> verifyTutorialLine(Path tutorialPath,
            String tutorialName, String line) {
        if (!line.contains(linkSyntaxFragment)) {
            return Collections.emptyList();
        }

        List<String> validationErrors = new ArrayList<>();

        Matcher matcher = linkPattern.matcher(line);
        while (matcher.find()) {
            if (matcher.groupCount() == 2) {
                validateLinkAndDescription(tutorialPath, tutorialName, line,
                        matcher.group(1), matcher.group(2))
                                .ifPresent(validationErrors::add);
            } else {
                validationErrors.add(String.format(
                        "Received malformed asciidoc link, tutorial = %s, line = %s",
                        tutorialName, line));
            }
        }
        return validationErrors;
    }

    private Optional<String> validateLinkAndDescription(Path tutorialPath,
            String tutorialName, String line, String asciiDocLink,
            String description) {
        if (description == null || description.isEmpty()) {
            return Optional.of(String.format(
                    "Asciidoc link description is empty or malformed, tutorial = %s, line = %s",
                    tutorialName, line));
        }

        if (asciiDocLink == null || asciiDocLink.isEmpty()) {
            return Optional.of(String.format(
                    "Asciidoc link is empty or malformed, tutorial = %s, line = %s",
                    tutorialName, line));
        }

        if (checkedTutorialPaths.add(asciiDocLink)) {
            Path externalTutorialPath = Paths.get(
                    tutorialPath.getParent().toString(),
                    asciiDocLink + fileExtension);
            if (!Files.isRegularFile(externalTutorialPath)) {
                return Optional.of(String.format(
                        "Could not locate file '%s' referenced in tutorial %s",
                        asciiDocLink, tutorialName));
            }
        }
        return Optional.empty();
    }
}
