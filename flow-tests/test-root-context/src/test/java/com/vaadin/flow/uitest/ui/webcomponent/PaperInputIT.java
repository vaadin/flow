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
package com.vaadin.flow.uitest.ui.webcomponent;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PaperInputIT extends ChromeBrowserTest {

    @Test
    public void paperInputIsFunctional() {
        open();

        WebElement webComponent = findElement(By.tagName("paper-input"));
        Optional<WebElement> input = getInShadowRoot(webComponent,
                By.id("nativeInput"));
        input.get().sendKeys("bar");

        List<WebElement> updateValueElements = findElements(
                By.className("update-value"));
        WebElement lastUpdateValue = updateValueElements
                .get(updateValueElements.size() - 1);
        org.junit.Assert.assertEquals("bar", lastUpdateValue.getText());
    }
}
