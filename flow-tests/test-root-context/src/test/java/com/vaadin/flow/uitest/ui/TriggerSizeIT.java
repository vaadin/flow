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
package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerSizeIT extends ChromeBrowserTest {

    @Test
    public void initialObservation_andResize_propagateToDisplayDivs() {
        open();

        WebElement widthDiv = findElement(By.id("width"));
        WebElement heightDiv = findElement(By.id("height"));

        // ResizeObserver fires once when observe() is first called, so the
        // SetPropertyAction wired to width()/height() populates the display
        // divs without any user interaction.
        waitUntil(d -> String.valueOf(TriggerSizeView.INITIAL_WIDTH)
                .equals(widthDiv.getText()));
        waitUntil(d -> String.valueOf(TriggerSizeView.INITIAL_HEIGHT)
                .equals(heightDiv.getText()));

        // Resizing the panel via inline styles triggers a fresh ResizeObserver
        // entry, which the trigger forwards through SetPropertyAction to
        // update both textContent properties.
        ((JavascriptExecutor) getDriver())
                .executeScript("const p = document.getElementById('panel');"
                        + "p.style.width = '240px';"
                        + "p.style.height = '90px';");
        waitUntil(d -> "240".equals(widthDiv.getText()));
        waitUntil(d -> "90".equals(heightDiv.getText()));
    }
}
