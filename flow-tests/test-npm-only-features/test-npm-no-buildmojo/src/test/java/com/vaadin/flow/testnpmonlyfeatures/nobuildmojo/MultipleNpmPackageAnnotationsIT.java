/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class MultipleNpmPackageAnnotationsIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void pageShouldContainTwoPaperComponents() {
        TestBenchElement paperInput = $("paper-input").first();
        TestBenchElement paperCheckbox = $("paper-checkbox").first();

        // check that elements are on the page
        Assert.assertNotNull(paperInput);
        Assert.assertNotNull(paperCheckbox);

        // verify that the paper components are upgraded
        Assert.assertNotNull(paperInput.$("paper-input-container"));
        Assert.assertNotNull(paperCheckbox.$("checkboxContainer"));
    }

    // Tests funtionaity of TaskCopyLocalFrontendFiles
    @Test
    public void lazyComponentShouldExistInBody() {
        waitForElementPresent(By.id("lazy-element"));
        WebElement element = findElement(By.id("lazy-element"));

        Assert.assertTrue("Lazy created element should be displayed",
                element.isDisplayed());
    }
}
