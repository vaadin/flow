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
public class AsciiDocImageLinkChecker implements TutorialLineChecker {
    private final Set<String> checkedTutorialPaths = new HashSet<>();

    private final String linkElement = "image:";
    private final Pattern linkPattern = Pattern.compile("image:(.*?)\\[(.*?)]");
    private final String fileExtension = null;

    @Override
    public Collection<String> verifyTutorialLine(Path tutorialPath,
            String tutorialName, String line) {
        if (!line.contains(linkElement)) {
            return Collections.emptyList();
        }
        List<String> validationErrors = new ArrayList<>();

        Matcher matcher = linkPattern.matcher(line);
        while (matcher.find()) {
            if (matcher.groupCount() != 2) {
                validationErrors.add(String.format(
                        "Received malformed asciidoc link, tutorial = %s, line = %s",
                        tutorialName, line));
                continue;
            }

            String description = matcher.group(2);
            if (description.isEmpty()) {
                validationErrors.add(String.format(
                        "Image url description is empty or malformed, tutorial = %s, line = %s",
                        tutorialName, line));
            }

            String imageUrl = matcher.group(1);
            if (imageUrl == null || imageUrl.isEmpty()) {
                validationErrors.add(String.format(
                        "Image url is empty or malformed, tutorial = %s, line = %s",
                        tutorialName, line));
                continue;
            }

            if (checkedTutorialPaths.add(imageUrl)) {
                Path externalTutorialPath = Paths.get(
                        tutorialPath.getParent().toString(),
                        imageUrl + (fileExtension == null ? "" : fileExtension));
                if (!Files.isRegularFile(externalTutorialPath)) {
                    validationErrors.add(String.format(
                            "Could not locate image '%s' referenced in tutorial %s",
                            imageUrl, tutorialPath));
                }
            }
        }
        return validationErrors;
    }
}
