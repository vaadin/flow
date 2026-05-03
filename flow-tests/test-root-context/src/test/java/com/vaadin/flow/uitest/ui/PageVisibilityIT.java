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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PageVisibilityIT extends ChromeBrowserTest {

    @Test
    public void initialState_isReportedFromBootstrap() {
        open();
        // The bootstrap parameter v-pv must seed the signal to a real value
        // before the view renders. In headless CI the tab has no OS focus,
        // so VISIBLE_NOT_FOCUSED is just as valid as VISIBLE — the
        // contract is only that the signal is no longer UNKNOWN.
        waitUntil(d -> {
            String s = findElement(By.id("state")).getText();
            return "VISIBLE".equals(s) || "VISIBLE_NOT_FOCUSED".equals(s);
        });
    }

    @Test
    public void blurAndFocus_transitionsThroughVisibleNotFocused() {
        open();
        forceVisibleBaseline();

        // Headless Chrome does not actually fire blur/focus when another
        // tab is opened, so dispatch synthetic window events to drive the
        // server-side signal. The client-side handler reads document.hidden
        // and document.hasFocus() in response to these events, both of
        // which keep their default values here, so the only observable
        // transition comes from the event itself.
        executeScript("window.dispatchEvent(new Event('blur'));");
        waitUntilStateIs("VISIBLE_NOT_FOCUSED");

        executeScript("window.dispatchEvent(new Event('focus'));");
        waitUntilStateIs("VISIBLE");
    }

    @Test
    public void hidden_isReportedAsHidden() {
        open();
        forceVisibleBaseline();

        // The client-side handler reads document.hidden inside its
        // visibilitychange listener, so spoof the property before
        // dispatching the event.
        executeScript("Object.defineProperty(document, 'hidden', "
                + "{value: true, configurable: true});"
                + "document.dispatchEvent(new Event('visibilitychange'));");
        waitUntilStateIs("HIDDEN");

        executeScript("Object.defineProperty(document, 'hidden', "
                + "{value: false, configurable: true});"
                + "document.dispatchEvent(new Event('visibilitychange'));");
        waitUntilStateIs("VISIBLE");
    }

    /**
     * Drives the signal to VISIBLE so the rest of the test starts from a known
     * baseline regardless of whether the headless browser reports the tab as
     * focused.
     */
    private void forceVisibleBaseline() {
        executeScript("window.dispatchEvent(new Event('focus'));");
        waitUntilStateIs("VISIBLE");
    }

    private void waitUntilStateIs(String expected) {
        waitUntil(d -> expected.equals(findElement(By.id("state")).getText()));
    }
}
