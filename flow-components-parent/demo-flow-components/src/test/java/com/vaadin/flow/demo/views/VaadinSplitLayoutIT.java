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
package com.vaadin.flow.demo.views;

import org.junit.Before;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;

/**
 * Integration tests for {@link VaadinSplitLayoutView}.
 */
public class VaadinSplitLayoutIT extends AbstractChromeTest {

    private WebElement layout;

    @Override
    protected String getTestPath() {
        return "/vaadin-split-layout";
    }

    @Before
    public void init() {
        open();
        waitForElementPresent(By.tagName("main-layout"));
        layout = findElement(By.tagName("main-layout"));
    }
}
