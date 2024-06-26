/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.demo;

import org.openqa.selenium.By;

/**
 * Base class for the component integration tests for views that use tabs.
 *
 * @since 1.0
 */
public abstract class TabbedComponentDemoTest extends ComponentDemoTest {

    @Override
    public void openDemoPageAndCheckForErrors() {
        // NO-OP
    }

    /**
     * Navigates from one tab to another and checks whether the navigation
     * causes errors in the browser. The {@code tab} will be added to the
     * default test-path for navigation.
     *
     * @param tab
     *            the name of the tab to navigate to
     */
    public void openTabAndCheckForErrors(String tab) {
        String testPath = getTestURL();
        if (tab != null && !tab.isEmpty()) {
            testPath = testPath + (testPath.endsWith("/") ? tab : "/" + tab);
        }
        getDriver().get(testPath);
        waitForElementPresent(By.className("demo-view"));
        layout = findElement(By.className("demo-view"));
        checkLogsForErrors();
    }

}
