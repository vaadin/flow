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
package com.vaadin.flow.component;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.internal.JacksonUtils;

class JavaScriptInvocationTest {
    @Test
    public void testSerializable() {
        JavaScriptInvocation invocation = new UIInternals.JavaScriptInvocation(
                "expression", "string", JacksonUtils.writeValue("jsonString"));

        JavaScriptInvocation deserialized = SerializationUtils
                .deserialize(SerializationUtils.serialize(invocation));

        Assertions.assertNotSame(invocation, deserialized);

        Assertions.assertEquals("expression", deserialized.getExpression());
        Assertions.assertEquals(2, deserialized.getParameters().size());
        Assertions.assertEquals("string", deserialized.getParameters().get(0));
        Assertions.assertEquals("jsonString",
                ((JsonNode) deserialized.getParameters().get(1)).asString());
    }
}
