/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WebStorageIT extends ChromeBrowserTest {

    @Test
    public void testWebStorageSetAndRemove() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detect"));
        WebElement remove = findElement(By.id("remove"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        remove.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }

    @Test
    public void testWebStorageSetAndClear() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detect"));
        WebElement clear = findElement(By.id("clear"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        clear.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }

    @Test
    public void testWebStorageSetAndRemove_completableFuture() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detectCF"));
        WebElement remove = findElement(By.id("remove"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        remove.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }

    @Test
    public void testWebStorageSetAndClear_completableFuture() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detectCF"));
        WebElement clear = findElement(By.id("clear"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        clear.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }
}
