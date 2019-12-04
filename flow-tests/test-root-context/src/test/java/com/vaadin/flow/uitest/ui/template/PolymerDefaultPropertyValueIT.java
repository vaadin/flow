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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PolymerDefaultPropertyValueIT extends ChromeBrowserTest {

    @Test
    public void initialModelValues_polymerHasDefaultValues() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement text = template.$(TestBenchElement.class).id("text");

        Assert.assertEquals("foo", text.getText());

        TestBenchElement name = template.$(TestBenchElement.class).id("name");
        Assert.assertEquals("bar", name.getText());

        TestBenchElement msg = template.$(TestBenchElement.class).id("message");
        Assert.assertEquals("updated-message", msg.getText());

        TestBenchElement email = template.$(TestBenchElement.class).id("email");
        Assert.assertEquals("foo@example.com", email.getText());

        findElement(By.id("show-email")).click();

        WebElement serverSideEmailValue = findElement(By.id("email-value"));
        Assert.assertEquals("foo@example.com", serverSideEmailValue.getText());
    }
}
