package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

public class IntegerToDoubleConverterTest {

    IntegerToDoubleConverter converter = new IntegerToDoubleConverter();

    @Test
    public void testNullConversionToModel() {
        assertEquals(converter.convertToModel(null, null), Result.ok(null));
    }

    @Test
    public void testNullConversionToPresentation() {
        assertNull(converter.convertToPresentation(null, null));
    }

    @Test
    public void testConvertToModel() {
        assertEquals(Result.ok(42.0), converter.convertToModel(42, null));
    }

    @Test
    public void testConvertToPresentation() {
        assertEquals(Integer.valueOf(42), converter.convertToPresentation(42.0, null));
    }
}
