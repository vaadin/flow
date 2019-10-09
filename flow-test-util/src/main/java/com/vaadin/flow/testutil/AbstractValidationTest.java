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
package com.vaadin.flow.testutil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.HasValidation;

/**
 * Base class for validation tests of components that implement
 * {@link HasValidation}.
 * 
 * @see ValidationTestView
 * @since 1.0
 */
public abstract class AbstractValidationTest extends AbstractComponentIT {

    private WebElement field;
    private WebElement invalidate;
    private WebElement validate;

    @Before
    public void init() {
        open();

        waitForElementPresent(By.id("field"));
        field = findElement(By.id("field"));
        invalidate = findElement(By.id("invalidate"));
        validate = findElement(By.id("validate"));
    }

    @Test
    public void invalidateWhenEmpty() {
        scrollIntoViewAndClick(invalidate);
        assertInvalid();
    }

    @Test
    public void invalidateWhenNotEmpty() {
        setValue("not-empty");
        scrollIntoViewAndClick(invalidate);
        assertInvalid();
    }

    @Test
    public void invalidateAndValidateAgain() {
        scrollIntoViewAndClick(invalidate);
        assertInvalid();
        scrollIntoViewAndClick(validate);
        assertValid();
        scrollIntoViewAndClick(invalidate);
        assertInvalid();
    }

    @Test
    public void invalidatewhenEmptyAndThenBlur() {
        scrollIntoViewAndClick(invalidate);
        scrollIntoViewAndClick(field);

        executeScript("document.body.click()");

        assertInvalid();
    }

    @Test
    public void invalidateWhenNotEmptyAndThenBlur() {
        setValue("not-empty");
        scrollIntoViewAndClick(invalidate);
        scrollIntoViewAndClick(field);

        executeScript("document.body.click()");

        assertInvalid();
    }

    private void assertInvalid() {
        String invalid = field.getAttribute("invalid");
        Assert.assertTrue("The element should be in invalid state",
                Boolean.parseBoolean(invalid));

        String errorMessage = field.getAttribute("errorMessage");
        Assert.assertEquals("Invalidated from server", errorMessage);
    }

    private void assertValid() {
        String invalid = field.getAttribute("invalid");
        Assert.assertFalse("The element should be in valid state",
                Boolean.parseBoolean(invalid));

        String errorMessage = field.getAttribute("errorMessage");
        Assert.assertEquals("", errorMessage);
    }

    private void setValue(String value) {
        executeScript("arguments[0].value = arguments[1];", field, value);
    }

}
