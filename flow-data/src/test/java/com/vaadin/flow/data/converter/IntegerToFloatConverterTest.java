package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntegerToFloatConverterTest {
    IntegerToFloatConverter converter = new IntegerToFloatConverter();

    @Test
    public void testNullConversionToModel() {
        assertEquals(Result.ok(null), converter.convertToModel(null, null));
    }

    @Test
    public void testNullConversionToPresentation() {
        assertNull(converter.convertToPresentation(null, null));
    }

    @Test
    public void testConvertToModel() {
        Result<Float> result = converter.convertToModel(42, null);
        assertEquals(Result.ok(42.0f), result);
    }

    @Test
    public void testConvertToPresentation() {
        Integer value = converter.convertToPresentation(42.0f, null);
        assertEquals(Integer.valueOf(42), value);
    }

    @Test
    public void testConvertToModelWithDecimalValue() {
        Result<Float> result = converter.convertToModel(42, null);
        assertEquals(Result.ok(42.0f), result);
    }

    @Test
    public void testConvertToPresentationWithDecimalValue() {
        Integer value = converter.convertToPresentation(42.99f, null);
        assertEquals(Integer.valueOf(42), value);
    }

}
