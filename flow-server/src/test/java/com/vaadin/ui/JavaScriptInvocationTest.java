/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import com.vaadin.ui.UIInternals.JavaScriptInvocation;

import elemental.json.Json;
import elemental.json.JsonString;

public class JavaScriptInvocationTest {
    @Test
    public void testSerializable() {
        JavaScriptInvocation invocation = new UIInternals.JavaScriptInvocation(
                "expression", "string", Json.create("jsonString"));

        JavaScriptInvocation deserialized = SerializationUtils
                .deserialize(SerializationUtils.serialize(invocation));

        assertNotSame(invocation, deserialized);

        assertEquals("expression", deserialized.getExpression());
        assertEquals(2, deserialized.getParameters().size());
        assertEquals("string", deserialized.getParameters().get(0));
        assertEquals("jsonString",
                ((JsonString) deserialized.getParameters().get(1)).getString());
    }
}
