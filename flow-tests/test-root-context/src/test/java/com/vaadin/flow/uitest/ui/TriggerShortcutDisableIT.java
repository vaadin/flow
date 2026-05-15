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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerShortcutDisableIT extends ChromeBrowserTest {

    @Test
    public void enterShortcutClicksAndDisablesSubmitButton() {
        open();

        WebElement field = findElement(By.id("field"));
        WebElement submit = findElement(By.id("submit"));

        field.click();
        field.sendKeys("hello", Keys.ENTER);

        // The ClickAction fires first while the button is still enabled,
        // so Flow's server-side ClickListener runs and observes
        // isEnabled() == true.
        WebElement result = waitUntil(d -> {
            WebElement r = d.findElement(By.id("result"));
            return "clicked, enabled=true".equals(r.getText()) ? r : null;
        });
        Assert.assertEquals("clicked, enabled=true", result.getText());

        // SetEnabledAction then disables the button locally. The browser
        // would block any subsequent user-initiated click, closing the
        // latency window in which a second submit could otherwise happen.
        Assert.assertNotNull("submit button is disabled client-side",
                submit.getAttribute("disabled"));
    }
}
