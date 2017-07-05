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
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class VaadinGridIT extends ChromeBrowserTest {

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

        WebElement template = findElement(By.id("template"));
        WebElement usersTable = getInShadowRoot(template, By.id("users"));
        clickCell(usersTable, "foo");

        WebElement selected = getInShadowRoot(template, By.id("selected"));

        assertSelectionValue(usersTable, selected, "foo");

        assertSelectionValue(usersTable, selected, "bar");

        WebElement msgsTable = getInShadowRoot(template, By.id("messages"));

        assertSelectionValue(msgsTable, selected, "baz");

        assertSelectionValue(msgsTable, selected, "msg");
    }

    private void assertSelectionValue(WebElement table, WebElement selected,
            String item) {
        clickCell(table, item);
        waitUntil(driver -> new SelectedCondition(selected, item));
    }

    private void clickCell(WebElement table, String cellValue) {
        List<WebElement> cells = table
                .findElements(By.cssSelector("vaadin-grid-cell-content"));

        cells.stream().filter(element -> element.getText().equals(cellValue))
                .findFirst().get().click();
    }
}
