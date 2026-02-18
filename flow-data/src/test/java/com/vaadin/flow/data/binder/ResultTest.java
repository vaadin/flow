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
package com.vaadin.flow.data.binder;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.SerializableFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
class ResultTest {

    @Test
    void testOk() {
        String value = "foo";
        Result<String> ok = Result.ok(value);
        assertFalse(ok.isError());
        assertFalse(ok.getMessage().isPresent());
        ok.ifOk(v -> assertEquals(value, v));
        ok.ifError(msg -> fail());
    }

    @Test
    void testError() {
        String message = "foo";
        Result<String> error = Result.error(message);
        assertTrue(error.isError());
        assertTrue(error.getMessage().isPresent());
        error.ifOk(v -> fail());
        error.ifError(msg -> assertEquals(message, msg));
        assertEquals(message, error.getMessage().get());
    }

    @Test
    void of_noException() {
        Result<String> result = Result.of(() -> "", exception -> null);
        assertTrue(result instanceof SimpleResult);
        assertFalse(result.isError());
    }

    @Test
    void of_exception() {
        String message = "foo";
        Result<String> result = Result.of(() -> {
            throw new RuntimeException();
        }, exception -> message);
        assertTrue(result instanceof SimpleResult);
        assertTrue(result.isError());
        assertEquals(message, result.getMessage().get());
    }

    @SuppressWarnings("serial")
    @Test
    void map_norError_mapperIsApplied() {
        Result<String> result = new SimpleResult<String>("foo", null) {

            @Override
            public <S> Result<S> flatMap(
                    SerializableFunction<String, Result<S>> mapper) {
                return mapper.apply("foo");
            }
        };
        Result<String> mapResult = result.map(value -> {
            assertEquals("foo", value);
            return "bar";
        });
        assertTrue(mapResult instanceof SimpleResult);
        assertFalse(mapResult.isError());
        mapResult.ifOk(v -> assertEquals("bar", v));
    }

    @SuppressWarnings("serial")
    @Test
    void map_error_mapperIsApplied() {
        Result<String> result = new SimpleResult<String>("foo", null) {

            @Override
            public <S> Result<S> flatMap(
                    SerializableFunction<String, Result<S>> mapper) {
                return new SimpleResult<>(null, "bar");
            }
        };
        Result<String> mapResult = result.map(value -> {
            assertEquals("foo", value);
            return "somevalue";
        });
        assertTrue(mapResult instanceof SimpleResult);
        assertTrue(mapResult.isError());
        mapResult.ifError(msg -> assertEquals("bar", msg));
    }
}
