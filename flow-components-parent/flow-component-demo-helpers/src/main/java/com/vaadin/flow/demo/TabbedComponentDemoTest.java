/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.testbench.By;

/**
 * Base class for the integration tests for views that use tabs.
 *
 */
public abstract class TabbedComponentDemoTest extends ComponentDemoTest {

    @Override
    public void openDemoPageAndCheckForErrors() {
        // NO-OP
    }

    public void openDemoPageAndCheckForErrors(String tab) {
        String testPath = getTestURL();
        if (tab != null && !tab.isEmpty()) {
            testPath = testPath + (testPath.endsWith("/") ? tab : "/" + tab);
        }
        getDriver().get(testPath);
        System.out.println(getDriver().getCurrentUrl());
        waitForElementPresent(By.tagName("div"));
        layout = findElement(By.tagName("div"));
        checkLogsForErrors();
    }

}
