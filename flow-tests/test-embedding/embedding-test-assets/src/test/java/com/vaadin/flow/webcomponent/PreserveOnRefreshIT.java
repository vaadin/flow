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

import java.util.List;

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
    private static final String NEW_ID = "new-id";
    private static final String INTERNAL_INPUT_ID = "value";
    private static final String PRESERVE_ON_REFRESH_TAG = "preserve-on-refresh";

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
        Assert.assertEquals(MODIFIED + " input should be empty", "",
                getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + " input should be empty", "",
                getValue(UNMODIFIED));

        final String EXPECTED = "expected text";
        writeInInput(MODIFIED, EXPECTED);

        Assert.assertEquals(MODIFIED + " input should have text", EXPECTED,
                getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + " input should be empty", "",
                getValue(UNMODIFIED));

        refreshPage();

        Assert.assertEquals(MODIFIED + " input should have text after refresh",
                EXPECTED, getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + " input should be empty after refresh",
                "", getValue(UNMODIFIED));
    }

    @Test
    public void preservedAndUnpreservedComponents_onlyPreservedComponent_should_keepNewValueAfterRefresh() {
        Assert.assertEquals(MODIFIED + " input should be empty", "",
                getValue(MODIFIED));
        Assert.assertEquals(NO_PRESERVE + " input should be empty", "",
                getValue(NO_PRESERVE));

        final String EXPECTED = "expected text";
        writeInInput(MODIFIED, EXPECTED);
        writeInInput(NO_PRESERVE, EXPECTED);

        Assert.assertEquals(MODIFIED + " input should have text", EXPECTED,
                getValue(MODIFIED));
        Assert.assertEquals(NO_PRESERVE + " input should have text", EXPECTED,
                getValue(NO_PRESERVE));

        refreshPage();

        Assert.assertEquals(MODIFIED + " input should have text after refresh",
                EXPECTED, getValue(MODIFIED));
        Assert.assertEquals(
                NO_PRESERVE + " input should be empty after refresh", "",
                getValue(NO_PRESERVE));
    }

    @Test
    public void whenValueIsChangedOnPreservingComponent_should_preserveTheNewValueAfterRefresh() {
        Assert.assertEquals(MODIFIED + " input should be empty", "",
                getValue(MODIFIED));

        // first value change and refresh
        final String EXPECTED_1 = "expected text";
        writeInInput(MODIFIED, EXPECTED_1);

        Assert.assertEquals(MODIFIED + " input should have text", EXPECTED_1,
                getValue(MODIFIED));

        refreshPage();
        Assert.assertEquals(MODIFIED + " input should display first changed "
                + "text after refresh", EXPECTED_1, getValue(MODIFIED));

        // second value change and refresh
        final String EXPECTED_2 = EXPECTED_1 + " with additions";
        writeInInput(MODIFIED, " with additions");

        Assert.assertEquals(MODIFIED + " input should have text", EXPECTED_2,
                getValue(MODIFIED));

        refreshPage();
        Assert.assertEquals(MODIFIED + " input should display second changed "
                + "text after refresh", EXPECTED_2, getValue(MODIFIED));
    }

    @Test
    public void openingToAnotherPageWithSameComponents_should_preserveComponentWithoutChangedIdAttribute() {
        /*
            Currently, when preserving exported components, they are
            identified by window name, component's tag, and generated id of
            the embedded web component or id of the wrapping element (if
            available).
            Since windows name does not necessarily change when page changes,
            and generated id is only defined by type of the web component and
            its order on the page, the component cannot be reliably
            identified uniquely.
            In order to assure uniqueness, the user must provide id for the
            component. If if is provided, the uniqueness can be assured
            between pages.
            Contents are synchronized between locations in two cases:
            - embedded element has the same id as its counter-part in the
              different location.
            - component does not have user-assigned id and has the same
              generated id on both locations (this happens, if the component
              has the same counted index within the component type between
              pages)
         */

        final String TEXT_CONTENTS = "black cat";

        WebElement noIdElement = findTagWithoutId(PRESERVE_ON_REFRESH_TAG);

        Assert.assertEquals(MODIFIED + " input should be empty", "",
                getValue(MODIFIED));
        Assert.assertEquals(UNMODIFIED + " input should be empty", "",
                getValue(UNMODIFIED));
        Assert.assertEquals("Input without id should be empty", "",
                getValue(noIdElement));

        writeInInput(MODIFIED, TEXT_CONTENTS);
        writeInInput(UNMODIFIED, TEXT_CONTENTS);
        writeInInput(noIdElement, TEXT_CONTENTS);

        relocateTo("preserveOnRefreshSecondary.html");

        // same place, same id
        Assert.assertEquals(MODIFIED + " input should have preserved state",
                TEXT_CONTENTS, getValue(MODIFIED));

        // same place, different id
        Assert.assertEquals(NEW_ID + " input should be empty", "",
                getValue(NEW_ID));

        // same place, no assigned id
        WebElement noIdElement2 = findTagWithoutId(PRESERVE_ON_REFRESH_TAG);
        Assert.assertEquals("Input without id should have preserved state",
                TEXT_CONTENTS, getValue(noIdElement2));
    }

    private String getValue(String id) {
        WebElement element = findElement(By.id(id));
        return getValue(element);
    }

    private String getValue(WebElement element) {
        return element.findElement(By.id(INTERNAL_INPUT_ID)).getAttribute("value");
    }

    private void writeInInput(String id, String text) {
        WebElement element = findElement(By.id(id));
        writeInInput(element, text);
    }

    private void writeInInput(WebElement element, String text) {
        element.findElement(By.id(INTERNAL_INPUT_ID)).sendKeys(text, Keys.ENTER);
    }

    private void refreshPage() {
        getCommandExecutor().executeScript("location.reload()");
        waitForElementVisible(By.id(MODIFIED));
    }

    private void relocateTo(String page) {
        getCommandExecutor().executeScript("location.assign('"
                + Constants.PAGE_CONTEXT + "/" + page + "')");
        waitForElementVisible(By.id(MODIFIED));
    }
    
    private WebElement findTagWithoutId(String tag) {
        List<WebElement> preserveOnRefreshElements = findElements(
                By.tagName(tag));
        return preserveOnRefreshElements.stream()
                .filter(webElement -> webElement.getAttribute("id").isEmpty())
                .findFirst().get();
    }
}
