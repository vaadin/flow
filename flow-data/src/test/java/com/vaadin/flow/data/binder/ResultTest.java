/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.SerializableFunction;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ResultTest {

    @Test
    public void testOk() {
        String value = "foo";
        Result<String> ok = Result.ok(value);
        Assert.assertFalse(ok.isError());
        Assert.assertFalse(ok.getMessage().isPresent());
        ok.ifOk(v -> Assert.assertEquals(value, v));
        ok.ifError(msg -> Assert.fail());
    }

    @Test
    public void testError() {
        String message = "foo";
        Result<String> error = Result.error(message);
        Assert.assertTrue(error.isError());
        Assert.assertTrue(error.getMessage().isPresent());
        error.ifOk(v -> Assert.fail());
        error.ifError(msg -> Assert.assertEquals(message, msg));
        Assert.assertEquals(message, error.getMessage().get());
    }

    @Test
    public void of_noException() {
        Result<String> result = Result.of(() -> "", exception -> null);
        Assert.assertTrue(result instanceof SimpleResult);
        Assert.assertFalse(result.isError());
    }

    @Test
    public void of_exception() {
        String message = "foo";
        Result<String> result = Result.of(() -> {
            throw new RuntimeException();
        }, exception -> message);
        Assert.assertTrue(result instanceof SimpleResult);
        Assert.assertTrue(result.isError());
        Assert.assertEquals(message, result.getMessage().get());
    }

    @SuppressWarnings("serial")
    @Test
    public void map_norError_mapperIsApplied() {
        Result<String> result = new SimpleResult<String>("foo", null) {

            @Override
            public <S> Result<S> flatMap(
                    SerializableFunction<String, Result<S>> mapper) {
                return mapper.apply("foo");
            }
        };
        Result<String> mapResult = result.map(value -> {
            Assert.assertEquals("foo", value);
            return "bar";
        });
        Assert.assertTrue(mapResult instanceof SimpleResult);
        Assert.assertFalse(mapResult.isError());
        mapResult.ifOk(v -> Assert.assertEquals("bar", v));
    }

    @SuppressWarnings("serial")
    @Test
    public void map_error_mapperIsApplied() {
        Result<String> result = new SimpleResult<String>("foo", null) {

            @Override
            public <S> Result<S> flatMap(
                    SerializableFunction<String, Result<S>> mapper) {
                return new SimpleResult<>(null, "bar");
            }
        };
        Result<String> mapResult = result.map(value -> {
            Assert.assertEquals("foo", value);
            return "somevalue";
        });
        Assert.assertTrue(mapResult instanceof SimpleResult);
        Assert.assertTrue(mapResult.isError());
        mapResult.ifError(msg -> Assert.assertEquals("bar", msg));
    }
}
