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
package com.vaadin.flow.js;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PROTOTYPE tests for typed results of JavaScript definitions. */
class TypedJavaScriptResultTest {

    public record Person(String name, int age) implements Serializable {
    }

    @JsDefinition
    interface DeepThought extends Serializable {
        @JsExpression("return 42")
        JavaScriptResult<Integer> getAnswer();

        @JsExpression("return [{name: 'Alice', age: 30}]")
        JavaScriptResult<List<Person>> getPeople();

        @JsExpression("return 1")
        PendingJavaScriptResult getUntyped();
    }

    @JsDefinition
    interface RawJs extends Serializable {
        @SuppressWarnings("rawtypes")
        @JsExpression("return 1")
        JavaScriptResult getRaw();
    }

    @JsDefinition
    interface WildcardJs extends Serializable {
        @JsExpression("return 1")
        JavaScriptResult<?> getWildcard();
    }

    /** Untyped result that records subscriptions instead of a browser. */
    static class FakeResult implements PendingJavaScriptResult {
        final List<SerializableConsumer<JsonNode>> handlers = new ArrayList<>();

        @Override
        public boolean cancelExecution() {
            return false;
        }

        @Override
        public boolean isSentToBrowser() {
            return false;
        }

        @Override
        public void then(SerializableConsumer<JsonNode> resultHandler,
                SerializableConsumer<String> errorHandler) {
            handlers.add(resultHandler);
        }

        void complete(String json) {
            JsonNode node = JacksonUtils.getMapper().readTree(json);
            handlers.forEach(handler -> handler.accept(node));
        }
    }

    private final FakeResult result = new FakeResult();

    private <T> T proxy(Class<T> type) {
        return JsDefinitionProxy.create(type, call -> result);
    }

    @Test
    void typedResult_thenGetsTheDeclaredType() {
        List<Integer> values = new ArrayList<>();

        proxy(DeepThought.class).getAnswer().then(value -> values.add(value));
        result.complete("42");

        assertEquals(List.of(42), values);
    }

    @Test
    void typedResult_nestedGenerics() {
        List<Person> people = new ArrayList<>();

        proxy(DeepThought.class).getPeople().then(people::addAll);
        result.complete("[{\"name\":\"Alice\",\"age\":30}]");

        assertEquals(List.of(new Person("Alice", 30)), people);
    }

    @Test
    void typedResult_toCompletableFuture() {
        CompletableFuture<Integer> future = proxy(DeepThought.class).getAnswer()
                .toCompletableFuture();
        result.complete("42");

        assertEquals(42, future.join());
    }

    @Test
    void typedResult_noHandler_doesNotSubscribe() {
        proxy(DeepThought.class).getAnswer();

        assertTrue(result.handlers.isEmpty());
    }

    @Test
    void typedResult_isSerializable() throws Exception {
        JavaScriptResult<Integer> typed = proxy(DeepThought.class).getAnswer();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(typed);
        }
        Object copy = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())).readObject();
        assertInstanceOf(JavaScriptResult.class, copy);
    }

    @Test
    void untypedResult_isStillThePendingResult() {
        assertSame(result, proxy(DeepThought.class).getUntyped());
    }

    @Test
    void pendingResult_isAJavaScriptResultOfJson() {
        // Code written against the base type accepts the untyped result
        JavaScriptResult<JsonNode> base = proxy(DeepThought.class).getUntyped();
        List<JsonNode> values = new ArrayList<>();
        base.then(values::add);
        result.complete("1");

        assertEquals(1, values.get(0).asInt());
    }

    @Test
    void rawTypedResult_isRefused() {
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(RuntimeException.class, () -> proxy(RawJs.class)));
    }

    @Test
    void wildcardTypedResult_isRefused() {
        assertInstanceOf(IllegalArgumentException.class, assertThrows(
                RuntimeException.class, () -> proxy(WildcardJs.class)));
    }
}
