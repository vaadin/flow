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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class AfterServerChangesIT extends ChromeBrowserTest {

    @Test
    public void notifyServerUpdateOnTheClientSide() {
        open();

        List<TestBenchElement> components = $("after-server-changes").all();
        components.forEach(component -> assertAfterServerUpdate(component, 1));

        WebElement update = findElement(By.id("update"));

        update.click();

        components.forEach(component -> assertAfterServerUpdate(component, 2));

        findElement(By.id("remove")).click();

        update.click();

        // The second components is removed
        // No exceptions , everything is functional
        assertAfterServerUpdate($("after-server-changes").first(), 3);
    }

    private void assertAfterServerUpdate(TestBenchElement element, int i) {
        WebElement count = element.$(TestBenchElement.class).id("count");
        Assert.assertEquals(String.valueOf(i), count.getText());

        WebElement delta = element.$(TestBenchElement.class).id("delta");
        Assert.assertEquals(Boolean.TRUE.toString(), delta.getText());
    }
}
