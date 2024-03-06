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
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class RequestParametersHistoryIT extends ChromeBrowserTest {
    @Test
    public void noParameters() {
        open();
        WebElement label = findElement(
                By.id(RequestParametersHistoryView.REQUEST_PARAM_ID));

        Assert.assertEquals(RequestParametersHistoryView.NO_INPUT_TEXT,
                label.getText());
    }

    @Test
    public void parameterProvided() {
        String paramValue = "Super-intelligent shade of the colour blue";
        open(String.format("%s=%s",
                RequestParametersHistoryView.REQUEST_PARAM_NAME, paramValue));
        WebElement label = findElement(
                By.id(RequestParametersHistoryView.REQUEST_PARAM_ID));
        Assert.assertEquals(paramValue, label.getText());
    }

    @Test
    public void goBachToHistoryWithParameters() {
        String oldParamValue = "oldParamValue";
        String newParamValue = "newParamValue";
        open(String.format("%s=%s",
                RequestParametersHistoryView.REQUEST_PARAM_NAME,
                oldParamValue));
        open(String.format("%s=%s",
                RequestParametersHistoryView.REQUEST_PARAM_NAME,
                newParamValue));

        findElement(By.id(RequestParametersHistoryView.BACK_BUTTON_ID)).click();
        waitForElementPresent(
                By.id(RequestParametersHistoryView.REQUEST_PARAM_ID));

        WebElement label = findElement(
                By.id(RequestParametersHistoryView.REQUEST_PARAM_ID));

        Assert.assertEquals(oldParamValue, label.getText());
    }
}
