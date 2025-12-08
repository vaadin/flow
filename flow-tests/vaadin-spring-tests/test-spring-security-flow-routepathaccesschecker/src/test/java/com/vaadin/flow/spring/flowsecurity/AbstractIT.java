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
package com.vaadin.flow.spring.flowsecurity;

import org.junit.After;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;

import com.vaadin.flow.component.login.testbench.LoginOverlayElement;
import com.vaadin.flow.spring.test.AbstractSpringTest;
import com.vaadin.testbench.TestBenchElement;

public abstract class AbstractIT extends AbstractSpringTest {

    private static final String ROOT_PAGE_HEADER_TEXT = "Welcome to the Java Bank of Vaadin";
    private static final String ANOTHER_PUBLIC_PAGE_HEADER_TEXT = "Another public view for testing";
    private static final int SERVER_PORT = 8888;

    @Override
    protected int getDeploymentPort() {
        return SERVER_PORT;
    }

    @After
    public void tearDown() {
        if (getDriver() != null) {
            checkForBrowserErrors();
        }
    }

    private void checkForBrowserErrors() {
        checkLogsForErrors(msg -> msg.contains(
                "restricted/secret.txt?continue - Failed to load resource: the server responded with a status of 403")
                || msg.contains(
                        "admin-only/secret.txt - Failed to load resource: the server responded with a status of 403")
                || msg.contains(
                        "admin-only/secret.txt?continue - Failed to load resource: the server responded with a status of 403")
                || (msg.contains("X-Atmosphere-Transport=close")
                        && msg.contains(
                                "Failed to load resource: the server responded with a status of 403")));
    }

    /**
     * Base path for Vaadin Servlet URL mapping, as defined in
     * {@literal vaadin.urlMapping} configuration property.
     *
     * For example, for {@code vaadin.urlMapping=/vaadin/*} return value should
     * be {@code /vaadin}, without ending slash.
     *
     * Default value is {@literal blank}, relative to the default {@code /*}
     * mapping.
     *
     * @return base path for Vaadin Servlet URL mapping.
     */
    protected String getUrlMappingBasePath() {
        return "";
    }

    protected void open(String path) {
        open(path, getDriver());
    }

    protected void open(String path, WebDriver driver) {
        driver.get(getRootURL() + getUrlMappingBasePath() + "/" + path);
    }

    protected void openResource(String path) {
        driver.get(getRootURL() + "/" + path);
    }

    protected void loginUser() {
        login("john", "john");
    }

    protected void loginAdmin() {
        login("emma", "emma");
    }

    protected void login(String username, String password) {
        assertLoginViewShown();

        LoginOverlayElement form = $(LoginOverlayElement.class).first();
        form.getUsernameField().setValue(username);
        form.getPasswordField().setValue(password);
        form.submit();
        waitUntilNot(
                driver -> driver.getCurrentUrl().contains("my/login/page"));
        waitUntilNot(driver -> $(LoginOverlayElement.class).exists());
    }

    protected void assertLoginViewShown() {
        assertPathShown("my/login/page");
        waitUntil(driver -> $(LoginOverlayElement.class).exists());
    }

    protected void assertRootPageShown() {
        waitForClientRouter();
        waitUntil(drive -> $("h1").attribute("id", "header").exists());
        String headerText = $("h1").id("header").getText();
        Assert.assertEquals(ROOT_PAGE_HEADER_TEXT, headerText);
    }

    protected void assertAnotherPublicPageShown() {
        waitForClientRouter();
        waitUntil(drive -> $("h1").attribute("id", "header").exists());
        String headerText = $("h1").id("header").getText();
        Assert.assertEquals(ANOTHER_PUBLIC_PAGE_HEADER_TEXT, headerText);
    }

    protected void assertPrivatePageShown(String fullName) {
        assertPathShown("private");
        waitUntil(driver -> $("span").attribute("id", "balanceText").exists());
        String balance = $("span").id("balanceText").getText();
        Assert.assertTrue(balance.startsWith(
                "Hello " + fullName + ", your bank account balance is $"));
    }

    protected void assertAdminPageShown(String fullName) {
        assertPathShown("admin");
        TestBenchElement welcome = waitUntil(driver -> $("*").id("welcome"));
        String welcomeText = welcome.getText();
        Assert.assertEquals("Welcome to the admin page, " + fullName,
                welcomeText);
    }

    protected void assertPathShown(String path) {
        waitForClientRouter();
        waitUntil(driver -> {
            String url = driver.getCurrentUrl();
            if (!url.startsWith(getRootURL())) {
                throw new IllegalStateException("URL should start with "
                        + getRootURL() + " but is " + url);
            }
            // HttpSessionRequestCache uses request parameter "continue",
            // see HttpSessionRequestCache::setMatchingRequestParameterName
            if (url.endsWith("continue")) {
                url = url.substring(0, url.length() - 9);
            }
            return url.equals(
                    getRootURL() + getUrlMappingBasePath() + "/" + path);
        });
    }

    protected void assertResourceShown(String path) {
        waitUntil(driver -> {
            // HttpSessionRequestCache uses request parameter "continue",
            // see HttpSessionRequestCache::setMatchingRequestParameterName
            String url = driver.getCurrentUrl();
            if (url.endsWith("continue")) {
                url = url.substring(0, url.length() - 9);
            }
            return url.equals(getRootURL() + "/" + path);
        });
    }

}
