/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category({ PushTests.class, IgnoreOSGi.class })
public class SsePushSessionExpiredIT extends ChromeBrowserTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return SsePushReconnectView.class;
    }

    @Test
    public void sessionGone_pushReconnects_pageReloads() {
        open();
        AbstractClientServerCounterIT.getIncrementButton(this).click();
        waitUntil(driver -> AbstractClientServerCounterIT
                .getClientCounter(this) == 1);
        // Survives push reconnects, but not a reload
        executeScript("window.beforeSessionGone = true");

        // As after a server restart: the server closes the push connection,
        // and the session is gone when the client opens a new one
        findElement(By.id(SsePushReconnectView.CLOSE_PUSH_LATER_ID)).click();
        getDriver().manage().deleteCookieNamed("JSESSIONID");

        // The new push connection is answered with the session expired
        // notification, which reloads the page by default. If the client
        // misses the notification, it keeps opening new push connections and
        // the page is not reloaded.
        try {
            waitUntil(
                    driver -> Boolean.TRUE.equals(executeScript(
                            "return window.beforeSessionGone === undefined")),
                    20);
        } catch (TimeoutException e) {
            Assert.fail("The page was not reloaded after the session was gone");
        }
    }
}
