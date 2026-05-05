/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeValidationUtilTest {

    @Test
    void testObjectsIncludeMethodWithSameElementsInArrays() {
        ArrayNode jsonFromBundle = createArrayNode("a", "b", "c");
        ArrayNode projectJson = createArrayNode("a", "b", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                projectJson, missedKeys);
        assertTrue(result);
        assertTrue(missedKeys.isEmpty());
    }

    @Test
    void testObjectsIncludeMethodWithSameElementsInArraysDifferentOrder() {
        ArrayNode jsonFromBundle = createArrayNode("a", "b", "c");
        ArrayNode projectJson = createArrayNode("b", "a", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                projectJson, missedKeys);
        assertTrue(result);
        assertTrue(missedKeys.isEmpty());
    }

    @Test
    void testObjectsIncludeMethodArraysAreDifferent() {
        List<String> missedKeysForBundle = new ArrayList<>();
        ArrayNode jsonFromBundle = createArrayNode("a", "c");
        ArrayNode jsonFromProject = createArrayNode("a", "b", "c");

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                jsonFromProject, missedKeysForBundle);
        assertFalse(result);
        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a ArrayNode the keys are quoted
        assertEquals(missedKeysForBundle, List.of("\"b\""));

        List<String> missedKeysForProject = new ArrayList<>();
        jsonFromBundle = createArrayNode("a", "b", "c");
        jsonFromProject = createArrayNode("a");
        result = ThemeValidationUtil.objectIncludesEntry(jsonFromProject,
                jsonFromBundle, missedKeysForProject);
        assertFalse(result);
        assertEquals(missedKeysForProject, List.of("\"b\"", "\"c\""));
    }

    @Test
    void testObjectsIncludeMethodBothEmptyArraysAreEmpty() {
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(
                JacksonUtils.createArrayNode(), JacksonUtils.createArrayNode(),
                missedKeys);
        assertTrue(result);
        assertTrue(missedKeys.isEmpty());
    }

    @Test
    void testObjectsIncludeMethodOneArrayIsEmpty() {
        List<String> missedKeysFromProject = new ArrayList<>();
        ArrayNode jsonFromBundle = createArrayNode("a", "b", "c");
        ArrayNode jsonFromProjectEmpty = createArrayNode();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                jsonFromProjectEmpty, missedKeysFromProject);
        assertFalse(result);

        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a ArrayNode the keys are quoted
        assertEquals(missedKeysFromProject, List.of("\"a\"", "\"b\"", "\"c\""));

        List<String> missedKeysFromBundle = new ArrayList<>();
        ArrayNode jsonFromProject = createArrayNode("a", "b", "c");
        ArrayNode jsonFromBundleEmpty = createArrayNode();

        result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundleEmpty,
                jsonFromProject, missedKeysFromBundle);
        assertFalse(result);
        assertEquals(missedKeysFromBundle, List.of("\"a\"", "\"b\"", "\"c\""));
    }

    private ArrayNode createArrayNode(String... values) {
        ArrayNode array = JacksonUtils.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
