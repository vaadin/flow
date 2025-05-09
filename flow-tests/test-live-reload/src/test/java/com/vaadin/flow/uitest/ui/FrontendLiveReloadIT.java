/*
 * Copyright 2000-2025 Vaadin Ltd.
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

    private String getValue(WebElement element) {
        Object result = getCommandExecutor()
                .executeScript("return arguments[0].value;", element);
        return result == null ? "" : result.toString();
    }
}
