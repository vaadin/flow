/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Test that application is deployed and can be opened in the browser. Works
 * also when starting server with exec-maven-plugin. Waits for server to start.
 */
public class SmokeTestIT extends AbstractSpringTest {

    @Override
    @Before
    public void checkIfServerAvailable() {
        try {
            waitViewUrl(60);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void componentsAreFoundAndLoaded() throws Exception {
        open();

        $(ButtonElement.class).waitForFirst();

        $(ButtonElement.class).first().click();

        Assert.assertTrue(
                "Clicking button should have opened a notification successfully.",
                $(NotificationElement.class).exists());
    }

    @Override
    protected String getTestPath() {
        return "/component-test";
    }

    private void waitViewUrl(int count)
            throws MalformedURLException, InterruptedException {
        String viewUrl = getContextRootURL() + getTestPath();
        if (count == 0) {
            throw new IllegalStateException(
                    "URL '" + viewUrl + "' is not available");
        }
        URL url = new URL(viewUrl);
        try {
            url.openConnection().connect();
        } catch (IOException exception) {
            Thread.sleep(1000);
            waitViewUrl(count - 1);
        }
    }
}
