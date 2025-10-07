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
package com.vaadin.viteapp;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.io.File;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.testutil.ChromeDeviceTest;
import com.vaadin.testbench.TestBenchElement;

public class BasicComponentIT extends ChromeDeviceTest {

    private static final String HOTDEPLOY_PROPERTY = "vaadin."
            + InitParameters.FRONTEND_HOTDEPLOY;

    private Server server;

    private WebAppContext context;

    private String hotdeploy;

    protected HttpSession session;

    @Before
    public void init() throws Exception {
        setup(8888);
        getDriver().get(getRootURL());
        waitForDevServer();
        getDriver().get(getRootURL() + "/basic-component.html");
    }

    @Test
    public void session_resynced_webcomponent_is_active() throws Exception {
        waitForWebComponent("login-form");
        // check if web component works
        clickButton();
        Assert.assertEquals("Authentication failure",
                getAuthenticationResult());

        TestBenchElement input = $("login-form").first().$("input").first();

        // simulate expired session by invalidating current session
        session.invalidate();
        // Wait for web component to be detached, session expiration message
        // should be delivered by PUSH long polling connection
        waitUntil(ExpectedConditions.stalenessOf(input));

        waitForElementPresent(By.tagName("login-form"));
        waitUntil(d -> "".equals(getAuthenticationResult()));

        // check if web component works again
        setUsername("admin");
        setPassword("admin");
        clickButton();
        Assert.assertEquals("Authentication success",
                getAuthenticationResult());
    }

    private void clickButton() {
        waitUntil(d -> $("login-form").first().$("button").first()).click();
    }

    private String getAuthenticationResult() {
        return $("login-form").first().$("div").last().getText();
    }

    private void setUsername(String value) {
        $("login-form").first().$("input").first().sendKeys(value + Keys.TAB);
    }

    private void setPassword(String value) {
        $("login-form").first().$("input").last().sendKeys(value + Keys.TAB);
    }

    @Override
    public void checkIfServerAvailable() {
        // NOP
    }

    public void setup(int port) throws Exception {
        hotdeploy = System.getProperty(HOTDEPLOY_PROPERTY);
        System.setProperty(HOTDEPLOY_PROPERTY, "true");
        server = new Server();
        try (ServerConnector connector = new ServerConnector(server)) {
            connector.setPort(port);
            server.setConnectors(new ServerConnector[] { connector });
        }

        File[] warDirs = new File("target")
                .listFiles(file -> file.getName().matches(
                        "vite-embedded-webcomponent-resync-.*-SNAPSHOT\\.war"));
        String warfile = "target/" + warDirs[0].getName();

        context = new WebAppContext(warfile, "/");

        // store session id to be able to invalidate it during test
        context.getSessionHandler().addEventListener(new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent httpSessionEvent) {
                session = httpSessionEvent.getSession();
            }
        });

        server.setHandler(context);
        server.start();
    }

    @After
    public void shutdown() throws Exception {
        try {
            context.stop();
            context.destroy();
            context = null;
        } finally {
            server.stop();
            if (hotdeploy == null) {
                System.clearProperty(HOTDEPLOY_PROPERTY);
            } else {
                System.setProperty(HOTDEPLOY_PROPERTY, hotdeploy);
            }
        }
    }
}
