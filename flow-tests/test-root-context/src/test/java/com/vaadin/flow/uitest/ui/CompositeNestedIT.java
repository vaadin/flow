/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CompositeNestedIT extends ChromeBrowserTest {

    @Test
    public void testBasics() {
        open();
        WebElement name = findElement(By.id(CompositeNestedView.NAME_ID));
        InputTextElement input = $(InputTextElement.class)
                .id(CompositeNestedView.NAME_FIELD_ID);
        Assert.assertEquals("Name on server:", name.getText());
        input.setValue("123");
        Assert.assertEquals("Name on server: 123", name.getText());
    }
}
