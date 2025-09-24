/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.login.testbench.LoginFormElement;
import com.vaadin.flow.component.login.testbench.LoginOverlayElement;
import com.vaadin.testbench.HasElementQuery;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

public class UIAccessContextIT extends AbstractIT {

    @Test
    public void securityContextSetForUIAccess() throws Exception {
        String expectedUserBalance = "Hello John the User, your bank account balance is $10000.00.";
        String expectedAdminBalance = "Hello Emma the Admin, your bank account balance is $200000.00.";

        WebDriver adminBrowser = getDriver();
        try {
            super.setup();
            open("private");
            loginUser();
            TestBenchElement balance = waitUntil(
                    d -> $("span").id("balanceText"));
            Assert.assertEquals(expectedUserBalance, balance.getText());

            open("private", adminBrowser);
            HasElementQuery adminContext = () -> adminBrowser;
            loginAdmin(adminContext);
            TestBenchElement adminBalance = waitUntil(
                    d -> adminContext.$("span").id("balanceText"));
            Assert.assertEquals(expectedAdminBalance, adminBalance.getText());

            ButtonElement sendRefresh = $(ButtonElement.class)
                    .id("sendRefresh");
            sendRefresh.click();
            Assert.assertEquals(expectedUserBalance, balance.getText());
            Assert.assertEquals(expectedAdminBalance, adminBalance.getText());

            ButtonElement adminSendRefresh = adminContext.$(ButtonElement.class)
                    .id("sendRefresh");
            adminSendRefresh.click();
            Assert.assertEquals(expectedUserBalance, balance.getText());
            Assert.assertEquals(expectedAdminBalance, adminBalance.getText());
        } finally {
            adminBrowser.quit();
        }
    }

    private void loginAdmin(HasElementQuery adminContext) {
        waitForClientRouter();
        LoginFormElement form = adminContext.$(LoginOverlayElement.class)
                .first().getLoginForm();
        form.getUsernameField().setValue("emma");
        form.getPasswordField().setValue("emma");
        // Try to wait before pressing submit button
        new Actions(getDriver()).pause(1000).perform();
        form.submit();
        waitUntilNot(driver -> ((WebDriver) adminContext.getContext())
                .getCurrentUrl().contains("my/login/page"));
        waitUntilNot(
                driver -> adminContext.$(LoginOverlayElement.class).exists());

    }

}
