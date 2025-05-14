/*
 * Copyright 2000-2025 Vaadin Ltd.
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
