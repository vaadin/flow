/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RuntimeJavaScriptModuleIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
        waitForElementPresent(By.id("view-content"));
    }

    @Test
    public void eagerModule_addedAsModuleScriptAndEvaluated() {
        assertModuleScriptPresent("dependencies/runtime-module.js");
        // The file only parses when loaded as a module, so the marker being
        // there proves it was evaluated as one
        waitForElementPresent(By.id("eager-module-marker"));
        Assert.assertEquals("runtime-module.js",
                findElement(By.id("eager-module-marker")).getText());
    }

    @Test
    public void lazyModule_addedAsModuleScriptAndEvaluated() {
        assertModuleScriptPresent("dependencies/runtime-module-lazy.js");
        waitForElementPresent(By.id("lazy-module-marker"));
        Assert.assertEquals("runtime-module-lazy.js",
                findElement(By.id("lazy-module-marker")).getText());
    }

    private void assertModuleScriptPresent(String path) {
        Assert.assertTrue(path
                + " should be added to the page as a script tag with type=module",
                findElements(By.tagName("script")).stream().anyMatch(script -> {
                    String src = script.getAttribute("src");
                    return src != null && src.endsWith(path)
                            && "module".equals(script.getAttribute("type"));
                }));
    }
}
