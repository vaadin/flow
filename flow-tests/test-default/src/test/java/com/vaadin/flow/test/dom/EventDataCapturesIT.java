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
package com.vaadin.flow.test.dom;

import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(EventDataCapturesView.class)
public class EventDataCapturesIT extends AbstractDefaultIT {

    @BrowserTest
    public void capturedEventDataIsReportedUnderItsName() {
        open();

        findElement(By.id("dataButton")).click();

        waitUntil(driver -> "captured label"
                .equals(findElement(By.id("dataResult")).getText()));
    }

    @BrowserTest
    public void capturedFilterOnlyMatchesItsOwnListener() {
        open();

        findElement(By.id("filterButton")).click();

        // Only the listener whose capture matches the primary button fires
        waitUntil(driver -> "primary"
                .equals(findElement(By.id("filterResult")).getText()));
    }
}
