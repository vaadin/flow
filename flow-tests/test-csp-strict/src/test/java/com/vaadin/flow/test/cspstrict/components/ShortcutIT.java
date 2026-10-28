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
package com.vaadin.flow.test.cspstrict.components;

import org.junit.jupiter.api.Disabled;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

/**
 * The shortcut registration builds its key filter as a string, which the strict
 * mode rejects. Enabled when the shortcut listener is a fixed declared
 * expression.
 */
@Disabled("Blocked by https://github.com/vaadin/flow/issues/26666")
public class ShortcutIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/shortcut";
    }

    @BrowserTest
    public void shortcut_firesTheListener() {
        open();

        $(InputTextElement.class).id("input")
                .sendKeys(Keys.chord(Keys.CONTROL, "s"));

        waitUntil(ExpectedConditions.textToBe(By.id("fired"), "Ctrl+S fired"));
        assertNoCspViolations();
    }
}
