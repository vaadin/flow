package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class BigDecimalToIntegerConverterTest {

    BigDecimalToIntegerConverter converter = new BigDecimalToIntegerConverter();

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
        Result<Integer> result = converter
                .convertToModel(BigDecimal.valueOf(42), null);
        assertEquals(Result.ok(42), result);
    }

    @Test
    public void testConvertToPresentation() {
        BigDecimal value = converter.convertToPresentation(42, null);
        assertEquals(BigDecimal.valueOf(42), value);
    }

    @Test
    public void testConvertToModelWithDecimalValue() {
        Result<Integer> result = converter
                .convertToModel(BigDecimal.valueOf(42.99), null);
        assertEquals(Result.ok(42), result);
    }

    @Test
    public void testConvertToPresentationWithDecimalValue() {
        BigDecimal value = converter.convertToPresentation(42, null);
        assertEquals(BigDecimal.valueOf(42), value);
    }
}
