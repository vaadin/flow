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
package com.vaadin.flow.server.communication.rpc;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.JacksonUtils;

public class EnumDecoderTest {

    private StringToEnumDecoder decoder = new StringToEnumDecoder();

    enum Title {
        MR, MRS;
    }

    @Test
    public void isApplicable_applicableToStringAndEnum() {
        Assert.assertTrue(decoder.isApplicable(JacksonUtils.createNode("foo"),
                Title.class));
    }

    @Test
    public void isApplicable_notApplicableToBooleanAndEnum() {
        Assert.assertFalse(decoder.isApplicable(JacksonUtils.createNode(true),
                Enum.class));
    }

    @Test
    public void isApplicable_notApplicableToStringAndString() {
        Assert.assertFalse(decoder.isApplicable(JacksonUtils.createNode("foo"),
                String.class));
    }

    @Test
    public void isApplicable_notApplicableToStringAndAbstractEnum() {
        Assert.assertFalse(decoder.isApplicable(JacksonUtils.createNode("foo"),
                Enum.class));
    }

    @Test
    public void stringToEnum_convertableString_valueIsConverted()
            throws RpcDecodeException {
        Title title = Title.MRS;
        Title decoded = decoder.decode(JacksonUtils.createNode(title.name()),
                Title.class);
        Assert.assertEquals(title, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringToEnum_nonConvertableString_valueIsConverted()
            throws RpcDecodeException {
        decoder.decode(JacksonUtils.createNode("foo"), Title.class);
    }

}
