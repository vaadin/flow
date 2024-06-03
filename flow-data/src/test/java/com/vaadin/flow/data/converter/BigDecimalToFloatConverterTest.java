package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class BigDecimalToFloatConverterTest {

    BigDecimalToFloatConverter converter = new BigDecimalToFloatConverter();

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
        Result<Float> result = converter
                .convertToModel(BigDecimal.valueOf(42.0), null);
        assertEquals(Result.ok(42.0f), result);
    }

    @Test
    public void testConvertToPresentation() {
        BigDecimal value = converter.convertToPresentation(42.0f, null);
        assertEquals(BigDecimal.valueOf(42.0), value);
    }
}
