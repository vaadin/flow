/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity;

import org.junit.After;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;

import com.vaadin.flow.component.login.testbench.LoginFormElement;
import com.vaadin.flow.component.login.testbench.LoginOverlayElement;
import com.vaadin.flow.spring.test.AbstractSpringTest;
import com.vaadin.testbench.TestBenchElement;
import org.openqa.selenium.interactions.Actions;

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
                "admin-only/secret.txt - Failed to load resource: the server responded with a status of 403")
                || msg.contains(
                        "admin-only/secret.txt?continue - Failed to load resource: the server responded with a status of 403"));
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

        LoginFormElement form = $(LoginOverlayElement.class).first()
                .getLoginForm();
        form.getUsernameField().setValue(username);
        form.getPasswordField().setValue(password);
        // Try to wait before pressing submit button
        new Actions(getDriver()).pause(1000).perform();
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
