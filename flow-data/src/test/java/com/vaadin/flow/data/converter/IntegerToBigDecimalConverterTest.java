/*
 * Copyright 2000-2025 Vaadin Ltd.
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
