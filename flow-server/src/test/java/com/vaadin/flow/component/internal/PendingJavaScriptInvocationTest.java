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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.PendingJavaScriptResult.JavaScriptException;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.SingleCaptureConsumer;

import elemental.json.JsonValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PendingJavaScriptInvocationTest {
    private static final JsonNode fooJsonString = JacksonUtils
            .createNode("foo");

    private PendingJavaScriptInvocation invocation;

    private SingleCaptureConsumer<JsonNode> jsonSuccessConsumer;
    private SingleCaptureConsumer<String> stringSuccessConsumer;
    private SingleCaptureConsumer<String> errorConsumer;

    private BiConsumer<JsonNode, Throwable> jsonFutureHandler;
    private BiConsumer<String, Throwable> stringFutureHandler;

    @BeforeEach
    void setUp() {
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
                assertEquals(JavaScriptException.class, error.getClass());
                errorConsumer.accept(error.getMessage());
            } else {
                successConsumer.accept(successValue);
            }
        };
    }

    @Test
    void untypedIgnoreErrors_success() {
        invocation.then(jsonSuccessConsumer);

        invocation.complete(fooJsonString);

        assertJsonSuccess();
    }

    @Test
    void untypedIgnoreErrors_fail() {
        invocation.then(jsonSuccessConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertNoUpdate();
    }

    @Test
    void untypedCaptureErrors_success() {
        invocation.then(jsonSuccessConsumer, errorConsumer);

        invocation.complete(fooJsonString);

        assertJsonSuccess();
    }

    @Test
    void untypedCaptureErrors_fail() {
        invocation.then(jsonSuccessConsumer, errorConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    void untypedFuture_success() {
        invocation.toCompletableFuture().whenComplete(jsonFutureHandler);

        invocation.complete(fooJsonString);

        assertJsonSuccess();
    }

    @Test
    void untypedFuture_fail() {
        invocation.toCompletableFuture().whenComplete(jsonFutureHandler);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    void typedIgnoreErrors_success() {
        invocation.then(String.class, stringSuccessConsumer);

        invocation.complete(fooJsonString);

        assertStringSuccess();
    }

    @Test
    void typedCaptureErrors_fail() {
        invocation.then(String.class, stringSuccessConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertNoUpdate();
    }

    @Test
    void typedCaptureErrors_success() {
        invocation.then(String.class, stringSuccessConsumer, errorConsumer);

        invocation.complete(fooJsonString);

        assertStringSuccess();
    }

    @Test
    void typedIgnoreErrors_fail() {
        invocation.then(String.class, stringSuccessConsumer, errorConsumer);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    void typedFuture_success() {
        invocation.toCompletableFuture(String.class)
                .whenComplete(stringFutureHandler);

        invocation.complete(fooJsonString);

        assertStringSuccess();
    }

    @Test
    void typedFuture_fail() {
        invocation.toCompletableFuture(String.class)
                .whenComplete(stringFutureHandler);

        invocation.completeExceptionally(fooJsonString);

        assertFail();
    }

    @Test
    void multipleSuccessHandlers() {
        invocation.then(jsonSuccessConsumer, errorConsumer);
        invocation.then(String.class, stringSuccessConsumer);

        invocation.complete(fooJsonString);

        assertSame(fooJsonString, jsonSuccessConsumer.getCapturedValue());
        assertEquals("foo", stringSuccessConsumer.getCapturedValue());
        assertNoErrorValue();
    }

    @Test
    void multipleErrorHandlers() {
        SingleCaptureConsumer<String> extraErrorHandler = new SingleCaptureConsumer<>();

        invocation.then(jsonSuccessConsumer, errorConsumer);
        invocation.then(String.class, stringSuccessConsumer, extraErrorHandler);

        invocation.completeExceptionally(fooJsonString);

        assertNoStringSuccessValue();
        assertNoJsonSuccessValue();

        assertSame("foo", errorConsumer.getCapturedValue());
        assertSame("foo", extraErrorHandler.getCapturedValue());
    }

    @Test
    void thenAfterSend_throws() {
        invocation.setSentToBrowser();

        assertThrows(IllegalStateException.class,
                () -> invocation.then(jsonSuccessConsumer));
    }

    @Test
    void subscribeAfterCancel_callFailHandler() {
        invocation.cancelExecution();

        invocation.then(jsonSuccessConsumer, errorConsumer);

        assertFail("Execution canceled");
    }

    @Test
    void susbscribeBeforeCancel_callFailHandler() {
        invocation.then(jsonSuccessConsumer, errorConsumer);

        invocation.cancelExecution();

        assertFail("Execution canceled");
    }

    @Test
    void blockFromInvokingThread_throws() throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        session.runWithLock(() -> {
            CompletableFuture<JsonNode> completableFuture = invocation
                    .toCompletableFuture();

            for (Callable<JsonNode> action : createBlockingActions(
                    completableFuture)) {
                try {
                    action.call();
                    fail("Blocking on a pending invocation while holding the session lock should throw");
                } catch (IllegalStateException e) {
                    // This is expected
                }
            }

            return null;
        });
    }

    @Test
    void blockFromSessionThreadAfterCompleting_doesNotThrow() throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        session.runWithLock(() -> {
            CompletableFuture<JsonNode> completableFuture = invocation
                    .toCompletableFuture();

            JsonNode value = JacksonUtils.createObjectNode();
            invocation.complete(value);

            for (Callable<JsonNode> action : createBlockingActions(
                    completableFuture)) {
                JsonNode actionValue = action.call();
                assertSame(value, actionValue);
            }

            return null;
        });
    }

    @Test
    void blockFromSessionThreadAfterFailing_doesNotThrow() throws Exception {
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
                    fail("Execution should have failed");
                } catch (ExecutionException | CompletionException e) {
                    JavaScriptException cause = (JavaScriptException) e
                            .getCause();
                    assertEquals(errorMessage, cause.getMessage());
                }
            }
            return null;
        });
    }

    @Test
    void blockFromOtherThread_doesNotThrow() throws Exception {
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

            assertEquals(0, futures.stream().filter(Future::isDone).count(),
                    "All futures should be pending");

            JsonNode value = JacksonUtils.createObjectNode();
            invocation.complete(value);

            executor.shutdown();
            executor.awaitTermination(100, TimeUnit.MILLISECONDS);

            assertEquals(futures.size(),
                    futures.stream().filter(Future::isDone).count(),
                    "All futures should be done");

            futures.forEach(future -> {
                try {
                    JsonNode futureValue = future.get();
                    assertSame(value, futureValue);
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

    /**
     * Simulates an add-on compiled against Vaadin 24, where {@code then} took a
     * {@code SerializableConsumer<JsonValue>}. Due to erasure, the call links
     * fine against the Vaadin 25 signature.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private SerializableConsumer<JsonValue> scheduleCallbackFromIncompatibleAddon() {
        SerializableConsumer<JsonValue> legacyCallback = value -> {
        };
        invocation.then((SerializableConsumer) legacyCallback);
        return legacyCallback;
    }

    @Test
    void incompatibleSuccessHandler_exceptionNamesHandlerClass() {
        SerializableConsumer<JsonValue> legacyCallback = scheduleCallbackFromIncompatibleAddon();

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> invocation.complete(JacksonUtils.nullNode()));

        assertTrue(
                exception.getMessage()
                        .contains(legacyCallback.getClass().getName()),
                exception.getMessage());
        assertInstanceOf(ClassCastException.class, exception.getCause());
    }

    @Test
    void successHandlerCastFailureWithoutMessage_rethrownUnchanged() {
        ClassCastException original = new ClassCastException();
        invocation.then(value -> {
            throw original;
        });

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> invocation.complete(JacksonUtils.nullNode()));

        assertSame(original, exception);
    }

    /**
     * A return value that a legacy handler can accept, so that the cast to
     * {@code JsonValue} succeeds and the handler body runs.
     */
    private static class LegacyCompatibleNode extends StringNode
            implements JsonValue {
        LegacyCompatibleNode() {
            super("foo");
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void incompatibleSuccessHandler_castFailureWithoutMessage_exceptionNamesHandlerClass() {
        // Stands in for the pre-allocated exception without a message that
        // the JVM throws for a cast that fails very often
        ClassCastException original = new ClassCastException();
        SerializableConsumer<JsonValue> legacyCallback = value -> {
            throw original;
        };
        invocation.then((SerializableConsumer) legacyCallback);

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> invocation.complete(new LegacyCompatibleNode()));

        assertTrue(
                exception.getMessage()
                        .contains(legacyCallback.getClass().getName()),
                exception.getMessage());
        assertSame(original, exception.getCause());
    }

    private static void acceptLegacyValue(JsonValue value) {
    }

    @Test
    void acceptsElementalJson_legacyHandlers_true() {
        SerializableConsumer<JsonValue> lambda = value -> {
        };
        SerializableConsumer<JsonValue> methodReference = PendingJavaScriptInvocationTest::acceptLegacyValue;
        SerializableConsumer<JsonValue> anonymous = new SerializableConsumer<>() {
            @Override
            public void accept(JsonValue value) {
            }
        };

        assertTrue(PendingJavaScriptInvocation.acceptsElementalJson(lambda));
        assertTrue(PendingJavaScriptInvocation
                .acceptsElementalJson(methodReference));
        assertTrue(PendingJavaScriptInvocation.acceptsElementalJson(anonymous));
    }

    @Test
    void acceptsElementalJson_jsonNodeHandlers_false() {
        SerializableConsumer<JsonNode> lambda = value -> {
        };
        SerializableConsumer<JsonNode> anonymous = new SerializableConsumer<>() {
            @Override
            public void accept(JsonNode value) {
            }
        };

        assertFalse(PendingJavaScriptInvocation.acceptsElementalJson(lambda));
        assertFalse(
                PendingJavaScriptInvocation.acceptsElementalJson(anonymous));
    }

    @Test
    void successHandlerThrowsUnrelatedClassCastException_rethrownUnchanged() {
        ClassCastException original = new ClassCastException(
                "class java.lang.Integer cannot be cast to class java.lang.String");
        invocation.then(value -> {
            throw original;
        });

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> invocation.complete(JacksonUtils.nullNode()));

        assertSame(original, exception);
    }

    @Test
    void successHandlerThrowsUnrelatedNoClassDefFoundError_rethrownUnchanged() {
        NoClassDefFoundError original = new NoClassDefFoundError(
                "com/example/Missing");
        invocation.then(value -> {
            throw original;
        });

        NoClassDefFoundError error = assertThrows(NoClassDefFoundError.class,
                () -> invocation.complete(JacksonUtils.nullNode()));

        assertSame(original, error);
    }

    @Test
    void successHandlerMissingElementalJson_errorNamesHandlerClass() {
        NoClassDefFoundError original = new NoClassDefFoundError(
                "elemental/json/JsonValue");
        SerializableConsumer<JsonNode> handler = value -> {
            throw original;
        };
        invocation.then(handler);

        NoClassDefFoundError error = assertThrows(NoClassDefFoundError.class,
                () -> invocation.complete(JacksonUtils.nullNode()));

        assertTrue(error.getMessage().contains(handler.getClass().getName()),
                error.getMessage());
        assertSame(original, error.getCause());
    }

    @Test
    void typedSuccessHandlerCastFailureWithoutMessage_rethrownUnchanged() {
        ClassCastException original = new ClassCastException();
        invocation.then(String.class, value -> {
            throw original;
        });

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> invocation.complete(fooJsonString));

        assertSame(original, exception);
    }

    private void assertStringSuccess() {
        assertNoJsonSuccessValue();
        assertEquals("foo", stringSuccessConsumer.getCapturedValue());
        assertNoErrorValue();
    }

    private void assertJsonSuccess() {
        assertSame(fooJsonString, jsonSuccessConsumer.getCapturedValue());
        assertNoStringSuccessValue();
        assertNoErrorValue();
    }

    private void assertFail() {
        assertFail("foo");
    }

    private void assertFail(String expectedMessage) {
        assertNoJsonSuccessValue();
        assertNoStringSuccessValue();
        assertSame(expectedMessage, errorConsumer.getCapturedValue());
    }

    private void assertNoUpdate() {
        assertNoStringSuccessValue();
        assertNoJsonSuccessValue();
        assertNoErrorValue();
    }

    private void assertNoJsonSuccessValue() {
        assertFalse(jsonSuccessConsumer.isCaptured(),
                "Json success consumer should not be invoked");
    }

    private void assertNoStringSuccessValue() {
        assertFalse(stringSuccessConsumer.isCaptured(),
                "String success consumer should not be invoked");
    }

    private void assertNoErrorValue() {
        assertFalse(errorConsumer.isCaptured(),
                "Error consumer should not be invoked");
    }
}
