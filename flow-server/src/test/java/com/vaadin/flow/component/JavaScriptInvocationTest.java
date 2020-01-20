/*
 * Copyright 2000-2020 Vaadin Ltd.
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
