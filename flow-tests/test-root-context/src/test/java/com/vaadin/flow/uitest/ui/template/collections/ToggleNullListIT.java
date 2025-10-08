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
package com.vaadin.flow.uitest.ui.template.collections;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.junit.Assert.assertFalse;

public class ToggleNullListIT extends ChromeBrowserTest {

    @Test
    public void shouldBeToggledWithNoClientErrors() {
        open();

        WebElement toggleButton = findElement(
                By.id(ToggleNullListView.TOGGLE_BUTTON_ID));

        for (int i = 0; i < 100; i++) {
            assertFalse(String.format(
                    "Failed %s the template with null list in the model after '%s' button click(s)",
                    i % 2 == 0 ? "attaching" : "reattaching", i),
                    isElementPresent(By.className("v-system-error")));
            toggleButton.click();
        }
    }
}
