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
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class BeanInListingIT extends ChromeBrowserTest {

    private static final class SelectedCondition
            implements ExpectedCondition<Boolean> {

        private final WebElement selected;
        private final String expected;

        SelectedCondition(WebElement selected, String value) {
            this.selected = selected;
            expected = value;
        }

        @Override
        public Boolean apply(WebDriver driver) {
            return selected.getText().equals(expected);
        }

        @Override
        public String toString() {
            return "The value of selected element is '" + selected.getText()
                    + "', expected '" + expected + "'";
        }

    }

    @Test
    public void beanInTwoWayBinding() throws InterruptedException {
        open();

        TestBenchElement selected = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("selected");

        assertSelectionValue("user-item", selected, "foo");

        assertSelectionValue("user-item", selected, "bar");

        assertSelectionValue("msg-item", selected, "baz");

        assertSelectionValue("msg-item", selected, "msg");
    }

    private void assertSelectionValue(String className, WebElement selected,
            String item) {
        TestBenchElement template = $(TestBenchElement.class).id("template");
        List<TestBenchElement> items = template.$(TestBenchElement.class)
                .withAttribute("class", className).all();
        items.stream().filter(itemElement -> itemElement.getText().equals(item))
                .findFirst().get().click();

        waitUntil(new SelectedCondition(selected, item));
    }

}
