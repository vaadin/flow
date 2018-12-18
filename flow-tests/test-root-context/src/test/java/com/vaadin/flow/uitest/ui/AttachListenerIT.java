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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AttachListenerIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Test
    public void firstAddedToMiddleOnFirstAttach() {
        assertCombination("middleAsHost", "firstAsChild",
                "attachListenerToFirst", "MiddleFirstLast");
    }

    @Test
    public void firstAddedToMiddleOnMiddleAttach() {
        assertCombination("middleAsHost", "firstAsChild",
                "attachListenerToMiddle", "MiddleFirstLast");
    }

    @Test
    public void firstAddedToMiddleOnLastAttach() {
        assertCombination("middleAsHost", "firstAsChild",
                "attachListenerToLast", "MiddleFirstLast");
    }

    @Test
    public void firstAddedToLastOnFirstAttach() {
        assertCombination("lastAsHost", "firstAsChild", "attachListenerToFirst",
                "MiddleLastFirst");
    }

    @Test
    public void firstAddedToLastOnMiddleAttach() {
        assertCombination("lastAsHost", "firstAsChild",
                "attachListenerToMiddle", "MiddleLastFirst");
    }

    @Test
    public void firstAddedToLastOnLastAttach() {
        assertCombination("lastAsHost", "firstAsChild", "attachListenerToLast",
                "MiddleLastFirst");
    }

    @Test
    public void lastAddedToMiddleOnFirstAttach() {
        assertCombination("middleAsHost", "lastAsChild",
                "attachListenerToFirst", "FirstMiddleLast");
    }

    @Test
    public void lastAddedToMiddleOnMiddleAttach() {
        assertCombination("middleAsHost", "lastAsChild",
                "attachListenerToMiddle", "FirstMiddleLast");
    }

    @Test
    public void lastAddedToMiddleOnLastAttach() {
        assertCombination("middleAsHost", "lastAsChild", "attachListenerToLast",
                "FirstMiddleLast");
    }

    @Test
    public void middleAddedToLastOnFirstAttach() {
        assertCombination("lastAsHost", "middleAsChild",
                "attachListenerToFirst", "FirstLastMiddle");
    }

    @Test
    public void middleAddedToLastOnMiddleAttach() {
        assertCombination("lastAsHost", "middleAsChild",
                "attachListenerToMiddle", "FirstLastMiddle");
    }

    @Test
    public void middleAddedToLastOnLastAttach() {
        assertCombination("lastAsHost", "middleAsChild", "attachListenerToLast",
                "FirstLastMiddle");
    }

    @Test
    public void middleAddedToFirstOnFirstAttach() {
        assertCombination("firstAsHost", "middleAsChild",
                "attachListenerToFirst", "FirstMiddleLast");
    }

    @Test
    public void middleAddedToFirstOnMiddleAttach() {
        assertCombination("firstAsHost", "middleAsChild",
                "attachListenerToMiddle", "FirstMiddleLast");
    }

    @Test
    public void middleAddedToFirstOnLastAttach() {
        assertCombination("firstAsHost", "middleAsChild",
                "attachListenerToLast", "FirstMiddleLast");
    }

    @Test
    public void lastAddedToFirstOnFirstAttach() {
        assertCombination("firstAsHost", "lastAsChild", "attachListenerToFirst",
                "FirstLastMiddle");
    }

    @Test
    public void lastAddedToFirstOnMiddleAttach() {
        assertCombination("firstAsHost", "lastAsChild",
                "attachListenerToMiddle", "FirstLastMiddle");
    }

    @Test
    public void lastAddedToFirstOnLastAttach() {
        assertCombination("firstAsHost", "lastAsChild", "attachListenerToLast",
                "FirstLastMiddle");
    }

    private void assertCombination(String host, String child, String listener,
            String expectedResult) {
        WebElement hostRadio = findElement(By.id(host));
        WebElement childRadio = findElement(By.id(child));
        WebElement listenerRadio = findElement(By.id(listener));

        hostRadio.click();
        childRadio.click();
        listenerRadio.click();

        findElement(By.id("submit")).click();

        waitForElementPresent(By.id("result"));

        WebElement result = findElement(By.id("result"));
        Assert.assertEquals(expectedResult, result.getText());
    }

}
