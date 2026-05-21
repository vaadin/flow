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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromiseActionTest {

    private static final String PROMISE_EXPR = "Promise.resolve(42)";

    @Test
    void fireAndForget_handlerJsIsJustThePromise_noServerCallback() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new StubPromiseAction());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction handler = handlerOf(singleInstallFn(ui));
        assertEquals(PROMISE_EXPR + ";", handler.getBody(),
                "fire-and-forget overload skips the round-trip");
        assertFalse(captureContainsReturnChannel(handler),
                "fire-and-forget overload captures no return channel");
    }

    @Test
    void withCallbacks_handlerJsAppendsThenCatch_andCapturesReturnChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new StubPromiseAction(() -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction handler = handlerOf(singleInstallFn(ui));
        String body = handler.getBody();
        assertTrue(body.startsWith(PROMISE_EXPR), body);
        assertTrue(body.contains(".then(()=>$0(true,null))"), body);
        assertTrue(body.contains(".catch(e=>$0(false, "), body);
        assertTrue(captureContainsReturnChannel(handler),
                "with callbacks a return channel is captured");
    }

    @Test
    void returnChannelInvocation_withSuccess_runsOnSuccess() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        new DomEventTrigger(button, "click").triggers(
                new StubPromiseAction(() -> succeeded.add("ok"), failed::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ReturnChannelRegistration channel = singleReturnChannel(ui);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(true);
        args.add(JacksonUtils.nullNode());
        channel.invoke(args);

        assertEquals(List.of("ok"), succeeded);
        assertEquals(List.of(), failed);
    }

    @Test
    void returnChannelInvocation_withFailure_runsOnErrorWithMessage() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        new DomEventTrigger(button, "click").triggers(
                new StubPromiseAction(() -> succeeded.add("ok"), failed::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ReturnChannelRegistration channel = singleReturnChannel(ui);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(false);
        args.add("NotAllowedError: blocked by permission policy");
        channel.invoke(args);

        assertEquals(List.of(), succeeded);
        assertEquals(List.of("NotAllowedError: blocked by permission policy"),
                failed);
    }

    private static boolean captureContainsReturnChannel(JsFunction handler) {
        return handler.getCaptures().stream()
                .anyMatch(o -> o instanceof ReturnChannelRegistration);
    }

    private static ReturnChannelRegistration singleReturnChannel(UI ui) {
        List<ReturnChannelRegistration> channels = handlerOf(
                singleInstallFn(ui)).getCaptures().stream()
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

    private static JsFunction handlerOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the handler JsFunction");
        return (JsFunction) o;
    }

    /** Minimal PromiseAction that emits a constant promise expression. */
    private static final class StubPromiseAction extends PromiseAction {
        StubPromiseAction() {
            super();
        }

        StubPromiseAction(SerializableRunnable onSuccess,
                SerializableConsumer<String> onError) {
            super(onSuccess, onError);
        }

        @Override
        protected void appendPromiseExpression(JsBuilder builder,
                StringBuilder out) {
            out.append(PROMISE_EXPR);
        }
    }
}
