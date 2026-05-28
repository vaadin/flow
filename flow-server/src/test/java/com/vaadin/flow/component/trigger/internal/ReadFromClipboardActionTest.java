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
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleReturnChannel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReadFromClipboardActionTest {

    @Test
    void actionFnWrapsClipboardReadPromiseWithObserverAndChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new ReadFromClipboardAction(p -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Action wraps the inner promise function with OBSERVE_PROMISE +
        // return channel. The inner just invokes the Clipboard.ts helper.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("let channel=$2;let inner=$1;let observer=$0;"
                + "observer(inner(event), channel)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals("return window.Vaadin.Flow.clipboard.readPayload()",
                inner.getBody());
    }

    @Test
    void okChannelInvocation_withPayload_handsTypedRecordToHandler() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<@Nullable ClipboardPayload> received = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new ReadFromClipboardAction(received::add, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode payload = JacksonUtils.createObjectNode();
        payload.put("text", "hello");
        payload.put("html", "<b>hello</b>");
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.set("value", payload);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(1, received.size());
        ClipboardPayload p = received.get(0);
        assertNotNull(p);
        assertEquals("hello", p.text());
        assertEquals("<b>hello</b>", p.html());
    }

    @Test
    void okChannelInvocation_withNullValue_handsNullToHandler() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<@Nullable ClipboardPayload> received = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new ReadFromClipboardAction(received::add, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Clipboard.ts resolves with null when clipboard is empty.
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.set("value", JacksonUtils.nullNode());
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(1, received.size());
        assertNull(received.get(0));
    }

    @Test
    void errChannelInvocation_runsOnErrorWithNameAndMessage() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<PromiseAction.Error> failed = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new ReadFromClipboardAction(p -> {
                }, failed::add));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode error = JacksonUtils.createObjectNode();
        error.put("name", "NotAllowedError");
        error.put("message", "denied");
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", false);
        outcome.set("error", error);
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(1, failed.size());
        assertEquals("NotAllowedError", failed.get(0).name());
        assertEquals("denied", failed.get(0).message());
    }

}
