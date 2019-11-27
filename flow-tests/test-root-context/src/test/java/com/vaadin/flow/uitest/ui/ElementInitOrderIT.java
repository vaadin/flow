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

import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ElementInitOrderIT extends ChromeBrowserTest {

    @Test
    public void elementInitOrder() throws URISyntaxException {
        open();

        assertInitOrder();

        findElement(By.id("reattach")).click();

        assertInitOrder();
    }

    private void assertInitOrder() {
        for (String name : Arrays.asList("init-order-polymer",
                "init-order-nopolymer")) {
            TestBenchElement element = $(name).first();
            String status = element.$(TestBenchElement.class).id("status")
                    .getText();
            Assert.assertEquals(
                    "property = property, attribute = attribute, child count = 1, style = style, class = class",
                    status);
        }
    }
}
