/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.ccdmtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchDriverProxy;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Ignore;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.Response;

@Ignore
public class CCDMTest extends ChromeBrowserTest {

    protected final void openTestUrl(String url) {
        getDriver().get(getRootURL() + "/foo" + url);
        waitForDevServer();
    }

    protected final void openVaadinRouter() {
        openVaadinRouter("/");
    }

    protected final void openVaadinRouter(String url) {
        openTestUrl(url);

        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));
    }

    protected ChromeDriver getChromeDriver() {
        return (ChromeDriver) ((TestBenchDriverProxy) getDriver())
                .getWrappedDriver();
    }

    protected Response chromeDriverExecute(String name, Map<String, ?> parameters) throws IOException {
        CommandExecutor executor = getChromeDriver().getCommandExecutor();
        Response response = executor.execute(new Command(getChromeDriver().getSessionId(), name, parameters));
        if (response.getStatus() != 0) {
            throw new RuntimeException("Unable to execute ChromeDriver command: " + name);
        }
        return response;
    }

    protected Response chromeDriverExecute(String name) throws IOException {
        return chromeDriverExecute(name, null);
    }

    protected void setSimulateOffline(boolean offline) {
        try {
            if (offline) {
                final Map<String, Object> conditions = new HashMap<>();
                conditions.put("offline", true);
                conditions.put("latency", 0);
                conditions.put("throughput", 0);

                final ImmutableMap<String, Object> params = ImmutableMap.of("network_conditions", ImmutableMap.copyOf(conditions));

                chromeDriverExecute("setNetworkConditions", params);

                Assert.assertEquals("navigator.onLine should be false", false,
                        executeScript("return navigator.onLine"));
            } else {
                chromeDriverExecute("deleteNetworkConditions");

                Assert.assertEquals("navigator.onLine should be true", true,
                        executeScript("return navigator.onLine"));
            }
        } catch (IOException e) {
            Assert.fail("ChromeDriver IOException in setSimulateOffline()");
        }
    }

    protected final WebElement findAnchor(String href) {
        final List<WebElement> anchors = findElements(By.tagName("a"));
        for (WebElement element : anchors) {
            if (element.getAttribute("href").endsWith(href)) {
                return element;
            }
        }

        return null;
    }

    protected final void assertView(String viewId, String assertViewText, String assertViewRoute) {
        waitForElementPresent(By.id(viewId));
        final WebElement serverViewDiv = findElement(By.id(viewId));

        Assert.assertEquals(assertViewText, serverViewDiv.getText());
        assertCurrentRoute(assertViewRoute);
    }

    protected final void assertCurrentRoute(String route) {
        final String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(String.format("Expecting route '%s', but url is '%s'",
                route, currentUrl), currentUrl.endsWith(route));
    }

}
