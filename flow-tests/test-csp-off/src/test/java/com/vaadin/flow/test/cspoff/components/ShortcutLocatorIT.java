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
package com.vaadin.flow.test.cspoff.components;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.test.cspoff.AbstractCspOffIT;
import com.vaadin.flow.test.cspoff.TestFor;
import com.vaadin.testbench.BrowserTest;

import static com.vaadin.flow.test.cspoff.components.ShortcutLocatorView.LOCATED_INPUT_ID;
import static com.vaadin.flow.test.cspoff.components.ShortcutLocatorView.OUTSIDE_INPUT_ID;
import static com.vaadin.flow.test.cspoff.components.ShortcutLocatorView.RESULT_ID;

@TestFor(ShortcutLocatorView.class)
public class ShortcutLocatorIT extends AbstractCspOffIT {

    @BrowserTest
    public void stringLocator_keydownInLocatedElementTriggersShortcut() {
        open();

        // Outside both the listen-on component and the located element
        findElement(By.id(OUTSIDE_INPUT_ID)).sendKeys("x");
        getCommandExecutor().waitForVaadin();
        Assertions.assertEquals("0", findElement(By.id(RESULT_ID)).getText());

        findElement(By.id(LOCATED_INPUT_ID)).sendKeys("x");
        waitUntil(driver -> "1"
                .equals(driver.findElement(By.id(RESULT_ID)).getText()));
    }
}
