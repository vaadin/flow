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
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

public class ListBindingIT extends ChromeBrowserTest {

    @Test
    public void listDataBindng() {
        open();

        WebElement webComponent = findElement(By.id("template"));

        Optional<WebElement> msg = getInShadowRoot(webComponent,
                By.className("msg"));

        Assert.assertEquals("foo", msg.get().getText());

        getInShadowRoot(webComponent, By.id("update")).get().click();

        List<WebElement> msgs = findInShadowRoot(webComponent,
                By.className("msg"));

        Assert.assertEquals("a", msgs.get(0).getText());
        Assert.assertEquals("b", msgs.get(1).getText());
        Assert.assertEquals("c", msgs.get(2).getText());
    }
}
