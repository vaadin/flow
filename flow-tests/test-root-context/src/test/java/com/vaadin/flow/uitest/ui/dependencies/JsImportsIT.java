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

public class JsImportsIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
        waitForElementPresent(By.id("named"));
    }

    @Test
    public void namedImports_availableToExpression() {
        waitUntil(driver -> !findElement(By.id("named")).getText().isEmpty());
        Assert.assertEquals("named from-js-imports-module",
                findElement(By.id("named")).getText());
    }

    @Test
    public void importAll_namespaceAvailableToExpression() {
        waitUntil(
                driver -> !findElement(By.id("namespace")).getText().isEmpty());
        Assert.assertEquals("namespace from-js-imports-module",
                findElement(By.id("namespace")).getText());
        // The default export is reachable through the namespace object as well
        Assert.assertEquals("yes",
                findElement(By.id("namespace")).getAttribute("data-default"));
    }

    @Test
    public void importsUsedInLaterRoundTrip_chunkStillAvailable() {
        // The chunk is only requested once, so a later expression has to work
        // without it being loaded again
        findElement(By.id("run-deferred")).click();
        waitUntil(
                driver -> !findElement(By.id("deferred")).getText().isEmpty());
        Assert.assertEquals("deferred from-js-imports-module",
                findElement(By.id("deferred")).getText());
    }
}
