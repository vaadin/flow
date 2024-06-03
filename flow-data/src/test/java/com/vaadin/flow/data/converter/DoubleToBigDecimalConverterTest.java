package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class DoubleToBigDecimalConverterTest {

    DoubleToBigDecimalConverter converter = new DoubleToBigDecimalConverter();

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
        Result<BigDecimal> result = converter.convertToModel(42.42, null);
        assertEquals(Result.ok(BigDecimal.valueOf(42.42)), result);
    }

    @Test
    public void testConvertToPresentation() {
        Double value = converter.convertToPresentation(new BigDecimal("42.42"),
                null);
        assertEquals(Double.valueOf(42.42), value);
    }

    @Test
    public void testConvertToModelWithDifferentScales() {
        Result<BigDecimal> result = converter.convertToModel(42.420, null);
        assertEquals(Result.ok(new BigDecimal("42.42")), result);
    }

    @Test
    public void testConvertToPresentationWithDifferentScales() {
        Double value = converter.convertToPresentation(new BigDecimal("42.420"),
                null);
        assertEquals(Double.valueOf(42.42), value);
    }
}
