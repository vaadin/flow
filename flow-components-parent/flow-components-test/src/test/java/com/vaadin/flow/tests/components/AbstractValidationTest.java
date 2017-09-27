package com.vaadin.flow.tests.components;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.testbench.By;

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

        WebElement body = findElement(By.tagName("body"));
        scrollIntoViewAndClick(body);

        assertInvalid();
    }

    @Test
    public void invalidateWhenNotEmptyAndThenBlur() {
        setValue("not-empty");
        scrollIntoViewAndClick(invalidate);
        scrollIntoViewAndClick(field);

        WebElement body = findElement(By.tagName("body"));
        scrollIntoViewAndClick(body);

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
