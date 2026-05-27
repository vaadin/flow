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
package com.vaadin.flow.component.trigger.internal;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromiseActionTest {

    private static final String PROMISE_EXPR = "Promise.resolve(42)";

    @Test
    void fireAndForget_actionFnIsJustThePromise_noServerCallback() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new StubPromiseAction());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Fire-and-forget mode collapses to the subclass' inner JsFunction
        // (no observer wrapping, no return channel) — installed directly as
        // the DOM listener.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("return " + PROMISE_EXPR, action.getBody());
        assertFalse(captureContainsReturnChannel(action),
                "fire-and-forget overload captures no return channel");
    }

    @Test
    void withCallbacks_actionFnWrapsInnerWithObserverAndChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new StubPromiseAction(s -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Action body: observer(inner(event), channel) — $0 is OBSERVE_PROMISE,
        // $1 is the inner JsFunction returning the promise, $2 is the return
        // channel.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        List<@Nullable Object> captures = action.getCaptures();
        assertEquals(3, captures.size(),
                "expected observer + inner + channel captures");
        assertTrue(captures.get(0) instanceof JsFunction,
                "first capture should be the observer JsFunction");
        assertTrue(captures.get(1) instanceof JsFunction,
                "second capture should be the inner promise JsFunction");
        assertTrue(captures.get(2) instanceof ReturnChannelRegistration,
                "third capture should be the return channel");
    }

    @Test
    void okChannelInvocation_withValue_decodesValueToTypedPayload() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<@Nullable String> received = new ArrayList<>();
        List<PromiseAction.Error> failed = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new StubPromiseAction(received::add, failed::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.put("value", "hello");
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(List.of("hello"), received);
        assertEquals(List.of(), failed);
    }

    @Test
    void okChannelInvocation_withNoValue_runsOnSuccessWithNull() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<@Nullable String> received = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new StubPromiseAction(received::add, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        // No "value" field — JS resolved with undefined.
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(1, received.size());
        assertNull(received.get(0),
                "missing JS 'value' should surface as null on the server");
    }

    @Test
    void errChannelInvocation_runsOnErrorWithNameAndMessage() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<@Nullable String> succeeded = new ArrayList<>();
        List<PromiseAction.Error> failed = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new StubPromiseAction(succeeded::add, failed::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode error = JacksonUtils.createObjectNode();
        error.put("name", "NotAllowedError");
        error.put("message", "blocked by permission policy");
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", false);
        outcome.set("error", error);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(List.of(), succeeded);
        assertEquals(1, failed.size());
        assertEquals("NotAllowedError", failed.get(0).name());
        assertEquals("blocked by permission policy", failed.get(0).message());
    }

    private static boolean captureContainsReturnChannel(JsFunction fn) {
        return fn.getCaptures().stream()
                .anyMatch(o -> o instanceof ReturnChannelRegistration);
    }

    private static ReturnChannelRegistration singleReturnChannel(UI ui) {
        JsFunction action = actionOf(singleInstallFn(ui));
        List<ReturnChannelRegistration> channels = action.getCaptures().stream()
                .filter(o -> o instanceof ReturnChannelRegistration)
                .map(o -> (ReturnChannelRegistration) o).toList();
        assertEquals(1, channels.size(),
                "Expected exactly one captured return channel");
        return channels.get(0);
    }

    private static JsFunction singleInstallFn(UI ui) {
        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, pending.size(), "Expected exactly one pending JS");
        return installFn(pending.get(0).getInvocation());
    }

    private static JsFunction installFn(JavaScriptInvocation invocation) {
        Object o = invocation.getParameters().get(2);
        assertTrue(o instanceof JsFunction, "Expected $2 to be a JsFunction");
        return (JsFunction) o;
    }

    private static JsFunction actionOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }

    /**
     * Minimal {@code PromiseAction<String>} that emits a constant promise
     * expression.
     */
    private static final class StubPromiseAction extends PromiseAction<String> {
        StubPromiseAction() {
            super();
        }

        StubPromiseAction(SerializableConsumer<@Nullable String> onSuccess,
                SerializableConsumer<Error> onError) {
            super(String.class, onSuccess, onError);
        }

        @Override
        protected JsFunction renderPromiseExpression(JsBuilder builder) {
            return JsFunction.of("return " + PROMISE_EXPR);
        }
    }
}
