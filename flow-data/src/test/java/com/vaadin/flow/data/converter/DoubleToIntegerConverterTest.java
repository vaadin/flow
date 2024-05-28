package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

public class DoubleToIntegerConverterTest {

    DoubleToIntegerConverter converter = new DoubleToIntegerConverter();

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
        Result<Integer> result = converter.convertToModel(42.0, null);
        assertEquals(Result.ok(42), result);
    }

    @Test
    public void testConvertToPresentation() {
        Double value = converter.convertToPresentation(42, null);
        assertEquals(Double.valueOf(42), value);
    }

    @Test
    public void testConvertToModelWithDecimalValue() {
        Result<Integer> result = converter.convertToModel(42.99, null);
        assertEquals(Result.ok(42), result);
    }

    @Test
    public void testConvertToPresentationWithDecimalValue() {
        Double value = converter.convertToPresentation(42, null);
        assertEquals(Double.valueOf(42), value);
    }
}
