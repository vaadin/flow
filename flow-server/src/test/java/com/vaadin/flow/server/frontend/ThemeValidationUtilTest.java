package com.vaadin.flow.server.frontend;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ThemeValidationUtilTest {
    private static boolean invokeObjectIncludesEntry(JsonValue jsonFromBundle,
            JsonValue jsonFromProject, Collection<String> missedKeys)
            throws ReflectiveOperationException {
        Method privateMethod = ThemeValidationUtil.class.getDeclaredMethod(
                "objectIncludesEntry", JsonValue.class, JsonValue.class,
                Collection.class);
        privateMethod.setAccessible(true);

        // invoke the private method for testing
        return (boolean) privateMethod.invoke(null, jsonFromBundle,
                jsonFromProject, missedKeys);
    }

    @Test
    public void testObjectsIncludeMethodWithSameElementsInArrays()
            throws ReflectiveOperationException {
        JsonArray jsonFromBundle = createJsonArray("a", "b", "c");
        JsonArray projectJson = createJsonArray("a", "b", "c");
        List<String> missedKeys = new ArrayList<>();

        boolean result = invokeObjectIncludesEntry(jsonFromBundle, projectJson,
                missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodArraysAreDifferent()
            throws ReflectiveOperationException {
        List<String> missedKeysForBundle = new ArrayList<>();
        JsonArray jsonFromBundle = createJsonArray("a", "c");
        JsonArray jsonFromProject = createJsonArray("a", "b", "c");

        boolean result = invokeObjectIncludesEntry(jsonFromBundle,
                jsonFromProject, missedKeysForBundle);
        Assert.assertFalse(result);
        // the missed keys should be the same as the jsonFromBundle as the other
        // array is empty
        // also because it's a JsonArray the keys are quoted
        Assert.assertEquals(missedKeysForBundle, List.of("\"b\""));

        List<String> missedKeysForProject = new ArrayList<>();
        jsonFromBundle = createJsonArray("a", "b", "c");
        jsonFromProject = createJsonArray("a");
        result = invokeObjectIncludesEntry(jsonFromProject, jsonFromBundle,
                missedKeysForProject);
        Assert.assertFalse(result);
        Assert.assertEquals(missedKeysForProject, List.of("\"b\"", "\"c\""));
    }

    @Test
    public void testObjectsIncludeMethodBothEmptyArraysAreEmpty()
            throws ReflectiveOperationException {
        List<String> missedKeys = new ArrayList<>();

        boolean result = invokeObjectIncludesEntry(Json.createArray(),
                Json.createArray(), missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    @Test
    public void testObjectsIncludeMethodOneArrayIsEmpty()
            throws ReflectiveOperationException {
        List<String> missedKeysFromProject = new ArrayList<>();
        JsonArray jsonFromBundle = createJsonArray("a", "b", "c");
        JsonArray jsonFromProjectEmpty = createJsonArray();

        boolean result = invokeObjectIncludesEntry(jsonFromBundle,
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

        result = invokeObjectIncludesEntry(jsonFromBundleEmpty, jsonFromProject,
                missedKeysFromBundle);
        Assert.assertFalse(result);
        Assert.assertEquals(missedKeysFromBundle,
                List.of("\"a\"", "\"b\"", "\"c\""));
    }

    @Test(expected = InvocationTargetException.class)
    public void testObjectsIncludeMethodWithWithNullArrays()
            throws ReflectiveOperationException {
        List<String> missedKeys = new ArrayList<>();

        boolean result = invokeObjectIncludesEntry(null, null, missedKeys);
        Assert.assertTrue(result);
        Assert.assertTrue(missedKeys.isEmpty());
    }

    private JsonArray createJsonArray(String... values) {
        JsonArray array = Json.createArray();
        for (String value : values) {
            array.set(array.length(), value);
        }
        return array;
    }
}
