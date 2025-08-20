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
package com.vaadin.flow.component.internal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.PendingJavaScriptResult.JavaScriptException;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.SingleCaptureConsumer;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

public class PendingJavaScriptInvocationTest {
    private static final JsonNode fooJsonString = JacksonUtils
            .createNode("foo");

    private PendingJavaScriptInvocation invocation;

    private SingleCaptureConsumer<JsonNode> jsonSuccessConsumer;
    private SingleCaptureConsumer<String> stringSuccessConsumer;
    private SingleCaptureConsumer<String> errorConsumer;

    private BiConsumer<JsonNode, Throwable> jsonFutureHandler;
    private BiConsumer<String, Throwable> stringFutureHandler;

    @Before
    public void setUp() {
        invocation = new PendingJavaScriptInvocation(
                new Element("dummy").getNode(), new JavaScriptInvocation(""));

        jsonSuccessConsumer = new SingleCaptureConsumer<>();
        stringSuccessConsumer = new SingleCaptureConsumer<>();
        errorConsumer = new SingleCaptureConsumer<>();

        jsonFutureHandler = futureHandler(jsonSuccessConsumer, errorConsumer);
        stringFutureHandler = futureHandler(stringSuccessConsumer,
                errorConsumer);
    }

    private static <T> BiConsumer<T, Throwable> futureHandler(
            Consumer<T> successConsumer, Consumer<String> errorConsumer) {
        return (successValue, error) -> {
            if (error != null) {
                Assert.assertEquals(JavaScriptException.class,
                        error.getClass());
                errorConsumer.accept(error.getMessage());
            } else {
                successConsumer.accept(successValue);
            }
        };
    }

    @Test
    public void untypedIgnoreErrors_success() {
        invocation.then(jsonSuccessConsumer);

        invocation.complete(fooJsonString);

        assertJsonSuccess();
    }

    @Test
    public void untypedIgnoreErrors_fail() {
        invocation.then(jsonSuccessConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertNoUpdate();
    }

    @Test
    public void untypedCaptureErrors_success() {
        invocation.then(jsonSuccessConsumer, errorConsumer);

        invocation.complete(fooJsonString);

        assertJsonSuccess();
    }

    @Test
    public void untypedCaptureErrors_fail() {
        invocation.then(jsonSuccessConsumer, errorConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    public void untypedFuture_success() {
        invocation.toCompletableFuture().whenComplete(jsonFutureHandler);

        invocation.complete(fooJsonString);

        assertJsonSuccess();
    }

    @Test
    public void untypedFuture_fail() {
        invocation.toCompletableFuture().whenComplete(jsonFutureHandler);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    public void typedIgnoreErrors_success() {
        invocation.then(String.class, stringSuccessConsumer);

        invocation.complete(fooJsonString);

        assertStringSuccess();
    }

    @Test
    public void typedCaptureErrors_fail() {
        invocation.then(String.class, stringSuccessConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertNoUpdate();
    }

    @Test
    public void typedCaptureErrors_success() {
        invocation.then(String.class, stringSuccessConsumer, errorConsumer);

        invocation.complete(fooJsonString);

        assertStringSuccess();
    }

    @Test
    public void typedIgnoreErrors_fail() {
        invocation.then(String.class, stringSuccessConsumer, errorConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    public void typedFuture_success() {
        invocation.toCompletableFuture(String.class)
                .whenComplete(stringFutureHandler);

        invocation.complete(fooJsonString);

        assertStringSuccess();
    }

    @Test
    public void typedFuture_fail() {
        invocation.toCompletableFuture(String.class)
                .whenComplete(stringFutureHandler);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    public void multipleSuccessHandlers() {
        invocation.then(jsonSuccessConsumer, errorConsumer);
        invocation.then(String.class, stringSuccessConsumer);

        invocation.complete(fooJsonString);

        Assert.assertSame(fooJsonString,
                jsonSuccessConsumer.getCapturedValue());
        Assert.assertEquals("foo", stringSuccessConsumer.getCapturedValue());
        assertNoErrorValue();
    }

    @Test
    public void multipleErrorHandlers() {
        SingleCaptureConsumer<String> extraErrorHandler = new SingleCaptureConsumer<>();

        invocation.then(jsonSuccessConsumer, errorConsumer);
        invocation.then(String.class, stringSuccessConsumer, extraErrorHandler);

        invocation.completeExceptionally(fooJsonString);

        assertNoStringSuccessValue();
        assertNoJsonSuccessValue();

        Assert.assertSame("foo", errorConsumer.getCapturedValue());
        Assert.assertSame("foo", extraErrorHandler.getCapturedValue());
    }

    @Test(expected = IllegalStateException.class)
    public void thenAfterSend_throws() {
        invocation.setSentToBrowser();

        invocation.then(jsonSuccessConsumer);
    }

    @Test
    public void subscribeAfterCancel_callFailHandler() {
        invocation.cancelExecution();

        invocation.then(jsonSuccessConsumer, errorConsumer);

        assertFail("Execution canceled");
    }

    @Test
    public void susbscribeBeforeCancel_callFailHandler() {
        invocation.then(jsonSuccessConsumer, errorConsumer);

        invocation.cancelExecution();

        assertFail("Execution canceled");
    }

    @Test
    public void blockFromInvokingThread_throws() throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        session.runWithLock(() -> {
            CompletableFuture<JsonNode> completableFuture = invocation
                    .toCompletableFuture();

            for (Callable<JsonNode> action : createBlockingActions(
                    completableFuture)) {
                try {
                    action.call();
                    Assert.fail(
                            "Blocking on a pending invocation while holding the session lock should throw");
                } catch (IllegalStateException e) {
                    // This is expected
                }
            }

            return null;
        });
    }

    @Test
    public void blockFromSessionThreadAfterCompleting_doesNotThrow()
            throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        session.runWithLock(() -> {
            CompletableFuture<JsonNode> completableFuture = invocation
                    .toCompletableFuture();

            JsonNode value = JacksonUtils.createObjectNode();
            invocation.complete(value);

            for (Callable<JsonNode> action : createBlockingActions(
                    completableFuture)) {
                JsonNode actionValue = action.call();
                Assert.assertSame(value, actionValue);
            }

            return null;
        });
    }

    @Test
    public void blockFromSessionThreadAfterFailing_doesNotThrow()
            throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        session.runWithLock(() -> {
            CompletableFuture<JsonNode> completableFuture = invocation
                    .toCompletableFuture();

            String errorMessage = "error message";
            invocation.completeExceptionally(
                    JacksonUtils.createNode(errorMessage));

            for (Callable<JsonNode> action : createBlockingActions(
                    completableFuture)) {
                try {
                    action.call();
                    Assert.fail("Execution should have failed");
                } catch (ExecutionException | CompletionException e) {
                    JavaScriptException cause = (JavaScriptException) e
                            .getCause();
                    Assert.assertEquals(errorMessage, cause.getMessage());
                }
            }
            return null;
        });
    }

    @Test
    public void blockFromOtherThread_doesNotThrow() throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        VaadinSession.setCurrent(session);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        session.lock();
        try {
            CompletableFuture<JsonNode> completableFuture = invocation
                    .toCompletableFuture();

            List<Future<JsonNode>> futures = createBlockingActions(
                    completableFuture).stream().map(executor::submit)
                    .collect(Collectors.toList());

            Assert.assertEquals("All futures should be pending", 0,
                    futures.stream().filter(Future::isDone).count());

            JsonNode value = JacksonUtils.createObjectNode();
            invocation.complete(value);

            executor.shutdown();
            executor.awaitTermination(100, TimeUnit.MILLISECONDS);

            Assert.assertEquals("All futures should be done", futures.size(),
                    futures.stream().filter(Future::isDone).count());

            futures.forEach(future -> {
                try {
                    JsonNode futureValue = future.get();
                    Assert.assertSame(value, futureValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        } finally {
            session.unlock();
            executor.shutdown();
        }
    }

    private <T> List<Callable<T>> createBlockingActions(
            CompletableFuture<T> completableFuture) {
        return Arrays.asList(completableFuture::get,
                () -> completableFuture.get(1, TimeUnit.HOURS),
                completableFuture::join);
    }

    private void assertStringSuccess() {
        assertNoJsonSuccessValue();
        Assert.assertEquals("foo", stringSuccessConsumer.getCapturedValue());
        assertNoErrorValue();
    }

    private void assertJsonSuccess() {
        Assert.assertSame(fooJsonString,
                jsonSuccessConsumer.getCapturedValue());
        assertNoStringSuccessValue();
        assertNoErrorValue();
    }

    private void assertFail() {
        assertFail("foo");
    }

    private void assertFail(String expectedMessage) {
        assertNoJsonSuccessValue();
        assertNoStringSuccessValue();
        Assert.assertSame(expectedMessage, errorConsumer.getCapturedValue());
    }

    private void assertNoUpdate() {
        assertNoStringSuccessValue();
        assertNoJsonSuccessValue();
        assertNoErrorValue();
    }

    private void assertNoJsonSuccessValue() {
        Assert.assertFalse("Json success consumer should not be invoked",
                jsonSuccessConsumer.isCaptured());
    }

    private void assertNoStringSuccessValue() {
        Assert.assertFalse("String success consumer should not be invoked",
                stringSuccessConsumer.isCaptured());
    }

    private void assertNoErrorValue() {
        Assert.assertFalse("Error consumer should not be invoked",
                errorConsumer.isCaptured());
    }
}
