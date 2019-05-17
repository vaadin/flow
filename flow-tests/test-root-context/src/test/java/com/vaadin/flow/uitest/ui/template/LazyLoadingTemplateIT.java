/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreNPM;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreNPM.class)
public class LazyLoadingTemplateIT extends ChromeBrowserTest {

    @Test
    public void lazyLoadedTemplateBehavior() {
        open();

        Assert.assertTrue(isElementPresent(By.id("initial-div")));
        testBench().disableWaitForVaadin();

        waitUntil(driver -> templateIsLoaded());

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement valueDiv = template.$(TestBenchElement.class)
                .id("msg");
        Assert.assertEquals("foo", valueDiv.getText());

        testBench().enableWaitForVaadin();
        WebElement input = template.$(TestBenchElement.class).id("input");
        input.clear();
        input.sendKeys("bar");
        input.sendKeys(Keys.ENTER);
        List<WebElement> updated = findElements(By.className("updated"));
        Assert.assertEquals("bar", updated.get(updated.size() - 1).getText());
    }

    private boolean templateIsLoaded() {
        Assert.assertTrue(isElementPresent(By.id("initial-div")));
        Assert.assertTrue(findElement(By.id("initial-div")).isDisplayed());
        List<TestBenchElement> template = $(TestBenchElement.class)
                .attribute("id", "template").all();
        return !template.isEmpty() && !template.get(0).$(TestBenchElement.class)
                .attribute("id", "msg").all().isEmpty();
    }
}
