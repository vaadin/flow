/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class RequestParametersIT extends ChromeBrowserTest {

    @Test
    public void noParameters() {
        open();
        WebElement label = findElement(By.id(RequestParametersView.REQUEST_PARAM_ID));

        Assert.assertEquals(RequestParametersView.NO_INPUT_TEXT, label.getText());
    }

    @Test
    public void parameterProvided() {
        String paramValue = "Super-intelligent shade of the colour blue";
        open(String.format("%s=%s", RequestParametersView.REQUEST_PARAM_NAME, paramValue));

        if (hasClientIssue("7589")) {
            return;
        }
        WebElement label = findElement(By.id(RequestParametersView.REQUEST_PARAM_ID));

        Assert.assertEquals(paramValue, label.getText());
    }
}
