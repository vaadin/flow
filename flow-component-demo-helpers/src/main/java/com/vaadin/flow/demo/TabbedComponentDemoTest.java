/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
