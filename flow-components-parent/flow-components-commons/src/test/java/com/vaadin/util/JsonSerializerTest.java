package com.vaadin.util;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.JsonBoolean;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

public class JsonSerializerTest {

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
        Assert.assertEquals(0.0, json.asNumber(), 0.000001);

        json = JsonSerializer.toJson(0.0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), 0.000001);

        json = JsonSerializer.toJson(0l);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), 0.000001);

        json = JsonSerializer.toJson((byte) 0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), 0.000001);

        json = JsonSerializer.toJson((short) 0);
        Assert.assertTrue("The JsonValue should be instanceof JsonNumber",
                json instanceof JsonNumber);
        Assert.assertEquals(0.0, json.asNumber(), 0.000001);

        json = JsonSerializer.toJson(true);
        Assert.assertTrue("The JsonValue should be instanceof JsonBoolean",
                json instanceof JsonBoolean);
        Assert.assertTrue(json.asBoolean());

        json = JsonSerializer.toJson(false);
        Assert.assertTrue("The JsonValue should be instanceof JsonBoolean",
                json instanceof JsonBoolean);
        Assert.assertFalse(json.asBoolean());
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

        // char is a different case, it is a string at the json
        Assert.assertTrue(object.hasKey("charProperty"));
        Assert.assertEquals((char) 0,
                object.getString("charProperty").charAt(0));

        char c = 65535;
        short s = 32767;

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

        JsonValue json = JsonSerializer.toJson(bean);
        Assert.assertTrue("The JsonValue should be instanceof JsonObject",
                json instanceof JsonObject);

        JsonObject object = (JsonObject) json;
        Assert.assertEquals("someProperty", object.getString("stringProperty"));
        Assert.assertEquals(1, object.getNumber("intProperty"), 0.000001);
        Assert.assertEquals(2, object.getNumber("integerProperty"), 0.000001);
        Assert.assertEquals(3, object.getNumber("longProperty"), 0.000001);
        Assert.assertEquals(4, object.getNumber("longObjectProperty"),
                0.000001);
        Assert.assertEquals(5, object.getNumber("shortProperty"), 0.000001);
        Assert.assertEquals(6, object.getNumber("shortObjectProperty"),
                0.000001);
        Assert.assertEquals(7, object.getNumber("doubleProperty"), 0.000001);
        Assert.assertEquals(8, object.getNumber("doubleObjectProperty"),
                0.000001);
        Assert.assertEquals(9, object.getNumber("byteProperty"), 0.000001);
        Assert.assertEquals(10, object.getNumber("byteObjectProperty"),
                0.000001);
        Assert.assertEquals(true, object.getBoolean("booleanProperty"));
        Assert.assertEquals(false, object.getBoolean("booleanObjectProperty"));
        Assert.assertEquals('c', object.getString("charProperty").charAt(0));
        Assert.assertEquals('C',
                object.getString("characterProperty").charAt(0));
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
                object.getNumber(key), 0.000001);
    }

    private void assertObjectHasFalseValueForKey(JsonObject object,
            String key) {
        Assert.assertTrue(key + " should be present in the JsonObject",
                object.hasKey(key));
        Assert.assertEquals(key + " should be false in the JsonObject", false,
                object.getBoolean(key));
    }

}
