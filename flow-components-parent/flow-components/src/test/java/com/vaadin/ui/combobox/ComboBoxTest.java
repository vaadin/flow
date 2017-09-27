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
package com.vaadin.ui.combobox;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class ComboBoxTest {

    private static class TestComboBox extends ComboBox<String> {

        private JsonArray items;

        @Override
        protected void setItems(JsonArray items) {
            this.items = items;
        }
    }

    @Test
    public void setItems_jsonItemsAreSet() {
        TestComboBox comboBox = new TestComboBox();

        comboBox.setItems(Arrays.asList("foo", "bar"));

        Assert.assertEquals(2, comboBox.items.length());

        assertItem(comboBox, 0, "foo");
        assertItem(comboBox, 1, "bar");
    }

    @Test
    public void setItemCaptionGenerator_jsonItemsAreSet() {
        TestComboBox comboBox = new TestComboBox();

        comboBox.setItems(Arrays.asList("foo", "bar"));

        comboBox.setItemLabelGenerator(item -> String.valueOf(item.hashCode()));

        assertItem(comboBox, 0, "101574");
        assertItem(comboBox, 1, "97299");
    }

    @Test(expected = NullPointerException.class)
    public void setNull_thrownException() {
        ComboBox<Object> comboBox = new ComboBox<>();
        comboBox.setDataProvider(null);
    }

    private void assertItem(TestComboBox comboBox, int index, String caption) {
        JsonValue value1 = comboBox.items.get(index);
        Assert.assertEquals(caption,
                ((JsonObject) value1).get("label").asString());
    }
}
