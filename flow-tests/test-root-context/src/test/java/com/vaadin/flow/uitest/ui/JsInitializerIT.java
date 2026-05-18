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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class JsInitializerIT extends ChromeBrowserTest {

    @Test
    public void registerRunsOnInitialAttach() {
        open();
        assertCounters(0, 0);
        clickAndWait("register");
        assertCounters(1, 0);
    }

    @Test
    public void sameRequestReattach_doesNotRerun() {
        open();
        clickAndWait("register");
        assertCounters(1, 0);

        clickAndWait("sameRequestReattach");
        // The client never discarded its DOM, so neither init nor cleanup
        // fired.
        assertCounters(1, 0);
    }

    @Test
    public void crossRequestReattach_cleansUpAndRerunsInit() {
        open();
        clickAndWait("register");
        assertCounters(1, 0);

        clickAndWait("detach");
        // Cleanup fires because the client unregistered the DOM.
        assertCounters(1, 1);

        clickAndWait("reattach");
        // Init runs again on the new client-side DOM.
        assertCounters(2, 1);
    }

    @Test
    public void removeRegistration_invokesCleanupAndStopsReinstall() {
        open();
        clickAndWait("register");
        assertCounters(1, 0);

        clickAndWait("removeRegistration");
        assertCounters(1, 1);

        // Detach + reattach must NOT re-install the initializer.
        clickAndWait("detach");
        clickAndWait("reattach");
        assertCounters(1, 1);
    }

    private void clickAndWait(String id) {
        findElement(By.id(id)).click();
    }

    private void assertCounters(int expectedInits, int expectedCleanups) {
        waitUntil(
                d -> textOf("initCounter").equals(String.valueOf(expectedInits))
                        && textOf("cleanupCounter")
                                .equals(String.valueOf(expectedCleanups)),
                5);
        Assert.assertEquals(String.valueOf(expectedInits),
                textOf("initCounter"));
        Assert.assertEquals(String.valueOf(expectedCleanups),
                textOf("cleanupCounter"));
    }

    private String textOf(String id) {
        return findElement(By.id(id)).getText();
    }
}
