/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.data.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BigDecimalToDoubleConverterTest {

    BigDecimalToDoubleConverter converter = new BigDecimalToDoubleConverter();

    @Test
    void testNullConversionToModel() {
        assertEquals(Result.ok(null), converter.convertToModel(null, null));
    }

    @Test
    void testNullConversionToPresentation() {
        assertNull(converter.convertToPresentation(null, null));
    }

    @Test
    void testConvertToModel() {
        Result<Double> result = converter
                .convertToModel(BigDecimal.valueOf(42.42), null);
        assertEquals(Result.ok(42.42), result);
    }

    @Test
    void testConvertToPresentation() {
        BigDecimal value = converter.convertToPresentation(42.42, null);
        assertEquals(BigDecimal.valueOf(42.42), value);
    }

    @Test
    void testConvertToModelWithDifferentScales() {
        Result<Double> result = converter
                .convertToModel(new BigDecimal("42.420"), null);
        assertEquals(Result.ok(42.42), result);
    }

    @Test
    void testConvertToPresentationWithDifferentScales() {
        BigDecimal value = converter.convertToPresentation(42.42, null);
        assertEquals(new BigDecimal("42.42"), value);
    }
}
