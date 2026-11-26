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
package com.vaadin.flow.test.cspoff.dom;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.test.cspoff.AbstractCspOffIT;
import com.vaadin.flow.test.cspoff.TestFor;
import com.vaadin.testbench.BrowserTest;

import static com.vaadin.flow.test.cspoff.dom.EventDataExpressionView.BUTTON_ID;
import static com.vaadin.flow.test.cspoff.dom.EventDataExpressionView.ELEMENT_ID;
import static com.vaadin.flow.test.cspoff.dom.EventDataExpressionView.VALUE_ID;

@TestFor(EventDataExpressionView.class)
public class EventDataExpressionIT extends AbstractCspOffIT {

    @BrowserTest
    public void expressionEventData_sentWithTheEvent() {
        open();

        findElement(By.id(BUTTON_ID)).click();

        waitUntil(driver -> !driver.findElement(By.id(VALUE_ID)).getText()
                .isEmpty());
        Assertions.assertEquals(BUTTON_ID + "-clicked",
                findElement(By.id(VALUE_ID)).getText());
    }

    @BrowserTest
    public void expressionEventDataElement_mappedToServerElement() {
        open();

        findElement(By.id(BUTTON_ID)).click();

        waitUntil(driver -> !driver.findElement(By.id(ELEMENT_ID)).getText()
                .isEmpty());
        Assertions.assertEquals(BUTTON_ID,
                findElement(By.id(ELEMENT_ID)).getText());
    }
}
