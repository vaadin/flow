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
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

/**
 * Normal tests with @Before are not implemented because each @Test starts new
 * Chrome process.
 */
public class ListInsideListBindingIT extends ChromeBrowserTest {

    @Test
    public void listDataBinding() {
        open();

        WebElement template = findElement(By.id("template"));

        checkListInsideList(template);
    }

    private void checkListInsideList(WebElement template) {
        List<WebElement> msgs = findInShadowRoot(template, By.className("submsg"));
        Assert.assertEquals("Wrong amount of nested messages", 4, msgs.size());

        msgs.get(1).click();

        Assert.assertEquals("Couldn't validate list selection.",
                findElement(By.id("multi-selection")).getText(),
                "Clicked message List: 3 abc");

        msgs.get(3).click();

        Assert.assertEquals("Couldn't validate list selection.",
                findElement(By.id("multi-selection")).getText(),
                "Clicked message List: 1 d");
    }
}
