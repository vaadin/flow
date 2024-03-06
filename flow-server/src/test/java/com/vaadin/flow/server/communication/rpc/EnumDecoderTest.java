/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;

public class EnumDecoderTest {

    private StringToEnumDecoder decoder = new StringToEnumDecoder();

    enum Title {
        MR, MRS;
    }

    @Test
    public void isApplicable_applicableToStringAndEnum() {
        Assert.assertTrue(
                decoder.isApplicable(Json.create("foo"), Title.class));
    }

    @Test
    public void isApplicable_notApplicableToBooleanAndEnum() {
        Assert.assertFalse(decoder.isApplicable(Json.create(true), Enum.class));
    }

    @Test
    public void isApplicable_notApplicableToStringAndString() {
        Assert.assertFalse(
                decoder.isApplicable(Json.create("foo"), String.class));
    }

    @Test
    public void isApplicable_notApplicableToStringAndAbstractEnum() {
        Assert.assertFalse(
                decoder.isApplicable(Json.create("foo"), Enum.class));
    }

    @Test
    public void stringToEnum_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Title title = Title.MRS;
        Title decoded = decoder.decode(Json.create(title.name()), Title.class);
        Assert.assertEquals(title, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringToEnum_nonConvertableString_valueIsConverted()
            throws RpcDecodeException {
        decoder.decode(Json.create("foo"), Title.class);
    }

}
