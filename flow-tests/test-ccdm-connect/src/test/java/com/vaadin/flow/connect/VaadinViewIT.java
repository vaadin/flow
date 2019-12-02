/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.connect;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Class for testing issues in a spring-boot container.
 */
public class VaadinViewIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/foo";
    }

    private TestBenchElement testComponent;

    @Before
    public void setup() throws Exception {
        super.setup();
        open();
        testComponent = $("test-component").first();
    }

    /**
     * Just a control test that assures that webcomponents is working.
     */
    @Test
    public void should_load_web_component() {
        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        WebElement button = testComponent.$(TestBenchElement.class).id("button");
        button.click();
        Assert.assertEquals("Hello World", content.getText());
    }

    /**
     * Just a control test that assures that webcomponents is working.
     * @throws Exception
     */
    @Test
    public void should_request_connect_service() throws Exception {
        WebElement button = testComponent.$(TestBenchElement.class).id("connect");
        button.click();

        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        // Wait for the server connect response
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "Anonymous access is not allowed"), 25);
    }

    @Test
    public void should_requestAnonymously_connect_service() throws Exception {
        WebElement button = testComponent.$(TestBenchElement.class).id(
                "connectAnonymous");
        button.click();

        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        // Wait for the server connect response
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "Hello, stranger!"), 25);
    }

    @Test
    public void should_requestAnonymously_when_CallConnectServiceFromANestedUrl() throws Exception {
        getDriver().get(getRootURL() + getTestPath() + "/more/levels/url");
        testComponent = $("test-component").first();
        WebElement button = testComponent.$(TestBenchElement.class).id(
                "connectAnonymous");
        button.click();

        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        // Wait for the server connect response
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "Hello, stranger!"), 25);
    }

    @Test
    public void should_useSendNull_when_paramterIsUndefined() {
        WebElement button = testComponent.$(TestBenchElement.class).id(
                "echoWithOptional");
        button.click();

        WebElement content = testComponent.$(TestBenchElement.class)
                .id("content");
        // Wait for the server connect response
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "1. one 3. three 4. four"), 25);
    }

    @Test
    public void should_notAbleToRequestAdminOnly_when_NotLoggedIn() {
        verifyCallingAdminService("Anonymous access is not allowed");
    }

    @Test
    public void should_RequestAdminOnly_when_LoggedInAsAdmin() {
        login("admin", "admin");

        // Verify admin calls
        verifyCallingAdminService("Hello, admin!");

        // Verify logged in user calls
        verifyCallingAuthorizedService();

        // Verify anonymous calls when logged in
        verifyCallingAnonymousService();
    }

    @Test
    public void should_NotRequestAdminOnly_when_LoggedInAsUser() {
        login("user", "user");

        // Verify admin calls
        verifyCallingAdminService("Unauthorized access to vaadin service");

        // Verify logged in user calls
        verifyCallingAuthorizedService();

        // Verify anonymous calls when logged in
        verifyCallingAnonymousService();
    }

    @Test
    public void should_add_appShellAnnotations() {
        WebElement meta = findElement(By.cssSelector("meta[name=foo]"));
        Assert.assertNotNull(meta);
        Assert.assertEquals("bar", meta.getAttribute("content"));
    }

    @Test
    public void should_show_pwaDialog() {
        WebElement pwa = findElement(By.id("pwa-ip"));
        Assert.assertTrue(pwa.getText().contains("My App"));
    }

    private void login(String username, String password) {
        String testUrl = getTestURL(getRootURL(), getTestPath() + "/login",
                new String[0]);
        getDriver().get(testUrl);
        TestBenchElement container = $("div")
                .attributeContains("class", "container").first();
        container.$(TestBenchElement.class).id("username").sendKeys(username);
        container.$(TestBenchElement.class).id("password").sendKeys(password);
        container.$("button").first().click();
        // Wait for the server connect response
        testComponent = $("test-component").first();
    }

    private void verifyCallingAdminService(String expectedMessage) {
        WebElement button = testComponent.$(TestBenchElement.class)
                .id("helloAdmin");
        button.click();
        WebElement content = testComponent.$(TestBenchElement.class)
                .id("content");
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                expectedMessage), 25);
    }

    private void verifyCallingAuthorizedService() {
        WebElement content;
        WebElement connect = testComponent.$(TestBenchElement.class)
                .id("connect");
        connect.click();
        content = testComponent.$(TestBenchElement.class).id("content");
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "Hello, Friend!"), 25);
    }

    private void verifyCallingAnonymousService() {
        WebElement content;
        WebElement connectAnonymous = testComponent.$(TestBenchElement.class)
                .id("connectAnonymous");
        connectAnonymous.click();
        content = testComponent.$(TestBenchElement.class).id("content");
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "Hello, stranger!"), 25);
    }
}
