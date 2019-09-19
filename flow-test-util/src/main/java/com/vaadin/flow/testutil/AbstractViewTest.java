/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.testutil;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * A base class for testing a view using TestBench. It opens the view and wait
 * for its root element to be present. It also checks errors in browser log at
 * both before and after phases of each test.
 *
 * @since 2.0
 */
public abstract class AbstractViewTest extends ChromeBrowserTest {
    private final By rootElementSelector;
    private WebElement rootElement;

    public AbstractViewTest(By rootElementSelector) {
        this.rootElementSelector = rootElementSelector;
    }

    @Before
    public void initialCheck() {
        open();

        waitForElementPresent(rootElementSelector);
        rootElement = findElement(rootElementSelector);

        checkLogsForErrors();
    }

    @After
    public void finalCheck() {
        checkLogsForErrors();
    }

    public WebElement getRootElement() {
        return rootElement;
    }
}
