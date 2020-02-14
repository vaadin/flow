/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class BasicTypeInListIT extends ChromeBrowserTest {

    @Test
    public void basicTypeInModeList() {
        open();
        TestBenchElement template = $(TestBenchElement.class).id("template");
        List<TestBenchElement> items = template.$(TestBenchElement.class)
                .attribute("class", "item").all();

        Assert.assertEquals(2, items.size());
        Assert.assertEquals("foo", items.get(0).getText());
        Assert.assertEquals("bar", items.get(1).getText());

        findElement(By.id("add")).click();

        items = template.$(TestBenchElement.class).attribute("class", "item")
                .all();

        Assert.assertEquals(3, items.size());
        Assert.assertEquals("newItem", items.get(2).getText());

        findElement(By.id("remove")).click();

        items = template.$(TestBenchElement.class).attribute("class", "item")
                .all();

        Assert.assertEquals(2, items.size());
        Assert.assertEquals("bar", items.get(0).getText());
    }
}
