/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.flow.data.binder.Result;

import java.math.BigDecimal;

public class IntegerToBigDecimalConverterTest {

    IntegerToBigDecimalConverter converter = new IntegerToBigDecimalConverter();

    @Test
    public void testNullConversionToModel() {
        assertEquals(converter.convertToModel(null, null), Result.ok(null));
    }

    @Test
    public void testNullConversionToPresentation() {
        assertNull(converter.convertToPresentation(null, null));
    }

    @Test
    public void testConvertToModel() {
        assertEquals(Result.ok(BigDecimal.valueOf(42)),
                converter.convertToModel(42, null));
    }

    @Test
    public void testConvertToPresentation() {
        assertEquals(Integer.valueOf(42),
                converter.convertToPresentation(BigDecimal.valueOf(42), null));
    }
}
