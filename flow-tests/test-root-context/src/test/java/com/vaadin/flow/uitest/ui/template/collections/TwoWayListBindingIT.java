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
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TwoWayListBindingIT extends ChromeBrowserTest {

    @Test
    public void itemsInList_twoWayDataBinding_updatesAreSentToServer() {
        open();

        findElement(By.id("enable")).click();

        List<WebElement> fields = $("two-way-list-binding").first()
                .$(DivElement.class).first().findElements(By.id("input"));

        // self check
        Assert.assertEquals(2, fields.size());

        fields.get(0).clear();
        fields.get(0).sendKeys("baz");
        fields.get(0).sendKeys(Keys.TAB);

        Assert.assertEquals("[baz, bar]", getLastInfoMessage());

        fields.get(1).sendKeys("foo");
        fields.get(1).sendKeys(Keys.TAB);

        Assert.assertEquals("[baz, barfoo]", getLastInfoMessage());
    }

    private String getLastInfoMessage() {
        List<WebElement> messages = findElements(By.className("messages"));
        // self check
        Assert.assertTrue(messages.size() > 0);
        return messages.get(messages.size() - 1).getText();
    }
}
