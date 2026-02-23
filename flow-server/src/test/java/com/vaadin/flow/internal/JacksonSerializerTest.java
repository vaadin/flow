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
package com.vaadin.flow.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link JacksonSerializer}.
 */
class JacksonSerializerTest {

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
        private String[] arrayOfStrings;

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

        public String[] getArrayOfStrings() {
            return arrayOfStrings;
        }

        public void setArrayOfStrings(String[] arrayOfStrings) {
            this.arrayOfStrings = arrayOfStrings;
        }
    }

    @Test
    public void serializeBasicTypes_returnJsonBasicTypes() {
        JsonNode json = JacksonSerializer.toJson("someString");
        assertTrue(json instanceof StringNode,
                "The JsonNode should be instanceof JsonString");
        assertEquals("someString", json.asString());

        json = JacksonSerializer.toJson(0);
        assertTrue(json instanceof NumericNode,
                "The JsonNode should be instanceof JsonNumber");
        assertEquals(0.0, json.doubleValue(), PRECISION);

        json = JacksonSerializer.toJson(0.0);
        assertTrue(json instanceof NumericNode,
                "The JsonNode should be instanceof JsonNumber");
        assertEquals(0.0, json.doubleValue(), PRECISION);

        json = JacksonSerializer.toJson(0l);
        assertTrue(json instanceof NumericNode,
                "The JsonNode should be instanceof JsonNumber");
        assertEquals(0.0, json.doubleValue(), PRECISION);

        json = JacksonSerializer.toJson((byte) 0);
        assertTrue(json instanceof NumericNode,
                "The JsonNode should be instanceof JsonNumber");
        assertEquals(0.0, json.doubleValue(), PRECISION);

        json = JacksonSerializer.toJson((short) 0);
        assertTrue(json instanceof NumericNode,
                "The JsonNode should be instanceof JsonNumber");
        assertEquals(0.0, json.doubleValue(), PRECISION);

        json = JacksonSerializer.toJson(true);
        assertTrue(json instanceof BooleanNode,
                "The JsonNode should be instanceof JsonBoolean");
        assertTrue(json.asBoolean());

        json = JacksonSerializer.toJson(false);
        assertTrue(json instanceof BooleanNode,
                "The JsonNode should be instanceof JsonBoolean");
        assertFalse(json.asBoolean());

        json = JacksonSerializer.toJson(SomeEnum.SOME_VALUE_1);
        assertTrue(json instanceof StringNode,
                "The JsonNode should be instanceof JsonString");
        assertEquals(SomeEnum.SOME_VALUE_1.name(), json.asString());
    }

    @Test
    public void serializeNull_returnNull() {
        JsonNode json = JacksonSerializer.toJson((Object) null);
        assertTrue(json instanceof NullNode,
                "The JsonNode should be instanceof JsonNull");
    }

    @Test
    public void serializeEmptyObjectWithBasicTypes_returnJsonObjectWithEmptyProperties() {
        ObjectWithSimpleTypes bean = new ObjectWithSimpleTypes();
        JsonNode json = JacksonSerializer.toJson(bean);
        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        ObjectNode object = (ObjectNode) json;
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
        assertTrue(object.has("charProperty"));
        assertEquals((char) 0, object.get("charProperty").asString().charAt(0));

        bean = JacksonSerializer.toObject(ObjectWithSimpleTypes.class, json);
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
        ObjectWithSimpleTypes bean = getPopulatedObjectWithSimpleTypes();

        JsonNode json = JacksonSerializer.toJson(bean);
        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode object = json;
        assertEquals("someProperty", object.get("stringProperty").asString());
        assertEquals(1, object.get("intProperty").intValue(), PRECISION);
        assertEquals(2, object.get("integerProperty").intValue(), PRECISION);
        assertEquals(3, object.get("longProperty").longValue(), PRECISION);
        assertEquals(4, object.get("longObjectProperty").longValue(),
                PRECISION);
        assertEquals(5, object.get("shortProperty").shortValue(), PRECISION);
        assertEquals(6, object.get("shortObjectProperty").shortValue(),
                PRECISION);
        assertEquals(7, object.get("doubleProperty").doubleValue(), PRECISION);
        assertEquals(8, object.get("doubleObjectProperty").doubleValue(),
                PRECISION);
        assertEquals(9, object.get("byteProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(10,
                object.get("byteObjectProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(true, object.get("booleanProperty").booleanValue());
        assertEquals(false, object.get("booleanObjectProperty").booleanValue());
        assertEquals('c', object.get("charProperty").asString().charAt(0));
        assertEquals('C', object.get("characterProperty").asString().charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_2.name(),
                object.get("enumProperty").asString());

        bean = JacksonSerializer.toObject(ObjectWithSimpleTypes.class, json);
        assertNotNull(bean);
        assertEquals("someProperty", bean.getStringProperty());
        assertEquals(1, bean.getIntProperty());
        assertEquals(Integer.valueOf(2), bean.getIntegerProperty());
        assertEquals(3, bean.getLongProperty());
        assertEquals(Long.valueOf(4), bean.getLongObjectProperty());
        assertEquals(5, bean.getShortProperty());
        assertEquals(Short.valueOf((short) 6), bean.getShortObjectProperty());
        assertEquals(7, bean.getDoubleProperty(), 0.00001);
        assertEquals(Double.valueOf(8), bean.getDoubleObjectProperty());
        assertEquals(9, bean.getByteProperty());
        assertEquals(Byte.valueOf((byte) 10), bean.getByteObjectProperty());
        assertEquals(true, bean.isBooleanProperty());
        assertEquals(Boolean.FALSE, bean.getBooleanObjectProperty());
        assertEquals('c', bean.getCharProperty());
        assertEquals((Character) 'C', bean.getCharacterProperty());
        assertEquals(SomeEnum.SOME_VALUE_2, bean.getEnumProperty());
    }

    @Test
    public void serializeEmptyObjectWithObjects_returnJsonObjectWithNullProperties() {
        ObjectWithOtherObjects bean = new ObjectWithOtherObjects();

        JsonNode json = JacksonSerializer.toJson(bean);

        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode jsonObject = json;
        assertTrue(jsonObject.has("object1"));
        assertTrue(jsonObject.get("object1") instanceof NullNode);
        assertTrue(jsonObject.has("object2"));
        assertTrue(jsonObject.get("object2") instanceof NullNode);
        assertTrue(jsonObject.has("record"));
        assertTrue(jsonObject.get("record") instanceof NullNode);

        bean = JacksonSerializer.toObject(ObjectWithOtherObjects.class, json);

        assertNotNull(bean, "The deserialized object should not be null");
        assertNull(bean.getObject1());
        assertNull(bean.getObject2());
        assertNull(bean.getRecord());
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

        JsonNode json = JacksonSerializer.toJson(bean);

        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode object = json.get("object1");
        assertNotNull(object, "The object1 should be not be null");

        assertEquals("someProperty", object.get("stringProperty").asString());
        assertEquals(1, object.get("intProperty").intValue(), PRECISION);
        assertEquals(2, object.get("integerProperty").intValue(), PRECISION);
        assertEquals(3, object.get("longProperty").longValue(), PRECISION);
        assertEquals(4, object.get("longObjectProperty").longValue(),
                PRECISION);
        assertEquals(5, object.get("shortProperty").shortValue(), PRECISION);
        assertEquals(6, object.get("shortObjectProperty").shortValue(),
                PRECISION);
        assertEquals(7, object.get("doubleProperty").doubleValue(), PRECISION);
        assertEquals(8, object.get("doubleObjectProperty").doubleValue(),
                PRECISION);
        assertEquals(9, object.get("byteProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(10,
                object.get("byteObjectProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(true, object.get("booleanProperty").booleanValue());
        assertEquals(false, object.get("booleanObjectProperty").booleanValue());
        assertEquals('c', object.get("charProperty").asString().charAt(0));
        assertEquals('C', object.get("characterProperty").asString().charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_2.name(),
                object.get("enumProperty").asString());

        object = json.get("object2");
        assertNotNull(object, "The object2 should be not be null");

        assertEquals("someOtherProperty",
                object.get("stringProperty").asString());
        assertEquals(10, object.get("intProperty").intValue(), PRECISION);
        assertEquals(20, object.get("integerProperty").intValue(), PRECISION);
        assertEquals(30, object.get("longProperty").longValue(), PRECISION);
        assertEquals(40, object.get("longObjectProperty").longValue(),
                PRECISION);
        assertEquals(50, object.get("shortProperty").shortValue(), PRECISION);
        assertEquals(60, object.get("shortObjectProperty").shortValue(),
                PRECISION);
        assertEquals(70, object.get("doubleProperty").doubleValue(), PRECISION);
        assertEquals(80, object.get("doubleObjectProperty").doubleValue(),
                PRECISION);
        assertEquals(90, object.get("byteProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(100,
                object.get("byteObjectProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(true, object.get("booleanProperty").booleanValue());
        assertEquals(false, object.get("booleanObjectProperty").booleanValue());
        assertEquals('d', object.get("charProperty").asString().charAt(0));
        assertEquals('D', object.get("characterProperty").asString().charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_1.name(),
                object.get("enumProperty").asString());

        object = json.get("record");
        assertNotNull(object, "The record should be not be null");
        assertEquals("someone", object.get("name").asString());
        assertEquals(42, object.get("age").intValue(), PRECISION);
    }

    @Test
    public void serializeEmptyRecursiveObject_returnJsonObjectWithNullProperties() {
        RecursiveObject bean = new RecursiveObject();

        JsonNode json = JacksonSerializer.toJson(bean);

        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode jsonObject = json;
        assertTrue(jsonObject.has("recursive"));
        assertTrue(jsonObject.get("recursive") instanceof NullNode);
        assertEquals(0, jsonObject.get("index").intValue(), PRECISION);

        bean = JacksonSerializer.toObject(RecursiveObject.class, json);

        assertNotNull(bean, "The deserialized object should not be null");
        assertNull(bean.getRecursive());
        assertEquals(0, bean.getIndex());
    }

    @Test
    public void serializePopulatedRecursiveObject_returnJsonObjectWithPopulatedProperties() {
        final int recursions = 10;
        RecursiveObject bean = createRecusiveObject(recursions, 0);

        JsonNode json = JacksonSerializer.toJson(bean);
        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode object = json;
        for (int i = 0; i < recursions; i++) {
            assertEquals(i, object.get("index").intValue(), PRECISION);
            if (i < recursions - 1) {
                object = object.get("recursive");
            } else {
                assertTrue(object.get("recursive") instanceof NullNode);
            }
        }

        bean = JacksonSerializer.toObject(RecursiveObject.class, json);

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

        JsonNode json = JacksonSerializer.toJson(bean);

        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode jsonObject = json;
        assertTrue(jsonObject.has("listOfStrings"));
        assertTrue(jsonObject.get("listOfStrings") instanceof NullNode);
        assertTrue(jsonObject.has("setOfIntegers"));
        assertTrue(jsonObject.get("setOfIntegers") instanceof NullNode);
        assertTrue(jsonObject.has("linkedListOfBooleans"));
        assertTrue(jsonObject.get("linkedListOfBooleans") instanceof NullNode);
        assertTrue(jsonObject.has("arrayListOfDoubles"));
        assertTrue(jsonObject.get("arrayListOfDoubles") instanceof NullNode);
        assertTrue(jsonObject.has("arrayOfStrings"));
        assertTrue(jsonObject.get("arrayOfStrings") instanceof NullNode);

        bean = JacksonSerializer.toObject(ObjectWithBasicCollections.class,
                json);

        assertNotNull(bean, "The deserialized object should not be null");
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
        bean.setArrayOfStrings(new String[] { "string3", "string4" });

        JsonNode json = JacksonSerializer.toJson(bean);

        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode jsonObject = json;
        ArrayNode array = (ArrayNode) jsonObject.get("listOfStrings");
        assertEquals("string1", array.get(0).asString());
        assertEquals("string2", array.get(1).asString());

        array = (ArrayNode) jsonObject.get("setOfIntegers");
        assertEquals(3, array.get(0).intValue(), PRECISION);
        assertEquals(4, array.get(1).intValue(), PRECISION);

        array = (ArrayNode) jsonObject.get("linkedListOfBooleans");
        assertEquals(true, array.get(0).booleanValue());
        assertEquals(false, array.get(1).booleanValue());

        array = (ArrayNode) jsonObject.get("arrayListOfDoubles");
        assertEquals(5, array.get(0).doubleValue(), PRECISION);
        assertEquals(6, array.get(1).doubleValue(), PRECISION);

        array = (ArrayNode) jsonObject.get("arrayOfStrings");
        assertEquals("string3", array.get(0).asString());
        assertEquals("string4", array.get(1).asString());

        bean = JacksonSerializer.toObject(ObjectWithBasicCollections.class,
                json);

        assertNotNull(bean, "The deserialized object should not be null");
        assertCollectionItemsAreEqual(bean.getListOfStrings(), "string1",
                "string2");
        assertCollectionItemsAreEqual(bean.getSetOfIntegers(), 3, 4);
        assertCollectionItemsAreEqual(bean.getLinkedListOfBooleans(), true,
                false);
        assertCollectionItemsAreEqual(bean.getArrayListOfDoubles(), 5.0, 6.0);
        assertCollectionItemsAreEqual(List.of(bean.getArrayOfStrings()),
                "string3", "string4");
    }

    @Test
    public void serializeRecordWithRecordAndObject_returnJsonObjectWithPopulatedProperties() {
        SomeRecord record = new SomeRecord("someone", 42);
        ObjectWithSimpleTypes bean = getPopulatedObjectWithSimpleTypes();
        RecordWithRecordAndObject mainRecord = new RecordWithRecordAndObject(
                record, bean);

        JsonNode json = JacksonSerializer.toJson(mainRecord);
        assertTrue(json instanceof ObjectNode,
                "The JsonNode should be instanceof JsonObject");

        JsonNode object = json;
        JsonNode nestedRecord = object.get("record");
        JsonNode nestedObject = object.get("object");
        assertNotNull(nestedRecord);
        assertNotNull(nestedObject);

        assertEquals("someone", nestedRecord.get("name").asString());
        assertEquals(42, nestedRecord.get("age").intValue(), PRECISION);

        assertEquals("someProperty",
                nestedObject.get("stringProperty").asString());
        assertEquals(1, nestedObject.get("intProperty").intValue(), PRECISION);
        assertEquals(2, nestedObject.get("integerProperty").intValue(),
                PRECISION);
        assertEquals(3, nestedObject.get("longProperty").longValue(),
                PRECISION);
        assertEquals(4, nestedObject.get("longObjectProperty").longValue(),
                PRECISION);
        assertEquals(5, nestedObject.get("shortProperty").shortValue(),
                PRECISION);
        assertEquals(6, nestedObject.get("shortObjectProperty").shortValue(),
                PRECISION);
        assertEquals(7, nestedObject.get("doubleProperty").doubleValue(),
                PRECISION);
        assertEquals(8, nestedObject.get("doubleObjectProperty").doubleValue(),
                PRECISION);
        assertEquals(9,
                nestedObject.get("byteProperty").numberValue().byteValue(),
                PRECISION);
        assertEquals(10, nestedObject.get("byteObjectProperty").numberValue()
                .byteValue(), PRECISION);
        assertTrue(nestedObject.get("booleanProperty").asBoolean());
        assertFalse(nestedObject.get("booleanObjectProperty").asBoolean());
        assertEquals('c',
                nestedObject.get("charProperty").asString().charAt(0));
        assertEquals('C',
                nestedObject.get("characterProperty").asString().charAt(0));
        assertEquals(SomeEnum.SOME_VALUE_2.name(),
                nestedObject.get("enumProperty").asString());

        mainRecord = JacksonSerializer.toObject(RecordWithRecordAndObject.class,
                json);
        assertNotNull(mainRecord);

        SomeRecord deserializedRecord = mainRecord.record;
        assertEquals("someone", deserializedRecord.name);
        assertEquals(42, deserializedRecord.age);

        ObjectWithSimpleTypes deserializedObject = mainRecord.object;
        assertEquals("someProperty", deserializedObject.getStringProperty());
        assertEquals(1, deserializedObject.getIntProperty());
        assertEquals(Integer.valueOf(2),
                deserializedObject.getIntegerProperty());
        assertEquals(3, deserializedObject.getLongProperty());
        assertEquals(Long.valueOf(4),
                deserializedObject.getLongObjectProperty());
        assertEquals(5, deserializedObject.getShortProperty());
        assertEquals(Short.valueOf((short) 6),
                deserializedObject.getShortObjectProperty());
        assertEquals(7, deserializedObject.getDoubleProperty(), 0.00001);
        assertEquals(Double.valueOf(8),
                deserializedObject.getDoubleObjectProperty());
        assertEquals(9, deserializedObject.getByteProperty());
        assertEquals(Byte.valueOf((byte) 10),
                deserializedObject.getByteObjectProperty());
        assertEquals(true, deserializedObject.isBooleanProperty());
        assertEquals(Boolean.FALSE,
                deserializedObject.getBooleanObjectProperty());
        assertEquals('c', deserializedObject.getCharProperty());
        assertEquals((Character) 'C',
                deserializedObject.getCharacterProperty());
        assertEquals(SomeEnum.SOME_VALUE_2,
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

    private void assertObjectHasNullValueForKey(ObjectNode object, String key) {
        assertTrue(object.has(key),
                key + " should be present in the JsonObject");
        assertTrue(object.get(key) instanceof NullNode,
                key + " property should be JsonNull");
    }

    private void assertObjectHas0ValueForKey(ObjectNode object, String key) {
        assertTrue(object.has(key),
                key + " should be present in the JsonObject");
        assertEquals(0.0, object.get(key).doubleValue(), PRECISION,
                key + " should be 0.0 in the JsonObject");
    }

    private void assertObjectHasFalseValueForKey(ObjectNode object,
            String key) {
        assertTrue(object.has(key),
                key + " should be present in the JsonObject");
        assertEquals(false, object.get(key).booleanValue(),
                key + " should be false in the JsonObject");
    }

}
