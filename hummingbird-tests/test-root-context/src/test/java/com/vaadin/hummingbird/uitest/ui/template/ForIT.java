/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class ForIT extends PhantomJSTest {
    @Test
    public void testNgFor() {
        open();

        Assert.assertEquals(0, getItems().size());

        click("add");

        Assert.assertEquals(1, getItems().size());
        Assert.assertEquals("Item 0", getItems().get(0).getText());

        click("add");
        click("update");

        Assert.assertEquals(2, getItems().size());
        Assert.assertEquals("Item 1 updated", getItems().get(1).getText());

        click("remove");

        Assert.assertEquals(1, getItems().size());
        Assert.assertEquals("Item 0", getItems().get(0).getText());
    }

    private void click(String buttonId) {
        findElement(By.id(buttonId)).click();
    }

    private List<WebElement> getItems() {
        return findElements(By.className("item"));
    }

}
