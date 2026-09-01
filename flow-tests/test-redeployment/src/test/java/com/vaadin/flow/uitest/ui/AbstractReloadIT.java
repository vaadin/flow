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

import org.junit.Before;
import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractReloadIT extends ChromeBrowserTest {

    // Tests in this module routinely recompile classes, which causes Spring
    // Boot DevTools to restart Tomcat. A previous test's restart can still be
    // in progress when the next test starts, so wait until the server is
    // reachable instead of failing on the first refused connection.
    @Before
    @Override
    public void checkIfServerAvailable() {
        waitUntil(driver -> {
            try {
                super.checkIfServerAvailable();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    protected void reloadAndWait() {
        String viewId = getViewId();
        $("*").id(SessionValueView.TRIGGER_RELOAD_ID).click();
        waitUntil(driver -> {
            try {
                return !getViewId().equals(viewId);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    protected String getViewId() {
        return $("*").id("viewId").getText();
    }
}
