package com.vaadin.flow.uitest.ui;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

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
