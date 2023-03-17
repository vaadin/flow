/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.typeconversion;

import org.junit.Test;

public class CollectionConversionTest extends BaseTypeConversionTest {

    @Test
    public void should_ConvertToIntegerCollection_When_ReceiveNumberArray() {
        String inputArray = "[1,3,2,3]";
        String expectedArray = "[2,4,3,4]";
        assertEqualExpectedValueWhenCallingMethod("addOneIntegerCollection",
                inputArray, expectedArray);
    }

    @Test
    public void should_ConvertToIntegerSet_When_ReceiveNumberArray() {
        String inputArray = "[1,3,2,3]";
        String expectedArray = "[2,4,3,4]";
        assertEqualExpectedValueWhenCallingMethod("addOneIntegerCollection",
                inputArray, expectedArray);
    }

    @Test
    public void should_ConvertToIntegerCollection_When_ReceiveMixedDecimalNumberArray() {
        String inputArray = "[1,2.1]";
        assertEqualExpectedValueWhenCallingMethod("addOneIntegerCollection",
                inputArray, "[2,3]");
    }

    @Test
    public void should_FailToConvertToIntegerCollection_When_ReceiveMixedDecimalStringNumberArray() {
        String inputArray = "[1,\"3.0\",2,3]";
        assert400ResponseWhenCallingMethod("addOneIntegerCollection",
                inputArray);
    }

    @Test
    public void should_FailToConvertToIntegerCollection_When_ReceiveAString() {
        String inputArray = "\"[1]\"";
        assert400ResponseWhenCallingMethod("addOneIntegerCollection",
                inputArray);
    }

    @Test
    public void should_ConvertToStringCollection_When_ReceiveStringArray() {
        String inputArray = "[\"first\",\"2.0\",\"-3\",\"4\"]";
        String expectedArray = "[\"firstfoo\",\"2.0foo\",\"-3foo\",\"4foo\"]";
        assertEqualExpectedValueWhenCallingMethod("addFooStringCollection",
                inputArray, expectedArray);
    }

    @Test
    public void should_ConvertToStringCollection_When_ReceiveNumberArray() {
        String inputArray = "[1,2,3,4]";
        assertEqualExpectedValueWhenCallingMethod("addFooStringCollection",
                inputArray, "[\"1foo\",\"2foo\",\"3foo\",\"4foo\"]");
    }

    @Test
    public void should_ConvertToDoubleCollection_When_ReceiveNumberArray() {
        String inputArray = "[1.9,3.2,-2.0,0.3]";
        String expectedArray = "[2.9,4.2,-1.0,1.3]";
        assertEqualExpectedValueWhenCallingMethod("addOneDoubleCollection",
                inputArray, expectedArray);
    }

    @Test
    public void should_FailToConvertToDoubleCollection_When_ReceiveArrayContainInteger() {
        String inputArray = "[1]";
        assertEqualExpectedValueWhenCallingMethod("addOneDoubleCollection",
                inputArray, "[2.0]");
    }

    @Test
    public void should_ConvertToDoubleCollection_When_ReceiveArrayContainString() {
        String inputArray = "[\"1.0\"]";
        assertEqualExpectedValueWhenCallingMethod("addOneDoubleCollection",
                inputArray, "[2.0]");
    }

    @Test
    public void should_ConvertToObjectCollection_When_ReceiveArrayObject() {
        String inputArray = "[\"1.0\", 1, 0.0, -99, {\"property\": \"value\"}]";
        String expectedArray = "[\"1.0\",1,0.0,-99,{\"property\":\"value\"}]";
        assertEqualExpectedValueWhenCallingMethod("getObjectCollection",
                inputArray, expectedArray);
    }

    @Test
    public void should_ConvertToEnumMap_When_ReceiveEnumMap() {
        String inputValue = "{\"FIRST\": \"first_value\", \"SECOND\": \"second_value\"}";
        String expectedValue = "{\"FIRST\":\"first_valuefoo\",\"SECOND\":\"second_valuefoo\"}";
        assertEqualExpectedValueWhenCallingMethod("getFooEnumMap", inputValue,
                expectedValue);
    }

    @Test
    public void should_ConvertToEnumSet_When_ReceiveEnumArray() {
        String inputValue = "[\"FIRST\", \"FIRST\", \"SECOND\", \"SECOND\"]";
        String expectedValue = "[\"SECOND\",\"THIRD\"]";
        assertEqualExpectedValueWhenCallingMethod("getNextValueEnumSet",
                inputValue, expectedValue);
    }
}
