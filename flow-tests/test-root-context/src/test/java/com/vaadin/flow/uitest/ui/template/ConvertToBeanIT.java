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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ConvertToBeanIT extends ChromeBrowserTest {

    @Test
    public void convertToBean_valuesAreUpdated() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        template.$(TestBenchElement.class).id("day").sendKeys("2");
        template.$(TestBenchElement.class).id("month").sendKeys("5");
        template.$(TestBenchElement.class).id("year").sendKeys("2000");

        template.$(TestBenchElement.class).id("click").click();

        String text = template.$(TestBenchElement.class).id("msg").getText();
        Assert.assertEquals("02.05.2000", text);
    }
}
