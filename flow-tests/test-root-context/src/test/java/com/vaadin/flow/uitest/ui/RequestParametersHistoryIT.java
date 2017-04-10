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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.PhantomJSTest;

/**
 * @author Vaadin Ltd.
 */
public class RequestParametersHistoryIT extends PhantomJSTest {
    @Test
    public void noParameters() {
        open();
        WebElement label = findElement(By.id(RequestParametersHistoryUI.REQUEST_PARAM_ID));

        Assert.assertEquals(RequestParametersHistoryUI.NO_INPUT_TEXT, label.getText());
    }

    @Test
    public void parameterProvided() {
        String paramValue = "Super-intelligent shade of the colour blue";
        open(String.format("%s=%s", RequestParametersHistoryUI.REQUEST_PARAM_NAME, paramValue));
        WebElement label = findElement(By.id(RequestParametersHistoryUI.REQUEST_PARAM_ID));

        Assert.assertEquals(paramValue, label.getText());
    }

    @Test
    public void goBachToHistoryWithParameters() {
        String oldParamValue = "oldParamValue";
        String newParamValue = "newParamValue";
        open(String.format("%s=%s", RequestParametersHistoryUI.REQUEST_PARAM_NAME, oldParamValue));
        open(String.format("%s=%s", RequestParametersHistoryUI.REQUEST_PARAM_NAME, newParamValue));

        findElement(By.id(RequestParametersHistoryUI.BACK_BUTTON_ID)).click();
        waitForElementPresent(By.id(RequestParametersHistoryUI.REQUEST_PARAM_ID));

        WebElement label = findElement(By.id(RequestParametersHistoryUI.REQUEST_PARAM_ID));

        Assert.assertEquals(oldParamValue, label.getText());
    }
}
