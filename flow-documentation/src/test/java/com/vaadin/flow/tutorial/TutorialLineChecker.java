package com.vaadin.flow.tutorial;

import java.util.Collection;

/**
 * @author Vaadin Ltd.
 */
public interface TutorialLineChecker {

    Collection<String> verifyTutorialLine(String tutorialName, String line);
}
