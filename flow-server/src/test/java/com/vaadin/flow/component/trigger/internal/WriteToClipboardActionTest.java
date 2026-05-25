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
    void fireAndForget_textOnly_emitsWriteWrappedInIIFEResolvingWithText() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class),
                        null));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // IIFE binds the text expression once, calls write([ClipboardItem
        // ({"text/plain": t})]), then resolves with the same t so onCopied
        // (if wired) sees the exact value.
        assertEquals(
                "((t) => navigator.clipboard.write([new ClipboardItem({\"text/plain\":t})]).then(() => t))($0[\"value\"]);",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void fireAndForget_textAndHtml_resolvesWithTextWhenBothSet() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new LiteralInput<>("plain"),
                        new LiteralInput<>("<b>html</b>")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Both slots are packed into one ClipboardItem; .then resolves with
        // the text value (text/plain wins over text/html for onCopied).
        assertEquals(
                "((t,h) => navigator.clipboard.write([new ClipboardItem({\"text/plain\":t,\"text/html\":h})]).then(() => t))(\"plain\",\"<b>html</b>\");",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void fireAndForget_htmlOnly_resolvesWithHtml() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(null,
                        new LiteralInput<>("<b>hi</b>")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Without text, the promise resolves with the html value.
        assertEquals(
                "((h) => navigator.clipboard.write([new ClipboardItem({\"text/html\":h})]).then(() => h))(\"<b>hi</b>\");",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void withCallbacks_handlerWrapsWriteInObserveCall() {
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

        // $0 = OBSERVE_PROMISE JsFunction, $1 = return channel, $2 = field.
        assertEquals(
                "$0(((t) => navigator.clipboard.write([new ClipboardItem({\"text/plain\":t})]).then(() => t))($2[\"value\"]), $1);",
                handlerOf(singleInstallFn(ui)).getBody());
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
        // gets null, honestly reflecting "no value" rather than masking it
        // as an empty string.
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
}
