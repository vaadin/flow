package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class FloatToBigDecimalConverterTest {

    FloatToBigDecimalConverter converter = new FloatToBigDecimalConverter();

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
        Result<BigDecimal> result = converter.convertToModel(42.0f, null);
        assertEquals(Result.ok(BigDecimal.valueOf(42.0f)), result);
    }

    @Test
    public void testConvertToPresentation() {
        Float value = converter.convertToPresentation(BigDecimal.valueOf(42.0f),
                null);
        assertEquals(Float.valueOf(42.0f), value);
    }
}
