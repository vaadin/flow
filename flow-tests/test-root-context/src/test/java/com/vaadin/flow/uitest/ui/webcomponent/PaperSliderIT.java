/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.CoreMatchers.is;

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
