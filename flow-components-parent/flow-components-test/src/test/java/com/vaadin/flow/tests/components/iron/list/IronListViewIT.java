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
package com.vaadin.flow.tests.components.iron.list;

import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNull;
import elemental.json.JsonObject;

public class IronListViewIT extends AbstractComponentIT {

    @Before
    public void init() {
        open();
        waitUntil(driver -> findElements(By.tagName("iron-list")).size() > 0);
    }

    @Test
    public void listWithStrings() {
        testListOrDataProvider("list-with-strings", "list-with-strings-3-items",
                "list-with-strings-2-items", "list-with-strings-0-items",
                "Item ", "Another item ");
    }

    @Test
    public void dataProviderWithStrings() {
        testListOrDataProvider("dataprovider-with-strings",
                "dataprovider-with-strings-3-items",
                "dataprovider-with-strings-2-items",
                "dataprovider-with-strings-0-items", "Item ", "Another item ");
    }

    @Test
    public void templateFromValueProviderWithPeople() {
        testListOrDataProvider("dataprovider-with-people",
                "dataprovider-with-people-3-items",
                "dataprovider-with-people-2-items",
                "dataprovider-with-people-0-items", "Person ", "");
    }

    @Test
    public void templateFromRendererWithPeople() {
        WebElement list = findElement(By.id("template-renderer-with-people"));

        JsonArray items = getItems(list);
        Assert.assertEquals(3, items.length());
        for (int i = 0; i < items.length(); i++) {
            Assert.assertEquals(String.valueOf(i + 1),
                    items.getObject(i).getString("key"));
            Assert.assertEquals("Person " + (i + 1),
                    items.getObject(i).getString("name"));
            Assert.assertEquals(String.valueOf(i + 1),
                    items.getObject(i).getString("age"));
            Assert.assertEquals("person_" + (i + 1),
                    items.getObject(i).getString("user"));
        }

        WebElement update = findElement(
                By.id("template-renderer-with-people-update-item"));

        scrollIntoViewAndClick(update);
        items = getItems(list);
        JsonObject person = items.getObject(0);
        Assert.assertEquals("Person 1 Updated", person.getString("name"));
        Assert.assertEquals("person_1_updated", person.getString("user"));
    }

    @Test
    public void lazyLoaded() {
        WebElement list = findElement(By.id("lazy-loaded"));
        WebElement message = findElement(By.id("lazy-loaded-message"));

        JsonArray items = getItems(list);
        // the items are preallocated in the list, but they are empty
        Assert.assertEquals(100, items.length());

        // default initial request for this size of list (100px height)
        Assert.assertEquals("Sent 32 items", message.getText());

        for (int i = 0; i < 32; i++) {
            if (items.get(i) instanceof JsonNull) {
                Assert.fail("Object at index " + i + " is null");
            }
            Assert.assertEquals(String.valueOf(i + 1),
                    items.getObject(i).getString("key"));
            Assert.assertEquals("Item " + (i + 1),
                    items.getObject(i).getString("label"));
        }

        // all the remaining items should be empty
        for (int i = 32; i < items.length(); i++) {
            Assert.assertThat(items.get(i),
                    CoreMatchers.instanceOf(JsonNull.class));
        }

        // scrolls all the way down
        executeScript("arguments[0].scrollBy(0,10000);", list);
        waitUntil(driver -> getItems(list).get(0) instanceof JsonNull);

        items = getItems(list);

        // all the initial items should be empty
        for (int i = 0; i < items.length() - 32; i++) {
            Assert.assertThat(items.get(i),
                    CoreMatchers.instanceOf(JsonNull.class));
        }

        // the last 32 items should have data
        for (int i = items.length() - 32; i < items.length(); i++) {
            if (items.get(i) instanceof JsonNull) {
                Assert.fail("Object at index " + i + " is null");
            }
            Assert.assertEquals("Item " + (i + 1),
                    items.getObject(i).getString("label"));
        }
    }

    private void testListOrDataProvider(String listId, String button1Id,
            String button2Id, String button3Id, String labelPrefix1,
            String labelPrefix2) {
        WebElement list = findElement(By.id(listId));

        JsonArray items = getItems(list);
        Assert.assertEquals(3, items.length());
        for (int i = 0; i < items.length(); i++) {
            Assert.assertEquals(String.valueOf(i + 1),
                    items.getObject(i).getString("key"));
            Assert.assertEquals(labelPrefix1 + (i + 1),
                    items.getObject(i).getString("label"));
        }

        WebElement button1 = findElement(By.id(button1Id));
        WebElement button2 = findElement(By.id(button2Id));
        WebElement button3 = findElement(By.id(button3Id));

        scrollIntoViewAndClick(button2);
        waitUntil(driver -> getItems(list).length() == 2);
        items = getItems(list);
        for (int i = 0; i < items.length(); i++) {
            // the keys are incremental
            Assert.assertEquals(String.valueOf(i + 3 + 1),
                    items.getObject(i).getString("key"));
            Assert.assertEquals(labelPrefix2 + (i + 1),
                    items.getObject(i).getString("label"));
        }

        scrollIntoViewAndClick(button1);
        waitUntil(driver -> getItems(list).length() == 3);
        items = getItems(list);
        for (int i = 0; i < items.length(); i++) {
            // the keys are incremental
            Assert.assertEquals(String.valueOf(i + 5 + 1),
                    items.getObject(i).getString("key"));
            Assert.assertEquals(labelPrefix1 + (i + 1),
                    items.getObject(i).getString("label"));
        }

        scrollIntoViewAndClick(button3);
        waitUntil(driver -> getItems(list).length() == 0);
    }

    private JsonArray getItems(WebElement element) {
        Object result = executeScript("return arguments[0].items;", element);
        JsonArray array = Json.createArray();
        if (!(result instanceof List)) {
            return array;
        }
        List<Map<String, ?>> list = (List<Map<String, ?>>) result;
        for (int i = 0; i < list.size(); i++) {
            Map<String, ?> map = list.get(i);
            if (map != null) {
                JsonObject obj = Json.createObject();
                map.entrySet().forEach(entry -> {
                    obj.put(entry.getKey(), String.valueOf(entry.getValue()));
                });
                array.set(i, obj);
            } else {
                array.set(i, Json.createNull());
            }
        }
        return array;
    }

}
