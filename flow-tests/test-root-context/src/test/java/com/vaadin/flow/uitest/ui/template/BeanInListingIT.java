/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
                .attribute("class", className).all();
        items.stream().filter(itemElement -> itemElement.getText().equals(item))
                .findFirst().get().click();

        waitUntil(new SelectedCondition(selected, item));
    }

}
