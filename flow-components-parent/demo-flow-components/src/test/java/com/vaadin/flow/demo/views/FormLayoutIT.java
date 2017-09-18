/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo.views;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;

/**
 * Integration tests for the {@link FormLayoutView}.
 */
public class FormLayoutIT extends AbstractChromeTest {

    private WebElement layout;

    @Before
    public void init() {
        open();
        waitForElementPresent(By.tagName("main-layout"));
        layout = findElement(By.tagName("main-layout"));
        Assert.assertTrue(isElementPresent(By.tagName("vaadin-form-layout")));
    }

    @Test
    public void custom_responsive_layouting() {
        WebElement firstLayout = layout
                .findElement(By.tagName("vaadin-form-layout"));
        List<WebElement> textFields = firstLayout
                .findElements(By.tagName("vaadin-text-field"));
        Assert.assertEquals(3, textFields.size());

        getDriver().manage().window().setSize(new Dimension(1000, 1000));

        // 3 columns, all should be horizontally aligned (tolerance of 2 pixels
        // given)
        Assert.assertTrue("All 3 columns should be horizontally aligned",
                Math.abs(textFields.get(2).getLocation().getY()
                        - textFields.get(1).getLocation().getY()) < 2);
        Assert.assertTrue(Math.abs(textFields.get(1).getLocation().getY()
                - textFields.get(0).getLocation().getY()) < 2);

        getDriver().manage().window().setSize(new Dimension(450, 620));

        // window resized, should be in 2 column mode, last textfield below
        // other two
        Assert.assertTrue(
                "Layout should be in 2 column mode, last field should be below the first two",
                textFields.get(2).getLocation().getY() > textFields.get(1)
                        .getLocation().getY());
        Assert.assertTrue(textFields.get(2).getLocation().getY() > textFields
                .get(0).getLocation().getY());

        getDriver().manage().window().setSize(new Dimension(400, 620));

        // resized to 1 column mode, fields should be arranged below one another
        Assert.assertTrue(
                "Layout should be in 1 column mode, all fields should be below one another",
                textFields.get(2).getLocation().getY() > textFields.get(1)
                        .getLocation().getY());
        Assert.assertTrue(textFields.get(1).getLocation().getY() > textFields
                .get(0).getLocation().getY());
    }

    @Test
    public void form_with_binder() {
        // Empty form validation: there is an error
        WebElement info = findElement(By.id("binder-info"));
        WebElement save = findElement(By.id("binder-save"));
        scrollIntoViewAndClick(save);

        waitUntil(
                driver -> "There are errors: Both phone and email cannot be empty, Please add the first name, Please add the last name"
                        .equals(info.getText()));

        // Fill form: there shouldn't be an error
        findFirstNameInput().sendKeys("foo");
        findLastNameInput().sendKeys("bar");
        findPhoneInput().sendKeys("123-456-789");
        findEmailInput().sendKeys("example@foo.bar");
        findBirthDayInput().sendKeys("01/02/2003");
        findBirthDayInput().sendKeys(Keys.ENTER);

        WebElement doNotCall = findElement(By.id("binder-do-not-call"));
        WebElement checkBox = getInShadowRoot(doNotCall,
                By.id("nativeCheckbox"));
        scrollIntoViewAndClick(checkBox);
        scrollIntoViewAndClick(save);

        waitUntil(driver -> info.getText().startsWith("Saved bean values"));

        Assert.assertTrue(info.getText().contains("foo bar"));
        Assert.assertTrue(info.getText()
                .contains(", phone 123-456-789 (don't call me!)"));
        Assert.assertTrue(info.getText().contains(", e-mail example@foo.bar"));
        Assert.assertTrue(info.getText().contains(", born on 2003-01-02"));

        // Make email address incorrect
        findEmailInput().clear();
        findEmailInput().sendKeys("abc");
        scrollIntoViewAndClick(save);

        waitUntil(driver -> info.getText().startsWith("There are errors"));
        Assert.assertEquals("There are errors: Incorrect email address",
                info.getText());

        // there's a bug preventing invalid fields from being cleared. See
        // https://github.com/vaadin/flow-demo/issues/344
        findEmailInput().clear();

        // reset
        scrollIntoViewAndClick(findElement(By.id("binder-reset")));

        // Wait for everything to update.
        waitUntil(driver -> info.getText().isEmpty());

        Assert.assertEquals("", findFirstNameInput().getAttribute("value"));
        Assert.assertEquals("", findLastNameInput().getAttribute("value"));
        Assert.assertEquals("", findPhoneInput().getAttribute("value"));
        Assert.assertEquals("", findEmailInput().getAttribute("value"));
        Assert.assertEquals("", findBirthDayInput().getAttribute("value"));
        Assert.assertFalse(checkBox.isSelected());
    }

    private WebElement findBirthDayInput() {
        return getInShadowRoot(findElement(By.id("binder-birth-date")),
                By.id("input"));
    }

    private WebElement findEmailInput() {
        return getInShadowRoot(findElement(By.id("binder-email")),
                By.id("input"));
    }

    private WebElement findLastNameInput() {
        return getInShadowRoot(findElement(By.id("binder-last-name")),
                By.id("input"));
    }

    private WebElement findFirstNameInput() {
        return getInShadowRoot(findElement(By.id("binder-first-name")),
                By.id("input"));
    }

    private WebElement findPhoneInput() {
        return getInShadowRoot(findElement(By.id("binder-phone")),
                By.id("input"));
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-form-layout";
    }
}
