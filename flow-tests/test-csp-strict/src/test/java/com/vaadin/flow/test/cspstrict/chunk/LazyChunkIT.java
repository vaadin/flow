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
package com.vaadin.flow.test.cspstrict.chunk;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

/**
 * Navigating to a view loads the chunk with its frontend module under the
 * policy.
 */
public class LazyChunkIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/lazy-chunk-start";
    }

    @BrowserTest
    public void navigation_loadsTheChunkOfTheTarget() {
        open();
        Assertions.assertEquals(false, isLazyElementDefined(),
                "The module of the target view should not be loaded with the page");

        $(AnchorElement.class).id("link").click();

        waitUntil(ExpectedConditions.textToBe(By.id("lazy"),
                "Loaded from a lazy chunk"));
        assertNoCspViolations();
    }

    private Object isLazyElementDefined() {
        return ((JavascriptExecutor) getDriver()).executeScript(
                "return customElements.get('csp-lazy-element') !== undefined");
    }
}
