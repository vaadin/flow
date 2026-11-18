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
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class VaadinPushScriptIT extends ChromeBrowserTest {

    @Test
    public void pushScriptURL_urlMapping_fromJavascriptBootstrapHandler() {
        // Push script url set by server side bootstrap handler. Only the
        // bootstrap adds the version parameter: the dev tools import
        // vaadinPush.js too, but without it.
        getDriver().get(
                getRootURL() + "/view/" + PushSettingsView.class.getName());
        waitForDevServer();

        String expectedUrl = "/view/"
                + ApplicationConstants.VAADIN_PUSH_DEBUG_JS + "?v=";
        String script = """
                return performance.getEntriesByType('resource')
                    .map(entry => entry.name)
                    .find(name => name.includes(arguments[0])) || null;
                """;
        waitUntil(driver -> executeScript(script, expectedUrl) != null);
    }

    @Test
    public void pushScriptURL_urlMapping_fromClientSide() {
        // Push script URL computed by client side (AtmospherePushConnection)
        // as consequence of server side push configuration change. The view
        // pushes a marker once the connection is up, which needs the script to
        // have loaded from the right url.
        getDriver().get(
                getRootURL() + "/view/" + ActivatePushView.class.getName());
        waitForDevServer();

        waitForElementPresent(By.id("pushed"));
    }

}
