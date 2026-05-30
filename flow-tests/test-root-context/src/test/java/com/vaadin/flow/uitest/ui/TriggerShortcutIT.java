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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerShortcutIT extends ChromeBrowserTest {

    @Test
    public void modifierMatchIsExact() {
        open();

        WebElement field = findElement(By.id("source"));
        WebElement status = findElement(By.id("status"));
        field.click();

        // Ctrl+S — fires the Ctrl+S binding (appends "S").
        pressChord(field, Keys.CONTROL, "s");
        waitUntil(d -> "S".equals(status.getText()));

        // Ctrl+Shift+S — only the Ctrl+Shift+S binding (appends "SS") must
        // fire. If the Ctrl+S binding leaked, the status would become "SSS"
        // (both append on the same keystroke); the assertion below catches
        // that.
        pressChord(field, Keys.CONTROL, Keys.SHIFT, "s");
        waitUntil(d -> "SSS".equals(status.getText())
                || "SSSS".equals(status.getText()));
        // Re-read once stable. Only the Ctrl+Shift+S handler should have
        // appended, so the result must be "SSS" (previous "S" + new "SS"),
        // not "SSSS" (which would mean both shortcuts fired).
        org.junit.Assert.assertEquals("SSS", status.getText());
    }

    private void pressChord(WebElement target, CharSequence... keys) {
        Actions actions = new Actions(driver);
        actions.click(target);
        // Hold every Keys modifier, then type the trailing string, then
        // release the modifiers in reverse order. Mirrors the pattern used
        // by the existing ShortcutsIT.
        for (CharSequence k : keys) {
            if (k instanceof Keys) {
                actions.keyDown(k);
            } else {
                actions.sendKeys(k);
            }
        }
        for (int i = keys.length - 1; i >= 0; i--) {
            if (keys[i] instanceof Keys) {
                actions.keyUp(keys[i]);
            }
        }
        actions.build().perform();
    }
}
