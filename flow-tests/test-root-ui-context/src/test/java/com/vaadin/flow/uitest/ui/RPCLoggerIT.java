/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class RPCLoggerIT extends ChromeBrowserTest {

    @BrowserTest
    public void interceptRpcInvocations()
            throws URISyntaxException, InterruptedException {
        open();

        WebElement button = findElement(By.tagName("button"));
        new Actions(getDriver()).moveToElement(button).click().build()
                .perform();
        // button click -> one log item with type event and required node
        List<WebElement> logs = findElements(By.className("log"));
        Assertions.assertEquals(1, logs.size());
        Assertions.assertEquals("Node is 5",
                logs.get(0).findElement(By.className("node")).getText());
        String json = logs.get(0).findElement(By.className("json")).getText();
        Assertions.assertTrue(json.contains("\"type\":\"event\""));
        Assertions.assertTrue(json.contains("\"node\":5"));

        WebElement input = findElement(By.tagName("input"));
        input.sendKeys("foo");
        input.sendKeys(Keys.TAB);
        // set text in the input -> RPCs for property synchronization, and dom
        // event
        logs = findElements(By.className("log"));
        Assertions.assertEquals(3, logs.size());

        Assertions.assertEquals("Node is 3",
                logs.get(1).findElement(By.className("node")).getText());
        json = logs.get(1).findElement(By.className("json")).getText();
        Assertions.assertTrue(json.contains("\"type\":\"mSync\""));
        Assertions.assertTrue(json
                .contains("\"feature\":" + NodeFeatures.ELEMENT_PROPERTIES));
        Assertions.assertTrue(json.contains("\"node\":3"));
        Assertions.assertTrue(json.contains("\"value\":\"foo\""));

        Assertions.assertEquals("Node is 3",
                logs.get(2).findElement(By.className("node")).getText());
        json = logs.get(2).findElement(By.className("json")).getText();
        Assertions.assertTrue(json.contains("\"type\":\"event\""));
        Assertions.assertTrue(json.contains("\"node\":3"));
        Assertions.assertTrue(json.contains("\"event\":\"change\""));
    }

    @Override
    protected String getTestPath() {
        return "/rpc/rpc-logger";
    }
}
