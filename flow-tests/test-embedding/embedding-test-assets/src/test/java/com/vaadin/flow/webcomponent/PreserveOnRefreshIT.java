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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshIT extends ChromeBrowserTest {
    private static final String MODIFIED = "modified";
    private static final String UNMODIFIED = "unmodified";
    private static final String NO_PRESERVE = "nopreserve";
    private static final String INPUT_ID = "value";

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/preserveOnRefresh.html";
    }

    @Before
    public void init() {
        open();
        waitForElementVisible(By.id(MODIFIED));
    }

    @Test
    public void twoPreservedComponents_modifiedValue_shouldNot_propagateToOtherComponentAfterRefresh() {
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

    @Test
    public void preservedAndUnpreservedComponents_onlyPreservedComponent_should_keepNewValueAfterRefresh() {
        Assert.assertEquals(MODIFIED + "-input should be empty", "",
                getValue(MODIFIED));
        Assert.assertEquals(NO_PRESERVE + "-input should be empty", "",
                getValue(NO_PRESERVE));

        final String EXPECTED = "expected text";
        writeInInput(MODIFIED, EXPECTED);
        writeInInput(NO_PRESERVE, EXPECTED);

        Assert.assertEquals(MODIFIED + "-input should have text", EXPECTED,
                getValue(MODIFIED));
        Assert.assertEquals(NO_PRESERVE + "-input should have text", EXPECTED,
                getValue(NO_PRESERVE));

        refreshPage();

        Assert.assertEquals(MODIFIED + "-input should have text after refresh",
                EXPECTED, getValue(MODIFIED));
        Assert.assertEquals(
                NO_PRESERVE + "-input should be empty after refresh", "",
                getValue(NO_PRESERVE));
    }

    @Test
    public void whenValueIsChangedOnPreservingComponent_should_preserveTheNewValueAfterRefresh() {
        Assert.assertEquals(MODIFIED + "-input should be empty", "",
                getValue(MODIFIED));

        // first value change and refresh
        final String EXPECTED_1 = "expected text";
        writeInInput(MODIFIED, EXPECTED_1);

        Assert.assertEquals(MODIFIED + "-input should have text", EXPECTED_1,
                getValue(MODIFIED));

        refreshPage();
        Assert.assertEquals(MODIFIED + "-input should display first changed "
                + "text after refresh", EXPECTED_1, getValue(MODIFIED));

        // second value change and refresh
        final String EXPECTED_2 = EXPECTED_1 + " with additions";
        writeInInput(MODIFIED, " with additions");

        Assert.assertEquals(MODIFIED + "-input should have text", EXPECTED_2,
                getValue(MODIFIED));

        refreshPage();
        Assert.assertEquals(MODIFIED + "-input should display second changed "
                + "text after refresh", EXPECTED_2, getValue(MODIFIED));
    }

    private String getValue(String id) {
        WebElement element = findElement(By.id(id));
        return element.findElement(By.id(INPUT_ID)).getAttribute("value");
    }

    private void writeInInput(String id, String text) {
        WebElement element = findElement(By.id(id));
        element.findElement(By.id(INPUT_ID)).sendKeys(text, Keys.ENTER);
    }

    private void refreshPage() {
        getCommandExecutor().executeScript("location.reload()");
        waitForElementVisible(By.id(MODIFIED));
    }
}
