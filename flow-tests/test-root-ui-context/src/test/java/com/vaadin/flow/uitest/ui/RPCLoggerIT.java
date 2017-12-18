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
package com.vaadin.flow.uitest.ui;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

/**
 * @author Vaadin Ltd
 *
 */
public class RPCLoggerIT extends ChromeBrowserTest {

    @Test
    public void interceptRpcInvocations()
            throws URISyntaxException, InterruptedException {
        open();

        findElement(By.tagName("button")).click();
        // button click -> one log item with type event and required node
        List<WebElement> logs = findElements(By.className("log"));
        Assert.assertEquals(1, logs.size());
        Assert.assertEquals("Node is 4",
                logs.get(0).findElement(By.className("node")).getText());
        String json = logs.get(0).findElement(By.className("json")).getText();
        Assert.assertTrue(json.contains("\"type\":\"event\""));
        Assert.assertTrue(json.contains("\"node\":4"));

        WebElement input = findElement(By.tagName("input"));
        input.sendKeys("foo");
        input.sendKeys(Keys.ENTER);
        // set text in the input -> server RPC with type "pSync" and required
        // node
        logs = findElements(By.className("log"));
        Assert.assertEquals(2, logs.size());
        Assert.assertEquals("Node is 5",
                logs.get(1).findElement(By.className("node")).getText());
        json = logs.get(1).findElement(By.className("json")).getText();
        Assert.assertTrue(json.contains("\"type\":\"mSync\""));
        Assert.assertTrue(json
                .contains("\"feature\":" + NodeFeatures.ELEMENT_PROPERTIES));
        Assert.assertTrue(json.contains("\"node\":5"));
        Assert.assertTrue(json.contains("\"value\":\"foo\""));
    }

    @Override
    protected String getTestPath() {
        return "/rpc/" + RPCLoggerUI.class.getCanonicalName();
    }
}
