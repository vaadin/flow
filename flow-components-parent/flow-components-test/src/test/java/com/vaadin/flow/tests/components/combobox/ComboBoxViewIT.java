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
package com.vaadin.flow.tests.components.combobox;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;

public class ComboBoxViewIT extends AbstractComponentIT {

    @Test
    public void dataProvider_itemCaptionGenerator() {
        open();

        WebElement combo = findElement(By.id("combo"));

        List<?> items = getItems(combo);
        Assert.assertEquals(2, items.size());
        assertItem(items, 0, "foo");
        assertItem(items, 1, "bar");

        // update data provider
        findElement(By.id("update-provider")).click();

        waitUntil(driver -> "baz".equals(getItem(getItems(combo), 0)));
        assertItem(getItems(combo), 1, "foobar");

        // update item caption generator
        findElement(By.id("update-caption-gen")).click();

        waitUntil(driver -> "3".equals(getItem(getItems(combo), 0)));
        assertItem(getItems(combo), 1, "6");
    }

    private List<?> getItems(WebElement combo) {
        List<?> items = (List<?>) getCommandExecutor()
                .executeScript("return arguments[0].items;", combo);
        return items;
    }

    private void assertItem(List<?> items, int index, String caption) {
        Map<?, ?> map = (Map<?, ?>) items.get(index);
        Assert.assertEquals(caption, map.get("label"));
    }

    private Object getItem(List<?> items, int index) {
        Map<?, ?> map = (Map<?, ?>) items.get(index);
        return map.get("label");
    }
}
