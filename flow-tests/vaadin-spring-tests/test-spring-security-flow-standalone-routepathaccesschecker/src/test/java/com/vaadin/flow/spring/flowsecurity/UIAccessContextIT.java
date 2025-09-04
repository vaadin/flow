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

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.login.testbench.LoginOverlayElement;
import com.vaadin.testbench.HasElementQuery;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

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
            TestBenchElement balance = $("span").id("balanceText");
            Assert.assertEquals(expectedUserBalance, balance.getText());

            open("private", adminBrowser);
            HasElementQuery adminContext = () -> adminBrowser;
            loginAdmin(adminContext);
            TestBenchElement adminBalance = adminContext.$("span")
                    .id("balanceText");
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
        LoginOverlayElement form = adminContext.$(LoginOverlayElement.class)
                .first();
        form.getUsernameField().setValue("emma");
        form.getPasswordField().setValue("emma");
        form.submit();
    }

}
