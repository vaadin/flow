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
package com.vaadin.flow.test.cspstrict.dom;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

/**
 * Event data expressions are still compiled by the browser, and the server
 * rejects them in the strict mode, so the view does not even render. Enabled
 * when the property paths of event data are sent without JavaScript.
 */
@Disabled("Blocked by https://github.com/vaadin/flow/issues/26152")
public class ClickIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/click";
    }

    @BrowserTest
    public void componentClickListener_receivesEventData() {
        open();

        $(NativeButtonElement.class).id("button").click();

        waitUntil(ExpectedConditions.textMatches(By.id("clicked"),
                Pattern.compile("clicked at x=\\d+")));
        assertNoCspViolations();
    }

    @BrowserTest
    public void domEventListener_receivesEventData() {
        open();

        $(DivElement.class).id("area").click();

        waitUntil(ExpectedConditions.textMatches(By.id("client-x"),
                Pattern.compile("x=\\d+")));
        assertNoCspViolations();
    }
}
