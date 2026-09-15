/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi.itest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.Browser;

@RunWith(Arquillian.class)
@RunAsClient
abstract public class AbstractCdiTest extends ChromeBrowserTest {

    /**
     * The iframe the Flow client renders in place of the view when it gives up
     * on initializing the UI. Its source resolves to no route in an application
     * without {@code @PWA}, so the frame ends up showing the router's "Couldn't
     * find route" page.
     */
    private static final By OFFLINE_STUB = By
            .cssSelector("iframe[src='./offline-stub.html']");

    /**
     * Counter key asked for to find out whether the deployment answers. It is
     * never asserted on, so any name that no test uses will do.
     */
    private static final String DEPLOYMENT_PROBE = "deployment-probe";

    private static final long DEPLOYMENT_TIMEOUT_SECONDS = 120;

    private static final long ELEMENT_TIMEOUT_SECONDS = 15;

    private static final long OFFLINE_RECOVERY_TIMEOUT_SECONDS = 90;

    @ArquillianResource
    protected URL deploymentUrl;

    @Override
    protected String getRootURL() {
        return super.getRootURL() + deploymentUrl.getPath();
    }

    @Override
    protected int getDeploymentPort() {
        return deploymentUrl.getPort();
    }

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Override
    public void setup() throws Exception {
        setDesiredCapabilities(Browser.CHROME.getDesiredCapabilities());
        super.setup();
        waitForDeployment();
    }

    /**
     * Waits until the deployment answers application requests.
     * <p>
     * Arquillian returns from the deploy operation once the container reports
     * the archive deployed, which on a loaded runner can be before the
     * application serves anything. The first test to navigate then gets a
     * failed request, and since the Flow client turns a failed UI
     * initialization into the offline stub, the test fails looking for an
     * element of a view that was never rendered.
     * <p>
     * The counter filter is the cheapest thing to ask for: it answers from the
     * filter chain, so it needs the archive deployed and CDI running, and it
     * creates neither a session nor a UI.
     */
    private void waitForDeployment() throws InterruptedException {
        long deadline = System.currentTimeMillis()
                + TimeUnit.SECONDS.toMillis(DEPLOYMENT_TIMEOUT_SECONDS);
        Exception lastFailure;
        do {
            try {
                // Parsed, not just read: a container that is up but has not
                // installed the application yet answers the request with an
                // error page rather than refusing it.
                Integer.parseInt(slurp("?getCount=" + DEPLOYMENT_PROBE).trim());
                return;
            } catch (IOException | RuntimeException failure) {
                lastFailure = failure;
            }
            Thread.sleep(500);
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError(String.format(
                "The deployment at %s did not answer within %d seconds.",
                getRootURL(), DEPLOYMENT_TIMEOUT_SECONDS), lastFailure);
    }

    protected void click(String elementId) {
        awaitElement(By.id(elementId)).click();
    }

    protected void follow(String linkText) {
        awaitElement(By.linkText(linkText)).click();
    }

    protected String getText(String id) {
        return awaitElement(By.id(id)).getText();
    }

    /**
     * Waits for the element to be present and returns it.
     * <p>
     * While the Flow client is showing the offline stub the page is loaded
     * again, because that is the only thing that retries the UI initialization:
     * the connection-lost state the client set is cleared by a browser
     * {@code online} event, which never fires when the browser was online the
     * whole time and only the server failed to answer. Nothing is lost by
     * reloading, since the stub means the view never rendered.
     */
    private WebElement awaitElement(By by) {
        long deadline = System.currentTimeMillis()
                + TimeUnit.SECONDS.toMillis(OFFLINE_RECOVERY_TIMEOUT_SECONDS);
        while (true) {
            try {
                waitUntil(ExpectedConditions.presenceOfElementLocated(by),
                        ELEMENT_TIMEOUT_SECONDS);
                return findElement(by);
            } catch (TimeoutException timeout) {
                boolean offlineStub = !findElements(OFFLINE_STUB).isEmpty();
                if (!offlineStub || System.currentTimeMillis() >= deadline) {
                    throw new AssertionError(describeMissing(by, offlineStub),
                            timeout);
                }
                getDriver().navigate().refresh();
            }
        }
    }

    /**
     * Describes an element that never appeared, so that the failure names the
     * offline stub when that is what the browser is showing and carries the
     * browser console along. Neither is visible in the test output otherwise,
     * which leaves a bare "no such element" as the only evidence.
     */
    private String describeMissing(By by, boolean offlineStub) {
        StringBuilder message = new StringBuilder(
                String.format("%s did not appear within %d seconds.", by,
                        ELEMENT_TIMEOUT_SECONDS));
        if (offlineStub) {
            message.append(" The Flow client is showing its offline stub,"
                    + " so the UI initialization request did not return the"
                    + " expected JSON response.");
        }
        message.append("\nCurrent url: ").append(getDriver().getCurrentUrl());
        message.append("\nBrowser console: ");
        try {
            List<LogEntry> entries = getDriver().manage().logs()
                    .get(LogType.BROWSER).getAll();
            if (entries.isEmpty()) {
                message.append("(empty)");
            }
            entries.forEach(entry -> message.append("\n  ").append(entry));
        } catch (RuntimeException unavailable) {
            // Reading the log must not replace the failure being reported.
            message.append("unavailable (").append(unavailable).append(')');
        }
        return message.toString();
    }

    protected void assertCountEquals(int expectedCount, String counter)
            throws IOException {
        Assert.assertEquals(expectedCount, getCount(counter));
    }

    protected void waitForCount(int expectedCount, String counter) {
        waitUntil(driver -> {
            try {
                return getCount(counter) == expectedCount;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, 10);
    }

    protected void assertTextEquals(String expectedText, String elementId) {
        Assert.assertEquals(expectedText, getText(elementId));
    }

    protected void resetCounts() throws IOException {
        slurp("?resetCounts");
    }

    protected int getCount(String id) throws IOException {
        getCommandExecutor().waitForVaadin();
        String line = slurp("?getCount=" + id);
        return Integer.parseInt(line);
    }

    private String slurp(String uri) throws IOException {
        URL url = new URL(getRootURL() + uri);
        InputStream is = url.openConnection().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        reader.close();
        return line;
    }
}
