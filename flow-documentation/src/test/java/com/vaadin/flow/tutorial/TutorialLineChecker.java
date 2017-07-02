package com.vaadin.flow.tutorial;

import java.nio.file.Path;
import java.util.Collection;

/**
 * @author Vaadin Ltd.
 */
public interface TutorialLineChecker {
    Collection<String> verifyTutorialLine(Path tutorialPath, String tutorialName, String line);
}
