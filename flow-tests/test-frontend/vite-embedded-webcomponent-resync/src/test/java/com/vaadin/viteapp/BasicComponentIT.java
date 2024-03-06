/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import java.io.File;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class BasicComponentIT extends ChromeDeviceTest {

    private Server server;

    private WebAppContext context;

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

        // check if web component works
        clickButton();
        Assert.assertEquals("Authentication failure",
                getAuthenticationResult());

        // simulate expired session by invalidating current session
        session.invalidate();

        // init request to resynchronize expired session and recreate components
        clickButton();

        try {
            // it seems WebDriver needs also sync to new session
            setUsername("");
        } catch (StaleElementReferenceException ex) {
            // NOP
        }

        // check if web component works again
        setUsername("admin");
        setPassword("admin");
        clickButton();
        Assert.assertEquals("Authentication success",
                getAuthenticationResult());
    }

    private void clickButton() {
        $("login-form").first().$("button").first().click();
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
        }
    }
}
