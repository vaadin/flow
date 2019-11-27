/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SynchronizedPropertyIT extends ChromeBrowserTest {

    @Test
    public void synchronizeOnChange() {
        open();
        WebElement syncOnChange = findElement(By.id("syncOnChange"));
        WebElement labelSyncOnChange = findElement(By.id("syncOnChangeLabel"));
        syncOnChange.sendKeys("123");
        blur();
        Assert.assertEquals("Server value: 123", labelSyncOnChange.getText());
        syncOnChange.sendKeys("456");
        blur();
        Assert.assertEquals("Server value: 123456",
                labelSyncOnChange.getText());
    }

    @Test
    public void synchronizeOnKeyUp() {
        open();
        WebElement syncOnKeyUp = findElement(By.id("syncOnKeyUp"));
        WebElement labelSyncOnKeyUp = findElement(By.id("syncOnKeyUpLabel"));
        syncOnKeyUp.sendKeys("1");
        syncOnKeyUp.sendKeys("2");
        syncOnKeyUp.sendKeys("3");
        Assert.assertEquals("Server value: 123", labelSyncOnKeyUp.getText());
        syncOnKeyUp.sendKeys("4");
        syncOnKeyUp.sendKeys("5");
        syncOnKeyUp.sendKeys("6");
        Assert.assertEquals("Server value: 123456", labelSyncOnKeyUp.getText());
    }

    @Test
    public void synchronizeInitialValueNotSentToServer() {
        open();
        WebElement syncOnChangeInitialValue = findElement(
                By.id("syncOnChangeInitialValue"));
        WebElement labelSyncOnChange = findElement(
                By.id("syncOnChangeInitialValueLabel"));

        // Property was set after label was created and sync set up
        // It is intentionally in the "wrong" state until there is a sync
        // message from the client
        Assert.assertEquals("Server value on create: null",
                labelSyncOnChange.getText());
        syncOnChangeInitialValue.sendKeys(Keys.END);
        syncOnChangeInitialValue.sendKeys("123");
        blur();
        Assert.assertEquals("Server value in change listener: initial123",
                labelSyncOnChange.getText());

    }

    @Test
    public void synchronizeMultipleProperties() {
        open();
        WebElement multiSyncValue = findElement(By.id("multiSyncValue"));
        WebElement multiSyncValueLabel = findElement(
                By.id("multiSyncValueLabel"));
        WebElement multiSyncValueAsNumberLabel = findElement(
                By.id("multiSyncValueAsNumberLabel"));
        multiSyncValue.sendKeys("123");
        waitUntil(driver -> "Server value: 123"
                .equals(multiSyncValueLabel.getText()), 2);
        Assert.assertEquals("", multiSyncValueAsNumberLabel.getText());
        blur();
        waitUntil(driver -> "Server value: 123"
                .equals(multiSyncValueLabel.getText()), 2);
        Assert.assertEquals("Server valueAsNumber: 123",
                multiSyncValueAsNumberLabel.getText());

        multiSyncValue.sendKeys("456");
        waitUntil(driver -> "Server value: 123456"
                .equals(multiSyncValueLabel.getText()), 2);
        Assert.assertEquals("Server valueAsNumber: 123",
                multiSyncValueAsNumberLabel.getText());
        blur();
        waitUntil(driver -> "Server value: 123456"
                .equals(multiSyncValueLabel.getText()), 2);
        Assert.assertEquals("Server valueAsNumber: 123456",
                multiSyncValueAsNumberLabel.getText());
    }

}
