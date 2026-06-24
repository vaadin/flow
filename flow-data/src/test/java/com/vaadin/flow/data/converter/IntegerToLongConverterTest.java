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

public class IntegerToLongConverterTest {

    IntegerToLongConverter converter = new IntegerToLongConverter();

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
        Result<Long> result = converter.convertToModel(42, null);
        assertEquals(Result.ok(42L), result);
    }

    @Test
    public void testConvertToPresentation() {
        Integer value = converter.convertToPresentation(42L, null);
        assertEquals(Integer.valueOf(42), value);
    }
}
