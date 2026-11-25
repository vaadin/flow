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
package com.vaadin.flow.test.cspstrict.csp;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

/**
 * A {@code data:} URL can be fetched under the policy, which an {@code SvgIcon}
 * given as a {@code data:image/svg+xml} URL needs.
 */
public class DataUrlFetchIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/data-url-fetch";
    }

    @BrowserTest
    public void dataUrl_isFetched() {
        open();

        WebElement fetcher = findElement(By.id("fetcher"));
        waitUntil(driver -> !fetcher.getText().isEmpty());
        assertNoCspViolations();
        Assertions.assertEquals("Fetched", fetcher.getText());
    }
}
