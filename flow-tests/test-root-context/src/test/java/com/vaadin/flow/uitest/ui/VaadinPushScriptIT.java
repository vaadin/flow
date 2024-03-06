/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.shared.ApplicationConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
        assertThatPushScriptUrlIsRelativeToUrlMapping(
                findElement(By.tagName("body")));
    }

    @Test
    public void pushScriptURL_urlMapping_fromClientSide() {
        // Push script URL computed by client side (AtmospherePushConnection)
        // as consequence of server side push configuration change
        getDriver().get(
                getRootURL() + "/view/" + ActivatePushView.class.getName());
        waitForDevServer();

        assertThatPushScriptUrlIsRelativeToUrlMapping(
                findElement(By.tagName("head")));
    }

    private void assertThatPushScriptUrlIsRelativeToUrlMapping(
            WebElement scriptContainer) {
        String pushScriptUrl = scriptContainer
                .findElements(By.tagName("script")).stream()
                .map(script -> script.getAttribute("src"))
                .filter(scriptUrl -> scriptUrl != null && scriptUrl
                        .contains(ApplicationConstants.VAADIN_PUSH_DEBUG_JS))
                .findFirst().orElse(null);

        Assert.assertNotNull(ApplicationConstants.VAADIN_PUSH_DEBUG_JS
                + " script not loaded by page", pushScriptUrl);
        Assert.assertTrue("Push script not relative to Vaadin servlet mapping",
                pushScriptUrl.contains(
                        "/view/" + ApplicationConstants.VAADIN_PUSH_DEBUG_JS));
    }

}
