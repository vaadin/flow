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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

public class ErrorMessageEscKeyIT extends ChromeBrowserTest {

    private static final By BY_TAG_LABEL = By.tagName("label");
    private static final By BY_TAG_BUTTON = By.tagName("button");
    private static final By BY_CLASS_SYSTEM_ERROR = By
            .className("v-system-error");

    @Test
    public void testErrorViewEscKey() {
        open();

        assertViewStartup();
        assertCookiesError();

        findElement(BY_TAG_BUTTON).sendKeys(Keys.ESCAPE);

        assertViewStartup();
        assertCookiesError();

        findElement(BY_CLASS_SYSTEM_ERROR).click();

        assertViewStartup();
    }

    private void assertViewStartup() {
        assertLabelsCount(0);

        final WebElement button = findElement(BY_TAG_BUTTON);
        button.click();
        assertLabelsCount(1);

        button.click();
        assertLabelsCount(2);
    }

    private void assertCookiesError() {
        final int labelsCount = countLabels();

        getDriver().manage().deleteAllCookies();

        final WebElement button = findElement(BY_TAG_BUTTON);
        button.click();

        final WebElement error = findElement(BY_CLASS_SYSTEM_ERROR);
        final WebElement caption = error.findElement(By.className("caption"));

        Assert.assertEquals("Invalid error message", "Cookies disabled",
                caption.getText());

        // button.click();
        assertLabelsCount(labelsCount);
    }

    private void assertLabelsCount(int count) {
        Assert.assertEquals("There should be " + count + " label.", count,
                countLabels());
    }

    private int countLabels() {
        return findElements(BY_TAG_LABEL).size();
    }

}
