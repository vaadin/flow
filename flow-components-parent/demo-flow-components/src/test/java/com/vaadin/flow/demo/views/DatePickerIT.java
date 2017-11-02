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

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import org.openqa.selenium.By;

/**
 * Integration tests for the {@link DatePickerView}.
 */
public class DatePickerIT extends ComponentDemoTest {
    @Before
    public void init() {
        waitForElementPresent(By.tagName("vaadin-date-picker"));
    }

    @Test
    public void selectDateOnSimpleDatePicker() {
        WebElement picker = layout.findElement(By.id("simple-picker"));
        WebElement message = layout.findElement(By.id("simple-picker-message"));

        executeScript("arguments[0].value = '1985-01-10'", picker);

        waitUntil(driver -> "Day: 10\nMonth: 1\nYear: 1985"
                .equals(message.getText()));

        executeScript("arguments[0].value = ''", picker);

        waitUntil(driver -> "No date is selected".equals(message.getText()));
    }

    @Test
    public void selectDateOnMinMaxDatePicker() {
        WebElement picker = layout.findElement(By.id("min-and-max-picker"));
        WebElement message = layout
                .findElement(By.id("min-and-max-picker-message"));

        LocalDate now = LocalDate.now();
        executeScript("arguments[0].value = arguments[1]", picker,
                now.toString());

        Assert.assertEquals("The selected date should be considered valid",
                false, executeScript("return arguments[0].invalid", picker));

        waitUntil(driver -> ("Day: " + now.getDayOfMonth() + "\nMonth: "
                + now.getMonthValue() + "\nYear: " + now.getYear())
                        .equals(message.getText()));

        executeScript("arguments[0].value = ''", picker);

        waitUntil(driver -> "No date is selected".equals(message.getText()));

        Assert.assertEquals("The empty date should be considered valid", false,
                executeScript("return arguments[0].invalid", picker));

        LocalDate invalid = now.minusYears(1);

        executeScript("arguments[0].value = arguments[1]", picker,
                invalid.toString());

        Assert.assertEquals("The selected date should be considered invalid",
                true, executeScript("return arguments[0].invalid", picker));
    }

    @Test
    public void selectDateOnFinnishDatePicker() {
        WebElement picker = layout.findElement(By.id("finnish-picker"));
        WebElement message = layout
                .findElement(By.id("finnish-picker-message"));

        executeScript("arguments[0].value = '1985-01-10'", picker);

        waitUntil(driver -> "Day of week: torstai\nMonth: tammiku"
                .equals(message.getText()));

        executeScript("arguments[0].value = ''", picker);

        waitUntil(driver -> "No date is selected".equals(message.getText()));
    }

    @Test
    public void selectDatesOnLinkedDatePickers() {
        WebElement startPicker = layout.findElement(By.id("start-picker"));
        WebElement endPicker = layout.findElement(By.id("end-picker"));
        WebElement message = layout.findElement(By.id("start-and-end-message"));

        executeScript("arguments[0].value = '1985-01-10'", startPicker);

        waitUntil(driver -> "Select the ending date".equals(message.getText()));

        Assert.assertEquals(
                "The min date at the end date picker should be 1985-01-11",
                true, executeScript("return arguments[0].min === '1985-01-11'",
                        endPicker));

        executeScript("arguments[0].value = '1985-01-20'", endPicker);

        waitUntil(driver -> "Selected period:\nFrom 1985-01-10 to 1985-01-20"
                .equals(message.getText()));

        Assert.assertEquals(
                "The max date at the start date picker should be 1985-01-19",
                true, executeScript("return arguments[0].max === '1985-01-19'",
                        startPicker));

        executeScript("arguments[0].value = ''", startPicker);
        waitUntil(
                driver -> "Select the starting date".equals(message.getText()));
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-date-picker";
    }

}
