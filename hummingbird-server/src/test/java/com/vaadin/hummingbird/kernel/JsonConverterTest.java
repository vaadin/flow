package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;

public class JsonConverterTest {

    @Test
    public void testJsonArrayBooleanTypeResolving() {
        JsonArray array = Json.createArray();
        array.set(0, false);
        array.set(1, true);
        array.set(2, Json.createNull());
        array.set(3, false);

        Type type = JsonConverter.findType(array);

        Assert.assertEquals(Boolean[].class, type);

        Assert.assertArrayEquals(
                new Boolean[] { Boolean.FALSE, Boolean.TRUE, null,
                        Boolean.FALSE },
                (Object[]) JsonConverter.fromJson(type, array));
    }

    @Test
    public void testJsonArrayNumberTypeResolving() {

        Double[] values = new Double[] { 0.0D, 5.0D, null, 100.12D };
        JsonArray array = Json.createArray();
        array.set(0, values[0]);
        array.set(1, values[1]);
        array.set(2, Json.createNull());
        array.set(3, values[3]);

        Type type = JsonConverter.findType(array);

        Assert.assertEquals(Double[].class, type);

        Assert.assertArrayEquals(values,
                (Object[]) JsonConverter.fromJson(type, array));
    }

    @Test
    public void testJsonArrayStringTypeResolving() {
        String[] values = new String[] { "foo", "bar", null, "baz" };
        JsonArray array = Json.createArray();
        array.set(0, values[0]);
        array.set(1, values[1]);
        array.set(2, Json.createNull());
        array.set(3, values[3]);

        Type type = JsonConverter.findType(array);

        Assert.assertEquals(String[].class, type);

        Assert.assertArrayEquals(values,
                (Object[]) JsonConverter.fromJson(type, array));
    }

    @Test
    public void testJsonArrayRandomTypeResolving() {
        Object[] values = new Object[] { "foo", 5.0D, null, false };
        JsonArray array = Json.createArray();
        array.set(0, (String) values[0]);
        array.set(1, (Double) values[1]);
        array.set(2, Json.createNull());
        array.set(3, (Boolean) values[3]);

        Type type = JsonConverter.findType(array);

        Assert.assertEquals(Object[].class, type);

        Assert.assertArrayEquals(values,
                (Object[]) JsonConverter.fromJson(type, array));
    }
}
