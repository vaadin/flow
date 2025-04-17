/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
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

    public record SomeRecord(String name, int age) {
    }

    public record RecordWithRecordAndObject(SomeRecord record,
            ObjectWithSimpleTypes object) {
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
        private SomeRecord record;

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

        public SomeRecord getRecord() {
            return record;
        }

        public void setRecord(SomeRecord record) {
            this.record = record;
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
        private List<Double> arrayListOfDoubles;

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

        public List<Double> getArrayListOfDoubles() {
            return arrayListOfDoubles;
        }

        public void setArrayListOfDoubles(List<Double> arrayListOfDoubles) {
            this.arrayListOfDoubles = arrayListOfDoubles;
        }
    }

    @Test
    public void serializeBasicTypes_returnJsonBasicTypes() {
        JsonValue json = JsonSerializer.toJson("someString");
        Assert.assertTrue("The JsonValue should be instanceof JsonString",
                json instanceof JsonString);
        Assert.assertEquals("someString", json.asString());

        json = JsonSerializer.toJson(0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson(0.0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson(0l);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson((byte) 0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson((short) 0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), PRECISION);

        json = JsonSerializer.toJson(true);
        Assert.assertTrue("The JsonValue should be instanceof JsonBoolean",
                json instanceof JsonBoolean);
        Assert.assertTrue(json.asBoolean());

        json = JsonSerializer.toJson(false);
        Assert.assertTrue("The JsonValue should be instanceof JsonBoolean",
                json instanceof JsonBoolean);
        Assert.assertFalse(json.asBoolean());

        json = JsonSerializer.toJson(SomeEnum.SOME_VALUE_1);
        Assert.assertTrue("The JsonValue should be instanceof JsonString",
                json instanceof JsonString);
        Assert.assertEquals(SomeEnum.SOME_VALUE_1.name(), json.asString());
    }

    @Test
    public void serializeNull_returnNull() {
        JsonValue json = JsonSerializer.toJson((Object) null);
        Assert.assertTrue("The JsonValue should be instanceof JsonNull",
                json instanceof JsonNull);
    }

    @Test
    public void serializeEmptyObjectWithBasicTypes_returnJsonObjectWithEmptyProperties() {
        ObjectWithSimpleTypes bean = new ObjectWithSimpleTypes();
        JsonValue json = JsonSerializer.toJson(bean);
        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
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
        Assert.assertTrue(object.hasKey("charProperty"));
        Assert.assertEquals((char) 0,
                object.getString("charProperty").charAt(0));

        bean = JsonSerializer.toObject(ObjectWithSimpleTypes.class, json);
        Assert.assertNotNull(bean);
        Assert.assertNull(bean.getStringProperty());
        Assert.assertEquals(0, bean.getIntProperty());
        Assert.assertNull(bean.getIntegerProperty());
        Assert.assertEquals(0, bean.getLongProperty());
        Assert.assertNull(bean.getLongObjectProperty());
        Assert.assertEquals(0, bean.getShortProperty());
        Assert.assertNull(bean.getShortObjectProperty());
        Assert.assertEquals(0, bean.getDoubleProperty(), 0.00001);
        Assert.assertNull(bean.getDoubleObjectProperty());
        Assert.assertEquals(0, bean.getByteProperty());
        Assert.assertNull(bean.getByteObjectProperty());
        Assert.assertEquals(false, bean.isBooleanProperty());
        Assert.assertNull(bean.getBooleanObjectProperty());
        Assert.assertEquals(0, bean.getCharProperty());
        Assert.assertNull(bean.getCharacterProperty());
        Assert.assertNull(bean.getEnumProperty());
    }

    @Test
    public void serializePopulatedObjectWithBasicTypes_returnJsonObjectWithDefinedProperties() {
        ObjectWithSimpleTypes bean = getPopulatedObjectWithSimpleTypes();

        JsonValue json = JsonSerializer.toJson(bean);
        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = (JsonObject) json;
        Assert.assertEquals("someProperty", object.getString("stringProperty"));
        Assert.assertEquals(1, object.getNumber("intProperty"), PRECISION);
        Assert.assertEquals(2, object.getNumber("integerProperty"), PRECISION);
        Assert.assertEquals(3, object.getNumber("longProperty"), PRECISION);
        Assert.assertEquals(4, object.getNumber("longObjectProperty"),
                PRECISION);
        Assert.assertEquals(5, object.getNumber("shortProperty"), PRECISION);
        Assert.assertEquals(6, object.getNumber("shortObjectProperty"),
                PRECISION);
        Assert.assertEquals(7, object.getNumber("doubleProperty"), PRECISION);
        Assert.assertEquals(8, object.getNumber("doubleObjectProperty"),
                PRECISION);
        Assert.assertEquals(9, object.getNumber("byteProperty"), PRECISION);
        Assert.assertEquals(10, object.getNumber("byteObjectProperty"),
                PRECISION);
        Assert.assertEquals(true, object.getBoolean("booleanProperty"));
        Assert.assertEquals(false, object.getBoolean("booleanObjectProperty"));
        Assert.assertEquals('c', object.getString("charProperty").charAt(0));
        Assert.assertEquals('C',
                object.getString("characterProperty").charAt(0));
        Assert.assertEquals(SomeEnum.SOME_VALUE_2.name(),
                object.getString("enumProperty"));

        bean = JsonSerializer.toObject(ObjectWithSimpleTypes.class, json);
        Assert.assertNotNull(bean);
        Assert.assertEquals("someProperty", bean.getStringProperty());
        Assert.assertEquals(1, bean.getIntProperty());
        Assert.assertEquals(Integer.valueOf(2), bean.getIntegerProperty());
        Assert.assertEquals(3, bean.getLongProperty());
        Assert.assertEquals(Long.valueOf(4), bean.getLongObjectProperty());
        Assert.assertEquals(5, bean.getShortProperty());
        Assert.assertEquals(Short.valueOf((short) 6),
                bean.getShortObjectProperty());
        Assert.assertEquals(7, bean.getDoubleProperty(), 0.00001);
        Assert.assertEquals(Double.valueOf(8), bean.getDoubleObjectProperty());
        Assert.assertEquals(9, bean.getByteProperty());
        Assert.assertEquals(Byte.valueOf((byte) 10),
                bean.getByteObjectProperty());
        Assert.assertEquals(true, bean.isBooleanProperty());
        Assert.assertEquals(Boolean.FALSE, bean.getBooleanObjectProperty());
        Assert.assertEquals('c', bean.getCharProperty());
        Assert.assertEquals((Character) 'C', bean.getCharacterProperty());
        Assert.assertEquals(SomeEnum.SOME_VALUE_2, bean.getEnumProperty());
    }

    @Test
    public void serializeEmptyObjectWithObjects_returnJsonObjectWithNullProperties() {
        ObjectWithOtherObjects bean = new ObjectWithOtherObjects();

        JsonValue json = JsonSerializer.toJson(bean);

        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        Assert.assertTrue(jsonObject.hasKey("object1"));
        Assert.assertTrue(jsonObject.get("object1") instanceof JsonNull);
        Assert.assertTrue(jsonObject.hasKey("object2"));
        Assert.assertTrue(jsonObject.get("object2") instanceof JsonNull);
        Assert.assertTrue(jsonObject.hasKey("record"));
        Assert.assertTrue(jsonObject.get("record") instanceof JsonNull);

        bean = JsonSerializer.toObject(ObjectWithOtherObjects.class, json);

        Assert.assertNotNull("The deserialized object should not be null",
                bean);
        Assert.assertNull(bean.getObject1());
        Assert.assertNull(bean.getObject2());
        Assert.assertNull(bean.getRecord());
    }

    @Test
    public void serializeObjectWithObjects_returnJsonObjectWithPopulatedProperties() {
        ObjectWithOtherObjects bean = new ObjectWithOtherObjects();
        ObjectWithSimpleTypes innerBean = getPopulatedObjectWithSimpleTypes();
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

        SomeRecord record = new SomeRecord("someone", 42);
        bean.setRecord(record);

        JsonValue json = JsonSerializer.toJson(bean);

        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = ((JsonObject) json).getObject("object1");
        Assert.assertNotNull("The object1 should be not be null", object);

        Assert.assertEquals("someProperty", object.getString("stringProperty"));
        Assert.assertEquals(1, object.getNumber("intProperty"), PRECISION);
        Assert.assertEquals(2, object.getNumber("integerProperty"), PRECISION);
        Assert.assertEquals(3, object.getNumber("longProperty"), PRECISION);
        Assert.assertEquals(4, object.getNumber("longObjectProperty"),
                PRECISION);
        Assert.assertEquals(5, object.getNumber("shortProperty"), PRECISION);
        Assert.assertEquals(6, object.getNumber("shortObjectProperty"),
                PRECISION);
        Assert.assertEquals(7, object.getNumber("doubleProperty"), PRECISION);
        Assert.assertEquals(8, object.getNumber("doubleObjectProperty"),
                PRECISION);
        Assert.assertEquals(9, object.getNumber("byteProperty"), PRECISION);
        Assert.assertEquals(10, object.getNumber("byteObjectProperty"),
                PRECISION);
        Assert.assertEquals(true, object.getBoolean("booleanProperty"));
        Assert.assertEquals(false, object.getBoolean("booleanObjectProperty"));
        Assert.assertEquals('c', object.getString("charProperty").charAt(0));
        Assert.assertEquals('C',
                object.getString("characterProperty").charAt(0));
        Assert.assertEquals(SomeEnum.SOME_VALUE_2.name(),
                object.getString("enumProperty"));

        object = ((JsonObject) json).getObject("object2");
        Assert.assertNotNull("The object2 should be not be null", object);

        Assert.assertEquals("someOtherProperty",
                object.getString("stringProperty"));
        Assert.assertEquals(10, object.getNumber("intProperty"), PRECISION);
        Assert.assertEquals(20, object.getNumber("integerProperty"), PRECISION);
        Assert.assertEquals(30, object.getNumber("longProperty"), PRECISION);
        Assert.assertEquals(40, object.getNumber("longObjectProperty"),
                PRECISION);
        Assert.assertEquals(50, object.getNumber("shortProperty"), PRECISION);
        Assert.assertEquals(60, object.getNumber("shortObjectProperty"),
                PRECISION);
        Assert.assertEquals(70, object.getNumber("doubleProperty"), PRECISION);
        Assert.assertEquals(80, object.getNumber("doubleObjectProperty"),
                PRECISION);
        Assert.assertEquals(90, object.getNumber("byteProperty"), PRECISION);
        Assert.assertEquals(100, object.getNumber("byteObjectProperty"),
                PRECISION);
        Assert.assertEquals(true, object.getBoolean("booleanProperty"));
        Assert.assertEquals(false, object.getBoolean("booleanObjectProperty"));
        Assert.assertEquals('d', object.getString("charProperty").charAt(0));
        Assert.assertEquals('D',
                object.getString("characterProperty").charAt(0));
        Assert.assertEquals(SomeEnum.SOME_VALUE_1.name(),
                object.getString("enumProperty"));

        object = ((JsonObject) json).getObject("record");
        Assert.assertNotNull("The record should be not be null", object);
        Assert.assertEquals("someone", object.getString("name"));
        Assert.assertEquals(42, object.getNumber("age"), PRECISION);
    }

    @Test
    public void serializeEmptyRecursiveObject_returnJsonObjectWithNullProperties() {
        RecursiveObject bean = new RecursiveObject();

        JsonValue json = JsonSerializer.toJson(bean);

        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        Assert.assertTrue(jsonObject.hasKey("recursive"));
        Assert.assertTrue(jsonObject.get("recursive") instanceof JsonNull);
        Assert.assertEquals(0, jsonObject.getNumber("index"), PRECISION);

        bean = JsonSerializer.toObject(RecursiveObject.class, json);

        Assert.assertNotNull("The deserialized object should not be null",
                bean);
        Assert.assertNull(bean.getRecursive());
        Assert.assertEquals(0, bean.getIndex());
    }

    @Test
    public void serializePopulatedRecursiveObject_returnJsonObjectWithPopulatedProperties() {
        final int recursions = 10;
        RecursiveObject bean = createRecusiveObject(recursions, 0);

        JsonValue json = JsonSerializer.toJson(bean);
        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = ((JsonObject) json);
        for (int i = 0; i < recursions; i++) {
            Assert.assertEquals(i, object.getNumber("index"), PRECISION);
            if (i < recursions - 1) {
                object = object.getObject("recursive");
            } else {
                Assert.assertTrue(object.get("recursive") instanceof JsonNull);
            }
        }

        bean = JsonSerializer.toObject(RecursiveObject.class, json);

        for (int i = 0; i < recursions; i++) {
            Assert.assertEquals(i, bean.getIndex());
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

        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        Assert.assertTrue(jsonObject.hasKey("listOfStrings"));
        Assert.assertTrue(jsonObject.get("listOfStrings") instanceof JsonNull);
        Assert.assertTrue(jsonObject.hasKey("setOfIntegers"));
        Assert.assertTrue(jsonObject.get("setOfIntegers") instanceof JsonNull);
        Assert.assertTrue(jsonObject.hasKey("linkedListOfBooleans"));
        Assert.assertTrue(
                jsonObject.get("linkedListOfBooleans") instanceof JsonNull);
        Assert.assertTrue(jsonObject.hasKey("arrayListOfDoubles"));
        Assert.assertTrue(
                jsonObject.get("arrayListOfDoubles") instanceof JsonNull);

        bean = JsonSerializer.toObject(ObjectWithBasicCollections.class, json);

        Assert.assertNotNull("The deserialized object should not be null",
                bean);
        Assert.assertNull(bean.getListOfStrings());
        Assert.assertNull(bean.getSetOfIntegers());
        Assert.assertNull(bean.getLinkedListOfBooleans());
        Assert.assertNull(bean.getArrayListOfDoubles());
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

        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) json;
        JsonArray array = (JsonArray) jsonObject.get("listOfStrings");
        Assert.assertEquals("string1", array.getString(0));
        Assert.assertEquals("string2", array.getString(1));

        array = (JsonArray) jsonObject.get("setOfIntegers");
        Assert.assertEquals(3, array.getNumber(0), PRECISION);
        Assert.assertEquals(4, array.getNumber(1), PRECISION);

        array = (JsonArray) jsonObject.get("linkedListOfBooleans");
        Assert.assertEquals(true, array.getBoolean(0));
        Assert.assertEquals(false, array.getBoolean(1));

        array = (JsonArray) jsonObject.get("arrayListOfDoubles");
        Assert.assertEquals(5, array.getNumber(0), PRECISION);
        Assert.assertEquals(6, array.getNumber(1), PRECISION);

        bean = JsonSerializer.toObject(ObjectWithBasicCollections.class, json);

        Assert.assertNotNull("The deserialized object should not be null",
                bean);
        assertCollectionItemsAreEqual(bean.getListOfStrings(), "string1",
                "string2");
        assertCollectionItemsAreEqual(bean.getSetOfIntegers(), 3, 4);
        assertCollectionItemsAreEqual(bean.getLinkedListOfBooleans(), true,
                false);
        assertCollectionItemsAreEqual(bean.getArrayListOfDoubles(), 5.0, 6.0);
    }

    @Test
    public void serializeRecordWithRecordAndObject_returnJsonObjectWithPopulatedProperties() {
        SomeRecord record = new SomeRecord("someone", 42);
        ObjectWithSimpleTypes bean = getPopulatedObjectWithSimpleTypes();
        RecordWithRecordAndObject mainRecord = new RecordWithRecordAndObject(
                record, bean);

        JsonValue json = JsonSerializer.toJson(mainRecord);
        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = (JsonObject) json;
        JsonObject nestedRecord = object.getObject("record");
        JsonObject nestedObject = object.getObject("object");
        Assert.assertNotNull(nestedRecord);
        Assert.assertNotNull(nestedObject);

        Assert.assertEquals("someone", nestedRecord.getString("name"));
        Assert.assertEquals(42, nestedRecord.getNumber("age"), PRECISION);

        Assert.assertEquals("someProperty",
                nestedObject.getString("stringProperty"));
        Assert.assertEquals(1, nestedObject.getNumber("intProperty"),
                PRECISION);
        Assert.assertEquals(2, nestedObject.getNumber("integerProperty"),
                PRECISION);
        Assert.assertEquals(3, nestedObject.getNumber("longProperty"),
                PRECISION);
        Assert.assertEquals(4, nestedObject.getNumber("longObjectProperty"),
                PRECISION);
        Assert.assertEquals(5, nestedObject.getNumber("shortProperty"),
                PRECISION);
        Assert.assertEquals(6, nestedObject.getNumber("shortObjectProperty"),
                PRECISION);
        Assert.assertEquals(7, nestedObject.getNumber("doubleProperty"),
                PRECISION);
        Assert.assertEquals(8, nestedObject.getNumber("doubleObjectProperty"),
                PRECISION);
        Assert.assertEquals(9, nestedObject.getNumber("byteProperty"),
                PRECISION);
        Assert.assertEquals(10, nestedObject.getNumber("byteObjectProperty"),
                PRECISION);
        Assert.assertTrue(nestedObject.getBoolean("booleanProperty"));
        Assert.assertFalse(nestedObject.getBoolean("booleanObjectProperty"));
        Assert.assertEquals('c',
                nestedObject.getString("charProperty").charAt(0));
        Assert.assertEquals('C',
                nestedObject.getString("characterProperty").charAt(0));
        Assert.assertEquals(SomeEnum.SOME_VALUE_2.name(),
                nestedObject.getString("enumProperty"));

        mainRecord = JsonSerializer.toObject(RecordWithRecordAndObject.class,
                json);
        Assert.assertNotNull(mainRecord);

        SomeRecord deserializedRecord = mainRecord.record;
        Assert.assertEquals("someone", deserializedRecord.name);
        Assert.assertEquals(42, deserializedRecord.age);

        ObjectWithSimpleTypes deserializedObject = mainRecord.object;
        Assert.assertEquals("someProperty",
                deserializedObject.getStringProperty());
        Assert.assertEquals(1, deserializedObject.getIntProperty());
        Assert.assertEquals(Integer.valueOf(2),
                deserializedObject.getIntegerProperty());
        Assert.assertEquals(3, deserializedObject.getLongProperty());
        Assert.assertEquals(Long.valueOf(4),
                deserializedObject.getLongObjectProperty());
        Assert.assertEquals(5, deserializedObject.getShortProperty());
        Assert.assertEquals(Short.valueOf((short) 6),
                deserializedObject.getShortObjectProperty());
        Assert.assertEquals(7, deserializedObject.getDoubleProperty(), 0.00001);
        Assert.assertEquals(Double.valueOf(8),
                deserializedObject.getDoubleObjectProperty());
        Assert.assertEquals(9, deserializedObject.getByteProperty());
        Assert.assertEquals(Byte.valueOf((byte) 10),
                deserializedObject.getByteObjectProperty());
        Assert.assertEquals(true, deserializedObject.isBooleanProperty());
        Assert.assertEquals(Boolean.FALSE,
                deserializedObject.getBooleanObjectProperty());
        Assert.assertEquals('c', deserializedObject.getCharProperty());
        Assert.assertEquals((Character) 'C',
                deserializedObject.getCharacterProperty());
        Assert.assertEquals(SomeEnum.SOME_VALUE_2,
                deserializedObject.getEnumProperty());
    }

    private static ObjectWithSimpleTypes getPopulatedObjectWithSimpleTypes() {
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
        return bean;
    }

    private <T> void assertCollectionItemsAreEqual(Collection<T> collection,
            T... values) {
        int index = 0;
        for (T collectionValue : collection) {
            Assert.assertEquals(collectionValue, values[index]);
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
        Assert.assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        Assert.assertTrue(key + " property should be JsonNull",
                object.get(key) instanceof JsonNull);
    }

    private void assertObjectHas0ValueForKey(JsonObject object, String key) {
        Assert.assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        Assert.assertEquals(key + " should be 0.0 in the JsonObject", 0.0,
                object.getNumber(key), PRECISION);
    }

    private void assertObjectHasFalseValueForKey(JsonObject object,
            String key) {
        Assert.assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        Assert.assertEquals(key + " should be false in the JsonObject", false,
                object.getBoolean(key));
    }

}
