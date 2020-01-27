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

import org.junit.After;
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
public class AppViewIT extends ChromeBrowserTest {

    private void openTestUrl(String url) {
        getDriver().get(getRootURL() + "/foo" + url);
    }

    private TestBenchElement testComponent;
    private WebElement content;

    @Before
    public void setup() throws Exception {
        super.setup();
        openTestUrl("/");
        testComponent = $("test-component").first();
        content = testComponent.$(TestBenchElement.class).id("content");
    }

    @After
    public void tearDown() {
        openTestUrl("/logout");
    }

    /**
     * Just a control test that assures that webcomponents is working.
     */
    @Test
    public void should_load_web_component() {
        WebElement button = testComponent.$(TestBenchElement.class).id("button");
        button.click();
        verifyContent("Hello World");
    }

    /**
     * Just a control test that assures that webcomponents is working.
     * @throws Exception
     */
    @Test
    public void should_request_connect_service() throws Exception {
        WebElement button = testComponent.$(TestBenchElement.class).id("hello");
        button.click();

        // Wait for the server connect response
        verifyContent("Anonymous access is not allowed");
    }

    @Test
    public void should_requestAnonymously_connect_service() throws Exception {
        WebElement button = testComponent.$(TestBenchElement.class).id(
                "helloAnonymous");
        button.click();

        // Wait for the server connect response
        verifyContent("Hello, stranger!");
    }

    @Test
    public void should_requestAnonymously_when_CallConnectServiceFromANestedUrl() throws Exception {
        openTestUrl("/more/levels/url");

        testComponent = $("test-component").first();
        content = testComponent.$(TestBenchElement.class).id("content");

        WebElement button = testComponent.$(TestBenchElement.class).id(
                "helloAnonymous");
        button.click();

        // Wait for the server connect response
        verifyContent("Hello, stranger!");
    }

    @Test
    public void should_useSendNull_when_paramterIsUndefined() {
        WebElement button = testComponent.$(TestBenchElement.class).id(
                "echoWithOptional");
        button.click();

        // Wait for the server connect response
        verifyContent("1. one 3. three 4. four");
    }

    @Test
    public void should_notAbleToRequestAdminOnly_when_NotLoggedIn() {
        verifyCallingAdminService("Anonymous access is not allowed");
    }

    @Test
    public void should_RequestAdminOnly_when_LoggedInAsAdmin() {
        login("admin");

        // Verify admin calls
        verifyCallingAdminService("Hello, admin!");

        // Verify logged in user calls
        verifyCallingAuthorizedService();

        // Verify anonymous calls when logged in
        verifyCallingAnonymousService();
    }

    @Test
    public void should_NotRequestAdminOnly_when_LoggedInAsUser() {
        login("user");

        // Verify admin calls
        verifyCallingAdminService("Unauthorized access to Vaadin endpoint");

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

    @Test
    public void should_beAble_toLogin_usingSpringForm() {
        // Login by using the Spring Login Form
        openTestUrl("/login");

        TestBenchElement container = $("div")
                .attributeContains("class", "container").first();
        container.$(TestBenchElement.class).id("username").sendKeys("admin");
        container.$(TestBenchElement.class).id("password").sendKeys("admin");
        container.$("button").first().click();

        // Wait for the server connect response
        testComponent = $("test-component").first();
        content = testComponent.$(TestBenchElement.class).id("content");

        // Verify admin calls
        verifyCallingAdminService("Hello, admin!");
    }

    @Test
    public void should_checkAnonymousUser() {
        testComponent.$(TestBenchElement.class).id("checkUser").click();
        verifyCheckUser("anonymousUser");
    }

    @Test
    public void should_checkUser() {
        login("user");
        testComponent.$(TestBenchElement.class).id("checkUser").click();
        verifyCheckUser("user");
    }
    @Test
    public void should_checkAdminUser() {
        login("admin");
        testComponent.$(TestBenchElement.class).id("checkUser").click();
        verifyCheckUser("admin");
    }

    private void login(String user) {
        // Use form in the test component
        testComponent.$(TestBenchElement.class).id("username").sendKeys(user);
        testComponent.$(TestBenchElement.class).id("password").sendKeys(user);
        testComponent.$(TestBenchElement.class).id("login").click();
        testComponent = $("test-component").first();
        content = testComponent.$(TestBenchElement.class).id("content");
    }

    private void verifyCallingAdminService(String expectedMessage) {
        testComponent.$(TestBenchElement.class).id("helloAdmin").click();
        verifyContent(expectedMessage);
    }

    private void verifyCallingAuthorizedService() {
        testComponent.$(TestBenchElement.class).id("hello").click();
        verifyContent("Hello, Friend!");
    }

    private void verifyCallingAnonymousService() {
        testComponent.$(TestBenchElement.class).id("helloAnonymous").click();
        verifyContent("Hello, stranger!");
    }

    private void verifyCheckUser(String expectedMessage) {
        testComponent.$(TestBenchElement.class).id("checkUser").click();
        verifyContent(expectedMessage);
    }

    private void verifyContent(String expected) {
        waitUntil(
                ExpectedConditions.textToBePresentInElement(content, expected),
                25);
    }
}
