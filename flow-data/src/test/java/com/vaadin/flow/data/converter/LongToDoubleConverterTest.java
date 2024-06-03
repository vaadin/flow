package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

public class LongToDoubleConverterTest {

    LongToDoubleConverter converter = new LongToDoubleConverter();

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
        Result<Double> result = converter.convertToModel(42L, null);
        assertEquals(Result.ok(42.0), result);
    }

    @Test
    public void testConvertToPresentation() {
        Long value = converter.convertToPresentation(42.0, null);
        assertEquals(Long.valueOf(42), value);
    }
}
