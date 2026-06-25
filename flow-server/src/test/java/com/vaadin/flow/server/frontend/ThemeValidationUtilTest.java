/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.internal.JacksonUtils;

public class ThemeValidationUtilTest {

    @Test
    public void testObjectsIncludeMethodWithSameElementsInArrays() {
        ArrayNode jsonFromBundle = createArrayNode("a", "b", "c");
        ArrayNode projectJson = createArrayNode("a", "b", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                projectJson, missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodWithSameElementsInArraysDifferentOrder() {
        ArrayNode jsonFromBundle = createArrayNode("a", "b", "c");
        ArrayNode projectJson = createArrayNode("b", "a", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                projectJson, missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodArraysAreDifferent() {
        List<String> missedKeysForBundle = new ArrayList<>();
        ArrayNode jsonFromBundle = createArrayNode("a", "c");
        ArrayNode jsonFromProject = createArrayNode("a", "b", "c");

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                jsonFromProject, missedKeysForBundle);
        Assert.assertFalse(result);
        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a ArrayNode the keys are quoted
        Assert.assertEquals(missedKeysForBundle, List.of("\"b\""));

        List<String> missedKeysForProject = new ArrayList<>();
        jsonFromBundle = createArrayNode("a", "b", "c");
        jsonFromProject = createArrayNode("a");
        result = ThemeValidationUtil.objectIncludesEntry(jsonFromProject,
                jsonFromBundle, missedKeysForProject);
        Assert.assertFalse(result);
        Assert.assertEquals(missedKeysForProject, List.of("\"b\"", "\"c\""));
    }

    @Test
    public void testObjectsIncludeMethodBothEmptyArraysAreEmpty() {
        List<String> missedKeys = new ArrayList<>();

        boolean result = ThemeValidationUtil.objectIncludesEntry(
                JacksonUtils.createArrayNode(), JacksonUtils.createArrayNode(),
                missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodOneArrayIsEmpty() {
        List<String> missedKeysFromProject = new ArrayList<>();
        ArrayNode jsonFromBundle = createArrayNode("a", "b", "c");
        ArrayNode jsonFromProjectEmpty = createArrayNode();

        boolean result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundle,
                jsonFromProjectEmpty, missedKeysFromProject);
        Assert.assertFalse(result);

        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a ArrayNode the keys are quoted
        Assert.assertEquals(missedKeysFromProject,
                List.of("\"a\"", "\"b\"", "\"c\""));

        List<String> missedKeysFromBundle = new ArrayList<>();
        ArrayNode jsonFromProject = createArrayNode("a", "b", "c");
        ArrayNode jsonFromBundleEmpty = createArrayNode();

        result = ThemeValidationUtil.objectIncludesEntry(jsonFromBundleEmpty,
                jsonFromProject, missedKeysFromBundle);
        Assert.assertFalse(result);
        Assert.assertEquals(missedKeysFromBundle,
                List.of("\"a\"", "\"b\"", "\"c\""));
    }

    private ArrayNode createArrayNode(String... values) {
        ArrayNode array = JacksonUtils.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
