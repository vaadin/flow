/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server.communication.rpc;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.communication.rpc.RpcDecodeException;
import com.vaadin.flow.server.communication.rpc.StringToNumberDecoder;

import elemental.json.Json;

public class StringToNumberDecoderTest {

    private StringToNumberDecoder decoder = new StringToNumberDecoder();

    @Test
    public void isApplicable_applicableToStringAndNumber() {
        Assert.assertTrue(
                decoder.isApplicable(Json.create("foo"), Number.class));
    }

    @Test
    public void isApplicable_notApplicableToBooleanAndNumber() {
        Assert.assertFalse(
                decoder.isApplicable(Json.create(true), Number.class));
    }

    @Test
    public void isApplicable_notApplicableToStringAndString() {
        Assert.assertFalse(
                decoder.isApplicable(Json.create("foo"), String.class));
    }

    @Test
    public void isApplicable_notApplicableToStringAndAtomicInteger() {
        Assert.assertFalse(
                decoder.isApplicable(Json.create("foo"), AtomicInteger.class));
    }

    @Test
    public void isApplicable_applicableToStringAndLong() {
        Assert.assertTrue(decoder.isApplicable(Json.create("foo"), Long.class));
    }

    @Test
    public void stringToInteger_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Integer expected = 37;
        Integer value = decoder.decode(Json.create(String.valueOf(expected)),
                Integer.class);
        Assert.assertEquals(expected, value);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToInteger_nonConvertableString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("abc"), Integer.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToInteger_doubleString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("4.2"), Integer.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToInteger_longString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create(String.valueOf(Long.MAX_VALUE)),
                Integer.class);
    }

    @Test
    public void stringToLong_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Long expected = 37l;
        Long value = decoder.decode(Json.create(String.valueOf(expected)),
                Long.class);
        Assert.assertEquals(expected, value);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToLong_nonConvertableString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("abc"), Long.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToLong_doubleString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("4.2"), Long.class);
    }

    @Test
    public void stringToShort_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Short expected = 37;
        Short value = decoder.decode(Json.create(String.valueOf(expected)),
                Short.class);
        Assert.assertEquals(expected, value);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToShort_nonConvertableString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("abc"), Short.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToShort_intString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create(Integer.MAX_VALUE), Short.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToShort_doubleString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("4.2"), Short.class);
    }

    @Test
    public void stringToByte_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Byte expected = 37;
        Byte value = decoder.decode(Json.create(String.valueOf(expected)),
                Byte.class);
        Assert.assertEquals(expected, value);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToByte_nonConvertableString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("abc"), Byte.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToByte_intString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create(Short.MAX_VALUE), Byte.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToByte_doubleString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("4.2"), Byte.class);
    }

    @Test
    public void stringToFloat_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Float expected = 37.72f;
        Float value = decoder.decode(Json.create(String.valueOf(expected)),
                Float.class);
        Assert.assertEquals(expected, value);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToFloat_doubleString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create(String.valueOf(Double.MIN_NORMAL)),
                Float.class);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToFloat_nonConvertableString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("abc"), Float.class);
    }

    @Test
    public void stringToDouble_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Double expected = 823.6349d;
        Double value = decoder.decode(Json.create(String.valueOf(expected)),
                Double.class);
        Assert.assertEquals(expected, value);
    }

    @Test
    public void stringToDouble_minDoubleString_valueIsConverted()
            throws RpcDecodeException {
        // the value is represented in the specific notation. Check that it's
        // not a problem
        Double expected = Double.MIN_NORMAL;
        Double value = decoder.decode(Json.create(String.valueOf(expected)),
                Double.class);
        Assert.assertEquals(expected, value);
    }

    @Test(expected = RpcDecodeException.class)
    public void stringToDoublet_nonConvertableString_exceptionIsThrown()
            throws RpcDecodeException {
        decoder.decode(Json.create("abc"), Double.class);
    }

    @Test
    public void stringToNumber_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Double expected = 823.6349d;
        Number value = decoder.decode(Json.create(String.valueOf(expected)),
                Number.class);
        Assert.assertEquals(expected, value);
    }
}
