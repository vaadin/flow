/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui.webcomponent;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class PaperSliderIT extends PhantomJSTest {

    @Test
    public void domCorrect() {
        open();
        List<WebElement> progress = findElements(By.xpath("//paper-progress"));
        // Paper-slider uses <paper-progress> internally so if everything is
        // setup correctly, it should be found in the DOM
        Assert.assertEquals(1, progress.size());
    }

    @Test
    public void changeValueFromServer() {
        open();
        WebElement changeValue = findElement(
                By.id(PaperSliderView.CHANGE_VALUE_ID));
        changeValue.click();
        WebElement valueText = findElement(
                By.id(PaperSliderView.VALUE_TEXT_ID));

        // This should really be (set on server).
        // Depends on https://github.com/vaadin/hummingbird/issues/792
        Assert.assertEquals("Value: 50 (set on client)", valueText.getText());
    }

    @Test
    public void changeValueFromClientByJSApi() {
        open();
        WebElement paperSlider = findElement(By.xpath("//paper-slider"));
        executeScript("arguments[0].increment()", paperSlider);

        WebElement valueText = findElement(
                By.id(PaperSliderView.VALUE_TEXT_ID));
        Assert.assertEquals("Value: 76 (set on client)", valueText.getText());
    }
}
