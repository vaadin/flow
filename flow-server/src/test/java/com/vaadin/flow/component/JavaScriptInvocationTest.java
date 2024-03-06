/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;

import elemental.json.Json;
import elemental.json.JsonString;

public class JavaScriptInvocationTest {
    @Test
    public void testSerializable() {
        JavaScriptInvocation invocation = new UIInternals.JavaScriptInvocation(
                "expression", "string", Json.create("jsonString"));

        JavaScriptInvocation deserialized = SerializationUtils
                .deserialize(SerializationUtils.serialize(invocation));

        Assert.assertNotSame(invocation, deserialized);

        Assert.assertEquals("expression", deserialized.getExpression());
        Assert.assertEquals(2, deserialized.getParameters().size());
        Assert.assertEquals("string", deserialized.getParameters().get(0));
        Assert.assertEquals("jsonString",
                ((JsonString) deserialized.getParameters().get(1)).getString());
    }
}
