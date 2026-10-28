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

import org.junit.jupiter.api.Disabled;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

@Disabled(AbstractCspStrictIT.NO_UI_IN_STRICT_MODE)
public class DeclaredJsIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/declared-js";
    }

    @BrowserTest
    public void declaredJavaScript_runsFromTheBundle() {
        open();

        waitUntil(ExpectedConditions.textToBe(By.id("status"),
                "declared JavaScript ran"));
        assertNoCspViolations();
    }
}
