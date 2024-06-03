package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class BigDecimalToLongConverterTest {

    BigDecimalToLongConverter converter = new BigDecimalToLongConverter();

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
        Result<Long> result = converter.convertToModel(BigDecimal.valueOf(42),
                null);
        assertEquals(Result.ok(42L), result);
    }

    @Test
    public void testConvertToPresentation() {
        BigDecimal value = converter.convertToPresentation(42L, null);
        assertEquals(BigDecimal.valueOf(42), value);
    }
}
