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
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallbackActionTest {

    @Test
    void channelInvocation_decodesValueAndInvokesCallback() {
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        ui.getElement().appendChild(input.getElement());

        List<String> received = new ArrayList<>();
        DomEventTrigger trigger = new DomEventTrigger(input, "input");
        trigger.triggers(new CallbackAction<>(String.class, received::add,
                new PropertyInput<>(input, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add("hello");
        singleReturnChannel(ui).invoke(args);

        assertEquals(List.of("hello"), received);
    }

    @Test
    void handlerBody_callsChannelWithSourceExpression() {
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        ui.getElement().appendChild(input.getElement());

        DomEventTrigger trigger = new DomEventTrigger(input, "input");
        trigger.triggers(new CallbackAction<>(String.class, v -> {
        }, new PropertyInput<>(input, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction handler = handlerOf(singleInstallFn(ui));
        // $0 = the return channel (the host element is `this`, so no element
        // capture comes first). The body forwards the source expression
        // straight into the channel call.
        assertEquals("$0(this[\"value\"]);", handler.getBody());
        assertEquals(1, handler.getCaptures().size());
        assertTrue(
                handler.getCaptures()
                        .get(0) instanceof ReturnChannelRegistration,
                "Expected the single capture to be the return channel");
    }

    @Test
    void nullArgument_throwsIllegalState() {
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        ui.getElement().appendChild(input.getElement());

        DomEventTrigger trigger = new DomEventTrigger(input, "input");
        trigger.triggers(new CallbackAction<>(String.class, v -> {
        }, new PropertyInput<>(input, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(JacksonUtils.nullNode());
        assertThrows(IllegalStateException.class,
                () -> singleReturnChannel(ui).invoke(args));
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
