package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

public class FloatToIntegerConverterTest {

    FloatToIntegerConverter converter = new FloatToIntegerConverter();

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
        Result<Integer> result = converter.convertToModel(42.0f, null);
        assertEquals(Result.ok(42), result);
    }

    @Test
    public void testConvertToPresentation() {
        Float value = converter.convertToPresentation(42, null);
        assertEquals(Float.valueOf(42), value);
    }
}
