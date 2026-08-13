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
package com.vaadin.flow.dom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsFunctionTest {

    @Test
    void withArguments_alreadySet_throws() {
        JsFunction fn = JsFunction.of("return a;").withArguments("a");
        assertThrows(IllegalStateException.class, () -> fn.withArguments("b"));
    }

    @Test
    void javaSerializationRoundTrip_preservesFunction() throws Exception {
        JsFunction fn = JsFunction
                .of("return $0($1)", JsFunction.of("return $0", "inner"),
                        JacksonUtils.createObjectNode().put("a", 1))
                .withArguments("event");

        JsFunction restored = serializeAndDeserialize(fn);

        assertEquals(fn.getBody(), restored.getBody());
        assertEquals(fn.getArgumentNames(), restored.getArgumentNames());
        assertEquals(2, restored.getCaptures().size());
        assertEquals("inner", ((JsFunction) restored.getCaptures().get(0))
                .getCaptures().get(0));
    }

    @Test
    void javaSerializationRoundTrip_captureNotSerializable_sameClientEncoding()
            throws Exception {
        // A bean Jackson can encode but Java serialization cannot write. It is
        // stored as the JSON it encodes to, so the browser sees no difference.
        JsFunction fn = JsFunction.of("return $0", new NotSerializableBean());
        JsonNode expected = JacksonCodec
                .encodeWithTypeInfo(fn.getCaptures().get(0));

        JsFunction restored = serializeAndDeserialize(fn);

        assertEquals(expected,
                JacksonCodec.encodeWithTypeInfo(restored.getCaptures().get(0)));
    }

    private static JsFunction serializeAndDeserialize(JsFunction fn)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(fn);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (JsFunction) in.readObject();
        }
    }

    public static class NotSerializableBean {
        public String getName() {
            return "name";
        }
    }
}
