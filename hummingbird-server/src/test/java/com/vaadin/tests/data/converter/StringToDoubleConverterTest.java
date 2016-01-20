package com.vaadin.tests.data.converter;

import com.vaadin.data.util.converter.StringToDoubleConverter;

import junit.framework.TestCase;

public class StringToDoubleConverterTest extends TestCase {

    StringToDoubleConverter converter = new StringToDoubleConverter();

    public void testNullConversion() {
        assertEquals(null, converter.convertToModel(null, Double.class, null));
    }

    public void testEmptyStringConversion() {
        assertEquals(null, converter.convertToModel("", Double.class, null));
    }

    public void testValueConversion() {
        assertEquals(10.0, converter.convertToModel("10", Double.class, null));
    }
}
