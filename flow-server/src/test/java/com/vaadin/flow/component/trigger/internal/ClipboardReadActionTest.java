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
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipboardReadActionTest {

    @Test
    void handlerJsCallsClipboardReadPayloadAndRoutesToCallback() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new ClipboardReadAction(p -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // The action delegates to Clipboard.ts and only renders the routing
        // glue — the actual clipboard read lives in
        // window.Vaadin.Flow.clipboard.
        assertEquals(
                "window.Vaadin.Flow.clipboard.readPayload()"
                        + ".then(p=>$0(p)).catch(()=>$0(null));",
                handlerOf(singleInstallFn(ui)).getBody());
    }

    @Test
    void channelInvocation_withPayload_handsTypedRecordToHandler() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<ClipboardPayload> received = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new ClipboardReadAction(received::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put("text", "hello");
        payload.put("html", "<b>hello</b>");
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(payload);
        singleReturnChannel(ui).invoke(args);

        assertEquals(1, received.size());
        assertEquals("hello", received.get(0).text());
        assertEquals("<b>hello</b>", received.get(0).html());
    }

    @Test
    void channelInvocation_withNull_handsNullToHandler() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<ClipboardPayload> received = new ArrayList<>();
        received.add(new ClipboardPayload("sentinel", null));
        new DomEventTrigger(button, "click")
                .triggers(new ClipboardReadAction(received::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(JacksonUtils.nullNode());
        singleReturnChannel(ui).invoke(args);

        assertEquals(2, received.size());
        assertNull(received.get(1));
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
