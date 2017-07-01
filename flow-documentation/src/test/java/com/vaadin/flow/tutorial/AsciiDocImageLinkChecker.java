package com.vaadin.flow.tutorial;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Vaadin Ltd.
 */
public class AsciiDocImageLinkChecker implements TutorialLineChecker {
    private static final String ASCII_DOC_IMAGE_LINK_SEPARATOR = "image:";

    @Override
    public Collection<String> verifyTutorialLine(String tutorialName, String line) {
        if (!line.contains(ASCII_DOC_IMAGE_LINK_SEPARATOR)) {
            return Collections.emptyList();
        }
        // TODO kirill
        return Collections.emptyList();
    }
}
