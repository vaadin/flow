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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshIT extends ChromeBrowserTest {
    private static final String MODIFIED = "modified";
    private static final String UNMODIFIED = "unmodified";
    private static final String INPUT_ID = "value";

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/preserveOnRefresh.html";
    }

    @Test
    public void twoComponents_onlyModifiedComponent_should_keepValueAfterRefresh() {
        open();

        waitForElementVisible(By.id(MODIFIED));

        WebElement modifiedComponent = findElement(By.id(MODIFIED));
        WebElement unmodifiedComponent = findElement(By.id(UNMODIFIED));

        Assert.assertEquals(MODIFIED + "-input should be empty", "",
                getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + "-input should be empty", "",
                getValue(UNMODIFIED));

        final String EXPECTED = "expected text";
        writeInInput(MODIFIED, EXPECTED);

        Assert.assertEquals(MODIFIED + "-input should have text", EXPECTED,
                getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + "-input should be empty", "",
                getValue(UNMODIFIED));

        refreshPage();

        Assert.assertEquals(MODIFIED + "-input should have text after refresh",
                EXPECTED, getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + "-input should be empty after refresh",
                "", getValue(UNMODIFIED));
    }

    private String getValue(String id) {
        WebElement count = findElement(By.id(id));
        return count.findElement(By.id(INPUT_ID)).getText();
    }

    private void writeInInput(String id, String text) {
        WebElement count = findElement(By.id(id));
        count.findElement(By.id(INPUT_ID)).sendKeys(text);
    }

    private void refreshPage() {
        getCommandExecutor().executeScript("location.reload()");
        waitForElementVisible(By.id(MODIFIED));
    }
}
