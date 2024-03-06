/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.webcomponent;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PaperSliderIT extends ChromeBrowserTest {

    @Test
    public void domCorrect() {
        open();

        WebElement eventField = findElement(
                By.id(PaperSliderView.VALUE_TEXT_ID));
        Assert.assertNotNull("No text value found on the page", eventField);

        WebElement paperSlider = findElement(By.tagName("paper-slider"));
        Assert.assertNotNull("No slider found on the page", paperSlider);

        int initialValue = PaperSliderView.INITIAL_VALUE;

        assertSliderValue(paperSlider, initialValue);

        changeSliderValueViaApi(eventField, paperSlider, initialValue + 1);
        changeSliderValueViaButton(eventField, paperSlider,
                PaperSliderView.UPDATED_VALUE);
    }

    private void changeSliderValueViaApi(WebElement eventField,
            WebElement paperSlider, int expectedValue) {
        executeScript("arguments[0].increment()", paperSlider);
        assertSliderValue(paperSlider, expectedValue);
        assertEventFieldValue(eventField, expectedValue);
    }

    private void changeSliderValueViaButton(WebElement eventField,
            WebElement paperSlider, int expectedValue) {
        findElement(By.id(PaperSliderView.CHANGE_VALUE_ID)).click();
        assertSliderValue(paperSlider, expectedValue);
        assertEventFieldValue(eventField, expectedValue);
    }

    private static void assertSliderValue(WebElement paperSlider,
            int expectedValue) {
        Assert.assertThat("Slider has incorrect value",
                Integer.valueOf(paperSlider.getAttribute("value")),
                is(expectedValue));
    }

    private static void assertEventFieldValue(WebElement eventField,
            int expectedValue) {
        Assert.assertThat(
                "Expected event field to be updated after slider value was changed",
                eventField.getText(),
                is(String.format("Value: %s (set on client)", expectedValue)));
    }
}
