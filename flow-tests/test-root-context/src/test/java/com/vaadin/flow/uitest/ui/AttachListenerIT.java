/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
