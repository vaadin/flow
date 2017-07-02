package com.vaadin.flow.tutorial;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
                String description = matcher.group(2);
                if (description == null || description.isEmpty()) {
                    validationErrors.add(String.format(
                            "Asciidoc link description is empty or malformed, tutorial = %s, line = %s",
                            tutorialName, line));
                }

                String asciiDocLink = matcher.group(1);
                if (asciiDocLink == null || asciiDocLink.isEmpty()) {
                    validationErrors.add(String.format(
                            "Asciidoc link is empty or malformed, tutorial = %s, line = %s",
                            tutorialName, line));
                } else if (checkedTutorialPaths.add(asciiDocLink)) {
                    Path externalTutorialPath = Paths.get(
                            tutorialPath.getParent().toString(),
                            asciiDocLink + fileExtension);
                    if (!Files.isRegularFile(externalTutorialPath)) {
                        validationErrors.add(String.format(
                                "Could not locate file '%s' referenced in tutorial %s",
                                asciiDocLink, tutorialPath));
                    }
                }
            } else {
                validationErrors.add(String.format(
                        "Received malformed asciidoc link, tutorial = %s, line = %s",
                        tutorialName, line));
            }
        }
        return validationErrors;
    }
}
