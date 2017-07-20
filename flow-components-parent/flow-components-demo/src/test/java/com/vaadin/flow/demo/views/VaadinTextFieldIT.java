package com.vaadin.flow.demo.views;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;
import com.vaadin.ui.VaadinTextField;

/**
 * Integration tests for the {@link VaadinTextField}.
 */
public class VaadinTextFieldIT extends AbstractChromeTest {

    private WebElement layout;

    @Override
    protected String getTestPath() {
        return "/vaadin-text-field";
    }

    @Before
    public void init() {
        open();
        waitForElementPresent(By.tagName("main-layout"));
        layout = findElement(By.tagName("main-layout"));
    }

    @Test
    public void valueChangeListenerReportsCorrectValues() {
        WebElement textFieldValueDiv = layout
                .findElement(By.id("text-field-value"));
        WebElement textField = layout
                .findElement(By.id("text-field-with-value-change-listener"));

        textField.sendKeys("a");
        Assert.assertEquals(textFieldValueDiv.getText(),
                "Text field value changed from '' to 'a'");
        textField.sendKeys(Keys.BACK_SPACE);
        Assert.assertEquals(textFieldValueDiv.getText(),
                "Text field value changed from 'a' to ''");

        textField.sendKeys("abcdefg");
        new Actions(getDriver()).keyDown(Keys.ALT).sendKeys(Keys.BACK_SPACE)
                .build().perform();
        Assert.assertEquals(textFieldValueDiv.getText(),
                "Text field value changed from 'abcdefg' to ''");
    }

    @Test
    public void textFieldHasPlaceholder() {
        WebElement textField = layout
                .findElement(By.id("text-field-with-value-change-listener"));
        Assert.assertEquals(textField.getAttribute("placeholder"),
                "placeholder text");
    }
}
