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
import com.vaadin.flow.data.binder.Result;
import org.junit.Test;

public class FloatToDoubleConverterTest {

    FloatToDoubleConverter converter = new FloatToDoubleConverter();

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
        assertEquals(Result.ok(42.0), converter.convertToModel(42.0f, null));
    }

    @Test
    public void testConvertToPresentation() {
        assertEquals(Float.valueOf(42.0f),
                converter.convertToPresentation(42.0, null));
    }

    // Test conversion of extreme values
    @Test
    public void testConvertToModelMaxFloat() {
        assertEquals(Result.ok((double) Float.MAX_VALUE),
                converter.convertToModel(Float.MAX_VALUE, null));
    }

    @Test
    public void testConvertToPresentationMaxFloat() {
        assertEquals(Float.valueOf(Float.MAX_VALUE), converter
                .convertToPresentation((double) Float.MAX_VALUE, null));
    }

    @Test
    public void testConvertToModelMinFloat() {
        assertEquals(Result.ok((double) Float.MIN_VALUE),
                converter.convertToModel(Float.MIN_VALUE, null));
    }

    @Test
    public void testConvertToPresentationMinFloat() {
        assertEquals(Float.valueOf(Float.MIN_VALUE), converter
                .convertToPresentation((double) Float.MIN_VALUE, null));
    }

    @Test
    public void testConvertToModelPositiveInfinity() {
        assertEquals(Result.ok(Double.POSITIVE_INFINITY),
                converter.convertToModel(Float.POSITIVE_INFINITY, null));
    }

    @Test
    public void testConvertToPresentationPositiveInfinity() {
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), converter
                .convertToPresentation(Double.POSITIVE_INFINITY, null));
    }

    @Test
    public void testConvertToModelNegativeInfinity() {
        assertEquals(Result.ok(Double.NEGATIVE_INFINITY),
                converter.convertToModel(Float.NEGATIVE_INFINITY, null));
    }

    @Test
    public void testConvertToPresentationNegativeInfinity() {
        assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), converter
                .convertToPresentation(Double.NEGATIVE_INFINITY, null));
    }

}
