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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

public class ListBindingIT extends ChromeBrowserTest {

    @Test
    public void listDataBinding() {
        open();

        WebElement template = findElement(By.id("template"));

        Optional<WebElement> msg = getInShadowRoot(template,
                By.className("msg"));

        Assert.assertEquals("foo", msg.get().getText());

        getInShadowRoot(template, By.id("update")).get().click();
        List<String> expectedResults = new ArrayList<>();
        expectedResults.add("a");
        expectedResults.add("b");
        expectedResults.add("c");
        checkResult(template, expectedResults);

        getInShadowRoot(template, By.id("addElement")).get().click();
        expectedResults.add("d1");
        checkResult(template, expectedResults);

        getInShadowRoot(template, By.id("addElementByIndex")).get().click();
        expectedResults.add(expectedResults.size() - 1, "d2");
        checkResult(template, expectedResults);

        getInShadowRoot(template, By.id("addNumerousElements")).get().click();
        expectedResults.add("e2");
        expectedResults.add("f2");
        checkResult(template, expectedResults);

        getInShadowRoot(template, By.id("addNumerousElementsByIndex")).get()
                .click();
        expectedResults.add(0, "f1");
        expectedResults.add(0, "e1");
        checkResult(template, expectedResults);
    }

    private void checkResult(WebElement template,
            List<String> expectedResults) {
        List<WebElement> msgs = findInShadowRoot(template, By.className("msg"));

        for (int i = 0; i < expectedResults.size(); i++) {
            Assert.assertEquals(expectedResults.get(i), msgs.get(i).getText());
        }
    }
}
