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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class LongPollingPushIT extends ChromeBrowserTest {

    @Test
    public void openPage_thereAreNoErrorsInTheConsole() {
        open();
        checkLogsForErrors(msg -> msg.contains("sockjs-node")
                || msg.contains("[WDS] Disconnected!"));

        waitForElementNotPresent(By.id("child"));

        WebElement button = findElement(By.id("visibility"));

        button.click();

        waitForElementPresent(By.id("child"));
        WebElement span = findElement(By.id("child"));

        Assert.assertEquals("Some text", span.getAttribute("innerHTML"));
        /*
         * SockJS client may try to connect to sockjs node server:
         * https://github.com/sockjs/sockjs-node/blob/master/README.md.
         *
         * This entry may be ignored.
         */
        checkLogsForErrors(msg -> msg.contains("sockjs-node")
                || msg.contains("[WDS] Disconnected!"));
    }

}
