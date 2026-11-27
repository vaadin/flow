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
package com.vaadin.flow.test.cspstrict.js;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

public class DeclaredJsImportIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/declared-js-import";
    }

    @BrowserTest
    public void importedModule_isLoadedWhenTheDefinitionIsCalled() {
        open();
        NativeButtonElement run = $(NativeButtonElement.class).id("run");

        Assertions.assertNotEquals(Boolean.TRUE,
                executeScript("return window.jsDefinitionImportLoaded"),
                "the module should not be loaded before a call needs it");

        run.click();

        waitUntil(ExpectedConditions.textToBe(By.id("status"),
                "DESCRIBED IMPORTED true from add-on"));
        Assertions.assertEquals(Boolean.TRUE,
                executeScript("return window.jsDefinitionImportLoaded"));
        assertNoCspViolations();
    }
}
