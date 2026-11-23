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
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertNull;

@TestFor(InvisibleSlotAttributeView.class)
public class InvisibleSlotAttributeIT extends AbstractDefaultIT {

    @BrowserTest
    public void initiallyInvisibleElement_slotAttributeImmediatelyPropagated() {
        open();

        // Find the element by the "slot" attribute, since "id" property
        // is not bound for invisible elements
        WebElement target = findElement(By.cssSelector("[slot='drawer']"));

        // Element is hidden but "slot" attribute should be present
        Assertions.assertEquals(Boolean.TRUE.toString(),
                target.getAttribute("hidden"));
        Assertions.assertEquals("drawer", target.getAttribute("slot"));
        // Non-structural attributes must NOT be sent for invisible elements
        assertNull(target.getAttribute("data-info"));

        $(NativeButtonElement.class).id("show-button").click();

        // After becoming visible, the element is re-bound and gets its id
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id("target")));
        WebElement visibleTarget = findElement(By.id("target"));

        // "hidden" should be gone and "slot" should still be present
        assertNull(visibleTarget.getAttribute("hidden"));
        Assertions.assertEquals("drawer", visibleTarget.getAttribute("slot"));

        // All attributes are sent after the element becomes visible
        Assertions.assertEquals("sensitive",
                visibleTarget.getAttribute("data-info"));
    }
}
