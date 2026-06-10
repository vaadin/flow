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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerOpenInNewTabIT extends ChromeBrowserTest {

    @Test
    public void clickUrl_callsWindowOpenWithBlankTargetAndDefaultFeatures() {
        open();
        installRecordingShim();

        findElement(By.id("open-tab")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("https://example.com/docs", recorded.get("url"));
        Assert.assertEquals("_blank", recorded.get("target"));
        Assert.assertEquals("noopener,noreferrer", recorded.get("features"));
    }

    @Test
    public void clickUrlWithFeatures_passesCustomFeaturesVerbatim() {
        open();
        installRecordingShim();

        findElement(By.id("open-tab-features")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("https://example.com/help", recorded.get("url"));
        Assert.assertEquals("_blank", recorded.get("target"));
        // Custom features replace the defaults; the action passes whatever
        // the caller gave through to window.open verbatim.
        Assert.assertEquals(TriggerOpenInNewTabView.CUSTOM_FEATURES,
                recorded.get("features"));
    }

    @Test
    public void clickInput_resolvesUrlFromInputValueAtFireTime() {
        open();
        installRecordingShim();

        WebElement field = findElement(By.id("url-source"));
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'https://example.com/from-input';",
                field);

        findElement(By.id("open-tab-input")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals("https://example.com/from-input",
                recorded.get("url"));
        Assert.assertEquals("_blank", recorded.get("target"));
    }

    @Test
    public void clickJavascriptInput_blocksWindowOpen() {
        open();
        installRecordingShim();

        WebElement field = findElement(By.id("url-source"));
        // Plain javascript: URL — the simplest variant. Leading
        // whitespace/case variants are covered by the unit test; here we
        // only need to prove the client guard prevents window.open from
        // being called at all.
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'javascript:window.__pwned = true';",
                field);

        findElement(By.id("open-tab-js-input")).click();

        // Click a safe URL afterwards so we have a positive signal that the
        // recording shim works end-to-end — without this we couldn't tell
        // "blocked correctly" from "click did nothing at all".
        ((JavascriptExecutor) getDriver()).executeScript(
                "arguments[0].value = 'https://example.com/after-block';",
                field);
        findElement(By.id("open-tab-input")).click();

        Map<String, Object> recorded = waitForFirstOpen();
        Assert.assertEquals(
                "javascript: URL must not reach window.open — should be"
                        + " skipped, and only the subsequent safe click should"
                        + " be recorded",
                "https://example.com/after-block", recorded.get("url"));
        // Make sure the inline payload never ran in the page either.
        Object pwned = ((JavascriptExecutor) getDriver())
                .executeScript("return window.__pwned;");
        Assert.assertNull("javascript: URL must not execute in the opener",
                pwned);
    }

    // Replace window.open with a recorder so the IT does not depend on Chrome
    // actually opening a popup (headless Chrome's popup behaviour is fragile)
    // and so the popup blocker is irrelevant. Each invocation pushes
    // {url, target, features} onto window.__opens; the shim returns a stub
    // object so callers checking the return value for null don't blow up.
    private void installRecordingShim() {
        ((JavascriptExecutor) getDriver()).executeScript("window.__opens = [];"
                + "window.open = (url, target, features) => {"
                + "  window.__opens.push("
                + "    {url: url, target: target, features: features});"
                + "  return {closed: false};" + "};");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> waitForFirstOpen() {
        return (Map<String, Object>) waitUntil(
                d -> ((JavascriptExecutor) d).executeScript(
                        "return (window.__opens && window.__opens.length)"
                                + " ? window.__opens[0] : null;"));
    }
}
