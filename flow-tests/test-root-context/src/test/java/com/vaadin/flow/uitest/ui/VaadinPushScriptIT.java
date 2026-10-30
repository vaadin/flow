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
import org.junit.experimental.categories.Category;

import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class VaadinPushScriptIT extends ChromeBrowserTest {

    @Test
    public void pushScriptURL_urlMapping_fromJavascriptBootstrapHandler() {
        // Push script url set by server side bootstrap handler
        getDriver().get(
                getRootURL() + "/view/" + PushSettingsView.class.getName());
        waitForDevServer();
        assertThatPushScriptUrlIsRelativeToUrlMapping();
    }

    @Test
    public void pushScriptURL_urlMapping_fromClientSide() {
        // Push script URL computed by client side (AtmospherePushConnection)
        // as consequence of server side push configuration change
        getDriver().get(
                getRootURL() + "/view/" + ActivatePushView.class.getName());
        waitForDevServer();

        assertThatPushScriptUrlIsRelativeToUrlMapping();
    }

    private void assertThatPushScriptUrlIsRelativeToUrlMapping() {
        // vaadinPush.js is imported as a module, so it shows up as a loaded
        // resource rather than as a script element
        String script = """
                return performance.getEntriesByType('resource')
                    .map(entry => entry.name)
                    .find(name => name.includes(arguments[0])) || null;
                """;
        waitUntil(driver -> executeScript(script,
                ApplicationConstants.VAADIN_PUSH_DEBUG_JS) != null);
        String pushScriptUrl = (String) executeScript(script,
                ApplicationConstants.VAADIN_PUSH_DEBUG_JS);

        Assert.assertTrue("Push script not relative to Vaadin servlet mapping",
                pushScriptUrl.contains(
                        "/view/" + ApplicationConstants.VAADIN_PUSH_DEBUG_JS));
    }

}
