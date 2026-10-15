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
package com.vaadin.flow.webcomponent;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ElementSizeIT extends ChromeBrowserTest implements HasById {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/elementSize.html";
    }

    @Test
    public void sizeSignal_reportsSizeOfEmbeddedElement() {
        open();

        waitForElementVisible(By.id("element-size"));
        TestBenchElement size = byId("element-size", "size");
        waitUntil(driver -> (ElementSizeComponent.INITIAL_WIDTH + "x"
                + ElementSizeComponent.INITIAL_HEIGHT).equals(size.getText()));

        TestBenchElement panel = byId("element-size", "panel");
        executeScript("arguments[0].style.width = '200px'", panel);
        waitUntil(driver -> ("200x" + ElementSizeComponent.INITIAL_HEIGHT)
                .equals(size.getText()));
    }
}
