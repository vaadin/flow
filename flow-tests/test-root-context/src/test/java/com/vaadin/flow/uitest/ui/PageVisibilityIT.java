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
    public void initialState_isVisible() {
        open();
        waitUntilStateIs("VISIBLE");
    }

    @Test
    public void blurAndFocus_transitionsThroughVisibleNotFocused() {
        open();
        waitUntilStateIs("VISIBLE");

        // Headless Chrome does not actually fire blur/focus when another tab
        // is opened, so dispatch synthetic window events to drive the
        // server-side signal. The client-side handler reads
        // document.hidden and document.hasFocus() in response to these
        // events, both of which keep their default values here, so the only
        // observable transition comes from the event itself.
        executeScript("window.dispatchEvent(new Event('blur'));");
        waitUntilStateIs("VISIBLE_NOT_FOCUSED");

        executeScript("window.dispatchEvent(new Event('focus'));");
        waitUntilStateIs("VISIBLE");
    }

    @Test
    public void hidden_isReportedAsHidden() {
        open();
        waitUntilStateIs("VISIBLE");

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

    private void waitUntilStateIs(String expected) {
        waitUntil(d -> expected.equals(findElement(By.id("state")).getText()));
    }
}
