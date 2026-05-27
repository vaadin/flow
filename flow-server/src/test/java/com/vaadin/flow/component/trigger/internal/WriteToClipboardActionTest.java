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
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteToClipboardActionTest {

    @Test
    void fireAndForget_textOnly_callsHelperWithHtmlSlotReturningNull() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class),
                        null));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event))",
                action.getBody());

        // $0 is the text input — a PropertyInput that reads from the field.
        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals("return $0[$1]", text.getBody());

        // $1 is the html slot — the no-op "return null" stand-in.
        JsFunction html = (JsFunction) action.getCaptures().get(1);
        assertEquals("return null", html.getBody());
        assertEquals(List.of(), html.getCaptures());
    }

    @Test
    void fireAndForget_textAndHtml_capturesBothInputFunctions() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new LiteralInput<>("plain"),
                        new LiteralInput<>("<b>html</b>")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event))",
                action.getBody());

        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals("plain", text.getCaptures().get(0));

        JsFunction html = (JsFunction) action.getCaptures().get(1);
        assertEquals("<b>html</b>", html.getCaptures().get(0));
    }

    @Test
    void fireAndForget_htmlOnly_textSlotReturnsNull() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(null,
                        new LiteralInput<>("<b>hi</b>")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event))",
                action.getBody());

        // $0 is the text slot — the no-op stand-in.
        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals("return null", text.getBody());

        // $1 is the html literal input.
        JsFunction html = (JsFunction) action.getCaptures().get(1);
        assertEquals("<b>hi</b>", html.getCaptures().get(0));
    }

    @Test
    void withCallbacks_actionFnWrapsInnerWithObserverAndChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class), null,
                        copied -> {
                        }, err -> {
                        }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // With outcome handling, the action wraps the inner promise function
        // with OBSERVE_PROMISE + the return channel. The inner function still
        // calls writePayload — the action class itself does no string
        // assembly beyond the static body constant.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event))",
                inner.getBody());
    }

    @Test
    void onCopied_receivesTheStringFromTheResolvedPromise() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        List<@Nullable String> copied = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class), null,
                        copied::add, err -> {
                        }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.put("value", "hello clipboard");
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(List.of("hello clipboard"), copied);
    }

    @Test
    void onCopied_receivesNullWhenJsResolvedWithoutValue() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        List<@Nullable String> copied = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class), null,
                        copied::add, err -> {
                        }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        // No "value" field — JS resolved with undefined. The typed Consumer
        // gets null.
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(Arrays.asList((String) null), copied);
    }

    @Test
    void constructor_bothInputsNullRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WriteToClipboardAction(null, null));
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
        // DomEventTrigger captures the action at install $0 by convention.
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }
}
