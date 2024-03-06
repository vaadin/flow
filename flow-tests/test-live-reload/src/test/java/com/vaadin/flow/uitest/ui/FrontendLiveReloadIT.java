/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class FrontendLiveReloadIT extends AbstractLiveReloadIT {

    @After
    public void resetFrontend() {
        executeScript("fetch('/context/view/reset_frontend')");
    }

    @Test
    public void liveReloadOnTouchedFrontendFile() {
        open();

        // when: the frontend code is updated
        WebElement codeField = findElement(
                By.id(FrontendLiveReloadView.FRONTEND_CODE_TEXT));
        String oldCode = getValue(codeField);
        String newCode = oldCode.replace("Custom component contents",
                "Updated component contents");
        codeField.clear();
        codeField.sendKeys(newCode);

        waitForElementPresent(
                By.id(FrontendLiveReloadView.FRONTEND_CODE_UPDATE_BUTTON));
        WebElement liveReloadTrigger = findElement(
                By.id(FrontendLiveReloadView.FRONTEND_CODE_UPDATE_BUTTON));
        liveReloadTrigger.click();

        // when: the page has reloaded
        waitForLiveReload();

        // then: the frontend changes are visible in the DOM
        TestBenchElement customComponent = $("*")
                .id(FrontendLiveReloadView.CUSTOM_COMPONENT);
        TestBenchElement embeddedDiv = customComponent.$("*").id("custom-div");
        Assert.assertEquals("Updated component contents",
                embeddedDiv.getText());
    }

    @Test
    public void webpackErrorIsShownAfterReloadAndHiddenAfterFix() {
        open();

        // when: a webpack error occurs during frontend file edit
        WebElement codeField = findElement(
                By.id(FrontendLiveReloadView.FRONTEND_CODE_TEXT));
        String oldCode = getValue(codeField);
        String erroneousCode = "{" + oldCode;
        codeField.clear();
        codeField.sendKeys(erroneousCode); // illegal TS
        WebElement insertWebpackError = findElement(
                By.id(FrontendLiveReloadView.FRONTEND_CODE_UPDATE_BUTTON));
        insertWebpackError.click();

        // then: an error box is shown
        testBench().disableWaitForVaadin();
        waitForElementPresent(errorBoxSelector());

        // when: the error is corrected
        resetFrontend();
        testBench().enableWaitForVaadin();

        // then: the error box is not shown and the view is reloaded
        waitForElementNotPresent(errorBoxSelector());
    }

    private String getValue(WebElement element) {
        Object result = getCommandExecutor()
                .executeScript("return arguments[0].value;", element);
        return result == null ? "" : result.toString();
    }

    protected By errorBoxSelector() {
        return By.tagName("vite-error-overlay");
    }
}
