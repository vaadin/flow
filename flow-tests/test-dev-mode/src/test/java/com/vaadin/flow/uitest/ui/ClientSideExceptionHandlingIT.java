/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientSideExceptionHandlingIT extends ChromeBrowserTest {

    private static final By ERROR_LOCATOR = By.className("v-system-error");

    public static final String ERROR_PATTERN = ".*TypeError.* : Cannot read properties of null .*reading 'foo'.*";

    @Test
    public void developmentModeExceptions() {
        open();
        causeException();

        String errorMessage = findElement(ERROR_LOCATOR).getText();

        Assert.assertTrue("Unexpected error message: " + errorMessage,
                Pattern.matches(ERROR_PATTERN, errorMessage));
    }

    @Test
    public void productionModeExceptions() {
        openProduction();
        causeException();

        Assert.assertFalse(isElementPresent(ERROR_LOCATOR));
    }

    private void causeException() {
        findElement(By.id(ClientSideExceptionHandlingView.CAUSE_EXCEPTION_ID))
                .click();
    }

}
