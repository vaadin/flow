/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ElementRemoveItselfIT extends ChromeBrowserTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void clickOnButton_removeFromLayout() {
        WebElement button = findElement(By.id("remove-me"));
        scrollIntoViewAndClick(button);

        waitForElementNotPresent(By.id("remove-me"));
        waitForElementPresent(By.id("all-removed"));
    }

}
