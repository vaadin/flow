package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.Result;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DoubleToLongConverterTest {
    DoubleToLongConverter converter = new DoubleToLongConverter();

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
        Result<Long> result = converter.convertToModel(42.22, null);
        assertEquals(Result.ok(42L), result);
    }

    @Test
    public void testConvertToPresentation() {
        Double value = converter.convertToPresentation(42L, null);
        assertEquals(Double.valueOf(42), value);
    }
}
