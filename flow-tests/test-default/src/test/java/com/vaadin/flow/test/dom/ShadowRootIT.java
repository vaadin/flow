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

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeLabelElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(ShadowRootView.class)
public class ShadowRootIT extends AbstractDefaultIT {

    @BrowserTest
    public void checkShadowRoot() {
        open();

        DivElement div = $(DivElement.class).id("test-element");

        WebElement shadowDiv = div.$(DivElement.class).id("shadow-div");
        Assertions.assertEquals("Div inside shadow DOM", shadowDiv.getText());

        WebElement shadowLabel = div.$(NativeLabelElement.class)
                .id("shadow-label");
        Assertions.assertEquals("Label inside shadow DOM",
                shadowLabel.getText());

        findElement(By.id("remove")).click();

        assertTrue(findElements(By.id("shadow-label")).isEmpty(),
                "Child has not been removed from the shadow root");
    }
}
