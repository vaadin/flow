package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class BigDecimalToDoubleConverterTest {

    BigDecimalToDoubleConverter converter = new BigDecimalToDoubleConverter();

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
        Result<Double> result = converter.convertToModel(BigDecimal.valueOf(42.42), null);
        assertEquals(Result.ok(42.42), result);
    }

    @Test
    public void testConvertToPresentation() {
        BigDecimal value = converter.convertToPresentation(42.42, null);
        assertEquals(BigDecimal.valueOf(42.42), value);
    }

    @Test
    public void testConvertToModelWithDifferentScales() {
        Result<Double> result = converter.convertToModel(new BigDecimal("42.420"), null);
        assertEquals(Result.ok(42.42), result);
    }

    @Test
    public void testConvertToPresentationWithDifferentScales() {
        BigDecimal value = converter.convertToPresentation(42.42, null);
        assertEquals(new BigDecimal("42.42"), value);
    }
}
