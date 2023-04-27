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
