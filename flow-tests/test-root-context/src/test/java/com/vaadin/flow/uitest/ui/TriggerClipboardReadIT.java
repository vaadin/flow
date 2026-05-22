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

public class TriggerClipboardReadIT extends ChromeBrowserTest {

    @Test
    public void clickReadsClipboard_andServerReceivesPayload() {
        open();
        installResolvingClipboardShim("clipped", "<b>clipped</b>");

        WebElement button = findElement(By.id("read"));
        WebElement status = findElement(By.id("status"));

        button.click();

        waitUntil(d -> status.getText() != null
                && status.getText().startsWith("text="));
        Assert.assertEquals("text=clipped;html=<b>clipped</b>",
                status.getText());
    }

    @Test
    public void clipboardReadRejection_propagatesAsError() {
        open();
        installRejectingClipboardShim();

        WebElement button = findElement(By.id("read"));
        WebElement status = findElement(By.id("status"));

        button.click();

        waitUntil(d -> "error=Error".equals(status.getText()));
    }

    // Replace navigator.clipboard.read with a Promise that resolves to a
    // single fake ClipboardItem exposing text/plain and text/html blobs.
    private void installResolvingClipboardShim(String text, String html) {
        String script = "const t = arguments[0]; const h = arguments[1];"
                + "const fake = {" + "  types: ['text/plain','text/html'],"
                + "  getType: mime => Promise.resolve({"
                + "    text: () => Promise.resolve(mime === 'text/plain' ? t : h)"
                + "  })" + "};"
                + "Object.defineProperty(navigator, 'clipboard', {"
                + "  configurable: true, value: {"
                + "    read: () => Promise.resolve([fake])" + "  }" + "});";
        ((JavascriptExecutor) getDriver()).executeScript(script, text, html);
    }

    private void installRejectingClipboardShim() {
        ((JavascriptExecutor) getDriver())
                .executeScript("Object.defineProperty(navigator, 'clipboard', {"
                        + "  configurable: true, value: {"
                        + "    read: () => Promise.reject(new Error('DeniedByTest'))"
                        + "  }" + "});");
    }
}
