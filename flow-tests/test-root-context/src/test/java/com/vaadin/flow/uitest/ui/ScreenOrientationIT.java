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

public class ScreenOrientationIT extends ChromeBrowserTest {

    @Test
    public void initialState_isReportedFromBootstrap() {
        open();
        // The bootstrap parameters v-so/v-soa must seed the signal to a real
        // value before the view renders. Headless Chrome reports a portrait
        // orientation by default; the contract is only that the type is no
        // longer UNKNOWN.
        waitUntil(d -> {
            String t = findElement(By.id("type")).getText();
            return t.startsWith("PORTRAIT") || t.startsWith("LANDSCAPE");
        });
    }

    @Test
    public void orientationChange_isPropagatedToSignal() {
        open();

        // Fake a change event by overriding screen.orientation and dispatching
        // the change event the client listener subscribes to. Headless Chrome
        // does not actually rotate, so the values are spoofed.
        executeScript("""
                Object.defineProperty(screen.orientation, 'type', \
                {value: 'landscape-primary', configurable: true});
                Object.defineProperty(screen.orientation, 'angle', \
                {value: 90, configurable: true});
                screen.orientation.dispatchEvent(new Event('change'));
                """);

        waitUntil(d -> "LANDSCAPE_PRIMARY"
                .equals(findElement(By.id("type")).getText())
                && "90".equals(findElement(By.id("angle")).getText()));
    }
}
