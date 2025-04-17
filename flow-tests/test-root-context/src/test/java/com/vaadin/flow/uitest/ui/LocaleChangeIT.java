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

import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.LocaleChangeView.CHANGE_LOCALE_BUTTON_ID;
import static com.vaadin.flow.uitest.ui.LocaleChangeView.SAME_UI_RESULT_ID;
import static com.vaadin.flow.uitest.ui.LocaleChangeView.SHOW_RESULTS_BUTTON_ID;
import org.openqa.selenium.WindowType;

public class LocaleChangeIT extends ChromeBrowserTest {

    @Test
    public void setSessionLocale_currentUIInstanceUpdatedUponEachLocaleUpdate() {
        final int openedUI = 3;

        IntStream.range(0, openedUI).forEach(i -> {
            driver.switchTo().newWindow(WindowType.TAB);
            open();
        });

        waitForElementPresent(By.id(CHANGE_LOCALE_BUTTON_ID));
        findElement(By.id(CHANGE_LOCALE_BUTTON_ID)).click();

        waitForElementPresent(By.id(SHOW_RESULTS_BUTTON_ID));
        findElement(By.id(SHOW_RESULTS_BUTTON_ID)).click();

        IntStream.range(0, openedUI).forEach(i -> {
            String id = String.format("%s-%d", SAME_UI_RESULT_ID, i);
            waitForElementPresent(By.id(id));
            WebElement result = findElement(By.id(id));
            Assert.assertTrue(
                    "Component's UI and current UI instances are different",
                    Boolean.parseBoolean(result.getText()));
        });
    }
}
