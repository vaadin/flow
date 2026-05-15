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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerSignalOutputIT extends ChromeBrowserTest {

    @Test
    public void clickCopiesSignalValue_andReflectsServerSideUpdates() {
        open();

        // Stub navigator.clipboard.writeText so the assertion is
        // independent of clipboard permissions.
        ((JavascriptExecutor) getDriver())
                .executeScript("window.__copied = null;"
                        + "Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    writeText: t => { window.__copied = t; return Promise.resolve(); }"
                        + "  }" + "});");

        WebElement copy = findElement(By.id("copy"));
        WebElement update = findElement(By.id("update"));

        copy.click();
        Object first = waitUntil(d -> {
            Object v = ((JavascriptExecutor) d)
                    .executeScript("return window.__copied;");
            return "first".equals(v) ? v : null;
        });
        Assert.assertEquals("first", first);

        // Update the signal server-side. The trigger snapshot must
        // re-emit so the next click copies the new value.
        update.click();

        copy.click();
        Object second = waitUntil(d -> {
            Object v = ((JavascriptExecutor) d)
                    .executeScript("return window.__copied;");
            return "second".equals(v) ? v : null;
        });
        Assert.assertEquals("second", second);
    }
}
