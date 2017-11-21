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
package com.vaadin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

/**
 * Tests for the {@link JsonSerializer}.
 */
public class JsonSerializerTest {

    private static final double PRECISION = 0.000001;

    public static enum SomeEnum {
        SOME_VALUE_1, SOME_VALUE_2;
    }

    public static class ObjectWithSimpleTypes {

        private String stringProperty;
        private int intProperty;
        private Integer integerProperty;
        private long longProperty;
        private Long longObjectProperty;
        private short shortProperty;
        private Short shortObjectProperty;
        private double doubleProperty;
        private Double doubleObjectProperty;
        private byte byteProperty;
        private Byte byteObjectProperty;
        private boolean booleanProperty;
        private Boolean booleanObjectProperty;
        private char charProperty;
        private Character characterProperty;
        private SomeEnum enumProperty;

        public String getStringProperty() {
            return stringProperty;
        }

        public void setStringProperty(String stringProperty) {
            this.stringProperty = stringProperty;
        }

        public int getIntProperty() {
            return intProperty;
        }

        public void setIntProperty(int intProperty) {
            this.intProperty = intProperty;
        }

        public Integer getIntegerProperty() {
            return integerProperty;
        }

        public void setIntegerProperty(Integer integerProperty) {
            this.integerProperty = integerProperty;
        }

        public long getLongProperty() {
            return longProperty;
        }

        public void setLongProperty(long longProperty) {
            this.longProperty = longProperty;
        }

        public Long getLongObjectProperty() {
            return longObjectProperty;
        }

        public void setLongObjectProperty(Long longObjectProperty) {
            this.longObjectProperty = longObjectProperty;
        }

        public short getShortProperty() {
            return shortProperty;
        }

        public void setShortProperty(short shortProperty) {
            this.shortProperty = shortProperty;
        }

        public Short getShortObjectProperty() {
            return shortObjectProperty;
        }

        public void setShortObjectProperty(Short shortObjectProperty) {
            this.shortObjectProperty = shortObjectProperty;
        }

        public Double getDoubleObjectProperty() {
            return doubleObjectProperty;
        }

        public void setDoubleObjectProperty(Double doubleObjectProperty) {
            this.doubleObjectProperty = doubleObjectProperty;
        }

        public double getDoubleProperty() {
            return doubleProperty;
        }

        public void setDoubleProperty(double doubleProperty) {
            this.doubleProperty = doubleProperty;
        }

        public byte getByteProperty() {
            return byteProperty;
        }

        public void setByteProperty(byte byteProperty) {
            this.byteProperty = byteProperty;
        }

        public Byte getByteObjectProperty() {
            return byteObjectProperty;
        }

        public void setByteObjectProperty(Byte byteObjectProperty) {
            this.byteObjectProperty = byteObjectProperty;
        }

        public boolean isBooleanProperty() {
            return booleanProperty;
        }

        public void setBooleanProperty(boolean booleanProperty) {
            this.booleanProperty = booleanProperty;
        }

        public Boolean getBooleanObjectProperty() {
            return booleanObjectProperty;
        }

        public void setBooleanObjectProperty(Boolean booleanObjectProperty) {
            this.booleanObjectProperty = booleanObjectProperty;
        }

        public char getCharProperty() {
            return charProperty;
        }

        public void setCharProperty(char charProperty) {
            this.charProperty = charProperty;
        }

        public Character getCharacterProperty() {
            return characterProperty;
        }

        public void setCharacterProperty(Character characterProperty) {
            this.characterProperty = characterProperty;
        }

        public SomeEnum getEnumProperty() {
            return enumProperty;
        }

        public void setEnumProperty(SomeEnum enumProperty) {
            this.enumProperty = enumProperty;
        }
    }

    public static class ObjectWithOtherObjects {

        private ObjectWithSimpleTypes object1;
        private ObjectWithSimpleTypes object2;

        public ObjectWithSimpleTypes getObject1() {
            return object1;
        }

        public void setObject1(ObjectWithSimpleTypes object1) {
            this.object1 = object1;
        }

        public ObjectWithSimpleTypes getObject2() {
            return object2;
        }

        public void setObject2(ObjectWithSimpleTypes object2) {
            this.object2 = object2;
        }
    }

    public static class RecursiveObject {

        private RecursiveObject recursive;
        private int index;

        public RecursiveObject getRecursive() {
            return recursive;
        }

        public void setRecursive(RecursiveObject recursive) {
            this.recursive = recursive;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    public static class ObjectWithBasicCollections {
        private List<String> listOfStrings;
        private Set<Integer> setOfIntegers;
        private LinkedList<Boolean> linkedListOfBooleans;
        private ArrayList<Double> arrayListOfDoubles;

        public List<String> getListOfStrings() {
            return listOfStrings;
        }

        public void setListOfStrings(List<String> listOfStrings) {
            this.listOfStrings = listOfStrings;
        }

        public Set<Integer> getSetOfIntegers() {
            return setOfIntegers;
        }

        public void setSetOfIntegers(Set<Integer> setOfIntegers) {
            this.setOfIntegers = setOfIntegers;
        }

        public LinkedList<Boolean> getLinkedListOfBooleans() {
            return linkedListOfBooleans;
        }

        public void setLinkedListOfBooleans(
                LinkedList<Boolean> linkedListOfBooleans) {
            this.linkedListOfBooleans = linkedListOfBooleans;
        }

        public ArrayList<Double> getArrayListOfDoubles() {
            return arrayListOfDoubles;
        }

        public void setArrayListOfDoubles(
                ArrayList<Double> arrayListOfDoubles) {
            this.arrayListOfDoubles = arrayListOfDoubles;
        }
    }

    @Test
    public void serializeBasicTypes_returnJsonBasicTypes() {
        JsonValue json = JsonSerializer.toJson("someString");
        assertTrue("The JsonValue should be instanceof JsonString",
                json instanceof JsonString);
        assertEquals("someString", json.asString());

        json = JsonSerializer.toJson(0);
        assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson(0.0);
        assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson(0l);
        assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson((byte) 0);
        assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson((short) 0);
        assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson(true);
        assertTrue("The JsonValue should be instanceof JsonBoolean",
                json instanceof JsonBoolean);
        assertTrue(json.asBoolean());

        json = JsonSerializer.toJson(false);
        assertTrue("The JsonValue should be instanceof JsonBoolean",
                json instanceof JsonBoolean);
        assertFalse(json.asBoolean());

        json = JsonSerializer.toJson(SomeEnum.SOME_VALUE_1);
        assertTrue("The JsonValue should be instanceof JsonString",
                json instanceof JsonString);
        assertEquals(SomeEnum.SOME_VALUE_1.name(), json.asString());
    }

    @Test
    public void serializeNull_returnNull() {
        JsonValue json = JsonSerializer.toJson((Object) null);
        assertTrue("The JsonValue should be instanceof JsonNull",
                json instanceof JsonNull);
    }

    @Test
    public void serializeEmptyObjectWithBasicTypes_returnJsonObjectWithEmptyProperties() {
        ObjectWithSimpleTypes bean = new ObjectWithSimpleTypes();
        JsonValue json = JsonSerializer.toJson(bean);
        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = (JsonObject) json;
        assertObjectHasNullValueForKey(object, "stringProperty");
        assertObjectHas0ValueForKey(object, "intProperty");
        assertObjectHasNullValueForKey(object, "integerProperty");
        assertObjectHas0ValueForKey(object, "longProperty");
        assertObjectHasNullValueForKey(object, "longObjectProperty");
        assertObjectHas0ValueForKey(object, "shortProperty");
        assertObjectHasNullValueForKey(object, "shortObjectProperty");
        assertObjectHas0ValueForKey(object, "doubleProperty");
        assertObjectHasNullValueForKey(object, "doubleObjectProperty");
        assertObjectHas0ValueForKey(object, "byteProperty");
        assertObjectHasNullValueForKey(object, "byteObjectProperty");
        assertObjectHasFalseValueForKey(object, "booleanProperty");
        assertObjectHasNullValueForKey(object, "booleanObjectProperty");
        assertObjectHasNullValueForKey(object, "characterProperty");
        assertObjectHasNullValueForKey(object, "enumProperty");

        // char is a different case, it is a string at the json
        assertTrue(object.hasKey("charProperty"));
        assertEquals((char) 0,
                object.getString("charProperty").charAt(0));

        bean = JsonSerializer.toObject(ObjectWithSimpleTypes.class, json);
        assertNotNull(bean);
        assertNull(bean.getStringProperty());
        assertEquals(0, bean.getIntProperty());
        assertNull(bean.getIntegerProperty());
        assertEquals(0, bean.getLongProperty());
        assertNull(bean.getLongObjectProperty());
        assertEquals(0, bean.getShortProperty());
        assertNull(bean.getShortObjectProperty());
        assertEquals(0, bean.getDoubleProperty(), 0.00001);
        assertNull(bean.getDoubleObjectProperty());
        assertEquals(0, bean.getByteProperty());
        assertNull(bean.getByteObjectProperty());
        assertEquals(false, bean.isBooleanProperty());
        assertNull(bean.getBooleanObjectProperty());
        assertEquals(0, bean.getCharProperty());
        assertNull(bean.getCharacterProperty());
        assertNull(bean.getEnumProperty());
    }

    @Test
    public void serializePopulatedObjectWithBasicTypes_returnJsonObjectWithDefinedProperties() {
        ObjectWithSimpleTypes bean = new ObjectWithSimpleTypes();
        bean.setStringProperty("someProperty");
        bean.setIntProperty(1);
        bean.setIntegerProperty(2);
        bean.setLongProperty(3);
        bean.setLongObjectProperty(4l);
        bean.setShortProperty((short) 5);
        bean.setShortObjectProperty((short) 6);
        bean.setDoubleProperty(7);
        bean.setDoubleObjectProperty(8.0);
        bean.setByteProperty((byte) 9);
        bean.setByteObjectProperty((byte) 10);
        bean.setBooleanProperty(true);
        bean.setBooleanObjectProperty(false);
        bean.setCharProperty('c');
        bean.setCharacterProperty('C');
        bean.setEnumProperty(SomeEnum.SOME_VALUE_2);

        JsonValue json = JsonSerializer.toJson(bean);
        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = (JsonObject) json;
        assertEquals("someProperty", object.getString("stringProperty"));
        assertEquals(1, object.getNumber("intProperty"), PRECISION);
        assertEquals(2, object.getNumber("integerProperty"), PRECISION);
        assertEquals(3, object.getNumber("longProperty"), PRECISION);
        assertEquals(4, object.getNumber("longObjectProperty"),
                PRECISION);
        assertEquals(5, object.getNumber("shortProperty"), PRECISION);
        assertEquals(6, object.getNumber("shortObjectProperty"),
                PRECISION);
        assertEquals(7, object.getNumber("doubleProperty"), PRECISION);
        assertEquals(8, object.getNumber("doubleObjectProperty"),
                PRECISION);
        assertEquals(9, object.getNumber("byteProperty"), PRECISION);
        assertEquals(10, object.getNumber("byteObjectProperty"),
                PRECISION);
        assertEquals(true, object.getBoolean("booleanProperty"));
        assertEquals(false, object.getBoolean("booleanObjectProperty"));
        assertEquals('c', object.getString("charProperty").charAt(0));
        assertEquals('C',
                object.getString("characterProperty").charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_2.name(),
                object.getString("enumProperty"));

        bean = JsonSerializer.toObject(ObjectWithSimpleTypes.class, json);
        assertNotNull(bean);
        assertEquals("someProperty", bean.getStringProperty());
        assertEquals(1, bean.getIntProperty());
        assertEquals(Integer.valueOf(2), bean.getIntegerProperty());
        assertEquals(3, bean.getLongProperty());
        assertEquals(Long.valueOf(4), bean.getLongObjectProperty());
        assertEquals(5, bean.getShortProperty());
        assertEquals(Short.valueOf((short) 6),
                bean.getShortObjectProperty());
        assertEquals(7, bean.getDoubleProperty(), 0.00001);
        assertEquals(Double.valueOf(8), bean.getDoubleObjectProperty());
        assertEquals(9, bean.getByteProperty());
        assertEquals(Byte.valueOf((byte) 10),
                bean.getByteObjectProperty());
        assertEquals(true, bean.isBooleanProperty());
        assertEquals(Boolean.FALSE, bean.getBooleanObjectProperty());
        assertEquals('c', bean.getCharProperty());
        assertEquals((Character) 'C', bean.getCharacterProperty());
        assertEquals(SomeEnum.SOME_VALUE_2, bean.getEnumProperty());
    }

    @Test
    public void serializeEmptyObjectWithObjects_returnJsonObjectWithNullProperties() {
        ObjectWithOtherObjects bean = new ObjectWithOtherObjects();

        JsonValue json = JsonSerializer.toJson(bean);

        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        assertTrue(jsonObject.hasKey("object1"));
        assertTrue(jsonObject.get("object1") instanceof JsonNull);
        assertTrue(jsonObject.hasKey("object2"));
        assertTrue(jsonObject.get("object2") instanceof JsonNull);

        bean = JsonSerializer.toObject(ObjectWithOtherObjects.class, json);

        assertNotNull("The deserialized object should not be null",
                bean);
        assertNull(bean.getObject1());
        assertNull(bean.getObject2());
    }

    @Test
    public void serializeObjectWithObjects_returnJsonObjectWithPopulatedProperties() {
        ObjectWithOtherObjects bean = new ObjectWithOtherObjects();
        ObjectWithSimpleTypes innerBean = new ObjectWithSimpleTypes();
        innerBean.setStringProperty("someProperty");
        innerBean.setIntProperty(1);
        innerBean.setIntegerProperty(2);
        innerBean.setLongProperty(3);
        innerBean.setLongObjectProperty(4l);
        innerBean.setShortProperty((short) 5);
        innerBean.setShortObjectProperty((short) 6);
        innerBean.setDoubleProperty(7);
        innerBean.setDoubleObjectProperty(8.0);
        innerBean.setByteProperty((byte) 9);
        innerBean.setByteObjectProperty((byte) 10);
        innerBean.setBooleanProperty(true);
        innerBean.setBooleanObjectProperty(false);
        innerBean.setCharProperty('c');
        innerBean.setCharacterProperty('C');
        innerBean.setEnumProperty(SomeEnum.SOME_VALUE_2);
        bean.setObject1(innerBean);

        innerBean = new ObjectWithSimpleTypes();
        innerBean.setStringProperty("someOtherProperty");
        innerBean.setIntProperty(10);
        innerBean.setIntegerProperty(20);
        innerBean.setLongProperty(30);
        innerBean.setLongObjectProperty(40l);
        innerBean.setShortProperty((short) 50);
        innerBean.setShortObjectProperty((short) 60);
        innerBean.setDoubleProperty(70);
        innerBean.setDoubleObjectProperty(80.0);
        innerBean.setByteProperty((byte) 90);
        innerBean.setByteObjectProperty((byte) 100);
        innerBean.setBooleanProperty(true);
        innerBean.setBooleanObjectProperty(false);
        innerBean.setCharProperty('d');
        innerBean.setCharacterProperty('D');
        innerBean.setEnumProperty(SomeEnum.SOME_VALUE_1);
        bean.setObject2(innerBean);

        JsonValue json = JsonSerializer.toJson(bean);

        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = ((JsonObject) json).getObject("object1");
        assertNotNull("The object1 should be not be null", object);

        assertEquals("someProperty", object.getString("stringProperty"));
        assertEquals(1, object.getNumber("intProperty"), PRECISION);
        assertEquals(2, object.getNumber("integerProperty"), PRECISION);
        assertEquals(3, object.getNumber("longProperty"), PRECISION);
        assertEquals(4, object.getNumber("longObjectProperty"),
                PRECISION);
        assertEquals(5, object.getNumber("shortProperty"), PRECISION);
        assertEquals(6, object.getNumber("shortObjectProperty"),
                PRECISION);
        assertEquals(7, object.getNumber("doubleProperty"), PRECISION);
        assertEquals(8, object.getNumber("doubleObjectProperty"),
                PRECISION);
        assertEquals(9, object.getNumber("byteProperty"), PRECISION);
        assertEquals(10, object.getNumber("byteObjectProperty"),
                PRECISION);
        assertEquals(true, object.getBoolean("booleanProperty"));
        assertEquals(false, object.getBoolean("booleanObjectProperty"));
        assertEquals('c', object.getString("charProperty").charAt(0));
        assertEquals('C',
                object.getString("characterProperty").charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_2.name(),
                object.getString("enumProperty"));

        object = ((JsonObject) json).getObject("object2");
        assertNotNull("The object2 should be not be null", object);

        assertEquals("someOtherProperty",
                object.getString("stringProperty"));
        assertEquals(10, object.getNumber("intProperty"), PRECISION);
        assertEquals(20, object.getNumber("integerProperty"), PRECISION);
        assertEquals(30, object.getNumber("longProperty"), PRECISION);
        assertEquals(40, object.getNumber("longObjectProperty"),
                PRECISION);
        assertEquals(50, object.getNumber("shortProperty"), PRECISION);
        assertEquals(60, object.getNumber("shortObjectProperty"),
                PRECISION);
        assertEquals(70, object.getNumber("doubleProperty"), PRECISION);
        assertEquals(80, object.getNumber("doubleObjectProperty"),
                PRECISION);
        assertEquals(90, object.getNumber("byteProperty"), PRECISION);
        assertEquals(100, object.getNumber("byteObjectProperty"),
                PRECISION);
        assertEquals(true, object.getBoolean("booleanProperty"));
        assertEquals(false, object.getBoolean("booleanObjectProperty"));
        assertEquals('d', object.getString("charProperty").charAt(0));
        assertEquals('D',
                object.getString("characterProperty").charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_1.name(),
                object.getString("enumProperty"));
    }

    @Test
    public void serializeEmptyRecursiveObject_returnJsonObjectWithNullProperties() {
        RecursiveObject bean = new RecursiveObject();

        JsonValue json = JsonSerializer.toJson(bean);

        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        assertTrue(jsonObject.hasKey("recursive"));
        assertTrue(jsonObject.get("recursive") instanceof JsonNull);
        assertEquals(0, jsonObject.getNumber("index"), PRECISION);

        bean = JsonSerializer.toObject(RecursiveObject.class, json);

        assertNotNull("The deserialized object should not be null",
                bean);
        assertNull(bean.getRecursive());
        assertEquals(0, bean.getIndex());
    }

    @Test
    public void serializePopulatedRecursiveObject_returnJsonObjectWithPopulatedProperties() {
        final int recursions = 10;
        RecursiveObject bean = createRecusiveObject(recursions, 0);

        JsonValue json = JsonSerializer.toJson(bean);
        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = ((JsonObject) json);
        for (int i = 0; i < recursions; i++) {
            assertEquals(i, object.getNumber("index"), PRECISION);
            if (i < recursions - 1) {
                object = object.getObject("recursive");
            } else {
                assertTrue(object.get("recursive") instanceof JsonNull);
            }
        }

        bean = JsonSerializer.toObject(RecursiveObject.class, json);

        for (int i = 0; i < recursions; i++) {
            assertEquals(i, bean.getIndex());
            bean = bean.getRecursive();
        }
    }

    @Test
    public void serializeEmptyObjectWithBasicCollections_returnJsonObjectWithNullProperties() {
        ObjectWithBasicCollections bean = new ObjectWithBasicCollections();

        /*
         * private List<String> listOfStrings; private Set<Integer>
         * setOfIntegers; private LinkedList<Boolean> linkedListOfBooleans;
         * private ArrayList<Double> arrayListOfDoubles;
         * 
         */

        JsonValue json = JsonSerializer.toJson(bean);

        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        assertTrue(jsonObject.hasKey("listOfStrings"));
        assertTrue(jsonObject.get("listOfStrings") instanceof JsonNull);
        assertTrue(jsonObject.hasKey("setOfIntegers"));
        assertTrue(jsonObject.get("setOfIntegers") instanceof JsonNull);
        assertTrue(jsonObject.hasKey("linkedListOfBooleans"));
        assertTrue(
                jsonObject.get("linkedListOfBooleans") instanceof JsonNull);
        assertTrue(jsonObject.hasKey("arrayListOfDoubles"));
        assertTrue(
                jsonObject.get("arrayListOfDoubles") instanceof JsonNull);

        bean = JsonSerializer.toObject(ObjectWithBasicCollections.class, json);

        assertNotNull("The deserialized object should not be null",
                bean);
        assertNull(bean.getListOfStrings());
        assertNull(bean.getSetOfIntegers());
        assertNull(bean.getLinkedListOfBooleans());
        assertNull(bean.getArrayListOfDoubles());
    }

    @Test
    public void serializeObjectWithCollections_returnJsonObjectWithPopulatedProperties() {
        ObjectWithBasicCollections bean = new ObjectWithBasicCollections();

        bean.setListOfStrings(Arrays.asList("string1", "string2"));
        bean.setSetOfIntegers(new LinkedHashSet<>(Arrays.asList(3, 4)));
        bean.setLinkedListOfBooleans(
                new LinkedList<>(Arrays.asList(true, false)));
        bean.setArrayListOfDoubles(new ArrayList<>(Arrays.asList(5.0, 6.0)));

        JsonValue json = JsonSerializer.toJson(bean);

        assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        JsonArray array = (JsonArray) jsonObject.get("listOfStrings");
        assertEquals("string1", array.getString(0));
        assertEquals("string2", array.getString(1));

        array = (JsonArray) jsonObject.get("setOfIntegers");
        assertEquals(3, array.getNumber(0), PRECISION);
        assertEquals(4, array.getNumber(1), PRECISION);

        array = (JsonArray) jsonObject.get("linkedListOfBooleans");
        assertEquals(true, array.getBoolean(0));
        assertEquals(false, array.getBoolean(1));

        array = (JsonArray) jsonObject.get("arrayListOfDoubles");
        assertEquals(5, array.getNumber(0), PRECISION);
        assertEquals(6, array.getNumber(1), PRECISION);

        bean = JsonSerializer.toObject(ObjectWithBasicCollections.class, json);

        assertNotNull("The deserialized object should not be null",
                bean);
        assertCollectionItemsAreEqual(bean.getListOfStrings(), "string1",
                "string2");
        assertCollectionItemsAreEqual(bean.getSetOfIntegers(), 3, 4);
        assertCollectionItemsAreEqual(bean.getLinkedListOfBooleans(), true,
                false);
        assertCollectionItemsAreEqual(bean.getArrayListOfDoubles(), 5.0, 6.0);
    }

    private <T> void assertCollectionItemsAreEqual(Collection<T> collection,
            T... values) {
        int index = 0;
        for (T collectionValue : collection) {
            assertEquals(collectionValue, values[index]);
            index++;
        }
    }

    private RecursiveObject createRecusiveObject(int recursions, int index) {
        if (index < recursions) {
            RecursiveObject bean = new RecursiveObject();
            bean.setIndex(index);
            bean.setRecursive(createRecusiveObject(recursions, index + 1));
            return bean;
        }
        return null;
    }

    private void assertObjectHasNullValueForKey(JsonObject object, String key) {
        assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        assertTrue(key + " property should be JsonNull",
                object.get(key) instanceof JsonNull);
    }

    private void assertObjectHas0ValueForKey(JsonObject object, String key) {
        assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        assertEquals(key + " should be 0.0 in the JsonObject", 0.0,
                object.getNumber(key), PRECISION);
    }

    private void assertObjectHasFalseValueForKey(JsonObject object,
            String key) {
        assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        assertEquals(key + " should be false in the JsonObject", false,
                object.getBoolean(key));
    }

}
