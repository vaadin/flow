/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import elemental.json.Json;
import elemental.json.JsonArray;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ThemeValidationUtilTest {

    @Test
    public void testObjectsIncludeMethodWithSameElementsInArrays() {
        JsonArray jsonFromBundle = createJsonArray("a", "b", "c");
        JsonArray projectJson = createJsonArray("a", "b", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                projectJson, missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodWithSameElementsInArraysDifferentOrder() {
        JsonArray jsonFromBundle = createJsonArray("a", "b", "c");
        JsonArray projectJson = createJsonArray("b", "a", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                projectJson, missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodArraysAreDifferent() {
        List<String> missedKeysForBundle = new ArrayList<>();
        JsonArray jsonFromBundle = createJsonArray("a", "c");
        JsonArray jsonFromProject = createJsonArray("a", "b", "c");

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                jsonFromProject, missedKeysForBundle);
        Assert.assertFalse(result);
        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a JsonArray the keys are quoted
        Assert.assertEquals(missedKeysForBundle, List.of("\"b\""));

        List<String> missedKeysForProject = new ArrayList<>();
        jsonFromBundle = createJsonArray("a", "b", "c");
        jsonFromProject = createJsonArray("a");
        result = ThemeValidationUtil.objectIncludesEntry(jsonFromProject,
                jsonFromBundle, missedKeysForProject);
        Assert.assertFalse(result);
        Assert.assertEquals(missedKeysForProject, List.of("\"b\"", "\"c\""));
    }

    @Test
    public void testObjectsIncludeMethodBothEmptyArraysAreEmpty() {
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(
                Json.createArray(), Json.createArray(), missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodOneArrayIsEmpty() {
        List<String> missedKeysFromProject = new ArrayList<>();
        JsonArray jsonFromBundle = createJsonArray("a", "b", "c");
        JsonArray jsonFromProjectEmpty = createJsonArray();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                jsonFromProjectEmpty, missedKeysFromProject);
        Assert.assertFalse(result);

        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a JsonArray the keys are quoted
        Assert.assertEquals(missedKeysFromProject,
                List.of("\"a\"", "\"b\"", "\"c\""));

        List<String> missedKeysFromBundle = new ArrayList<>();
        JsonArray jsonFromProject = createJsonArray("a", "b", "c");
        JsonArray jsonFromBundleEmpty = createJsonArray();

        result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundleEmpty,
                jsonFromProject, missedKeysFromBundle);
        Assert.assertFalse(result);
        Assert.assertEquals(missedKeysFromBundle,
                List.of("\"a\"", "\"b\"", "\"c\""));
    }

    private JsonArray createJsonArray(String... values) {
        JsonArray array = Json.createArray();
        for (String value : values) {
            array.set(array.length(), value);
        }
        return array;
    }
}
