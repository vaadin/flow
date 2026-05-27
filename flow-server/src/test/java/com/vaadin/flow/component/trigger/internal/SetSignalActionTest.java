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

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetSignalActionTest {

    @Test
    void channelInvocation_updatesSignal() {
        // Wire-level mechanics are exercised in CallbackActionTest; this test
        // just verifies the SetSignalAction subclass wires signal::set as the
        // callback so an invocation reaches the signal.
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        ui.getElement().appendChild(input.getElement());

        ValueSignal<String> signal = new ValueSignal<>("");
        DomEventTrigger trigger = new DomEventTrigger(input, "input");
        trigger.triggers(new SetSignalAction<>(signal, String.class,
                new PropertyInput<>(input, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add("hello");
        singleReturnChannel(ui).invoke(args);

        assertEquals("hello", signal.peek());
    }

    @Test
    void signalWithSuperType_acceptsNarrowerValueType() {
        // PECS: ValueSignal<? super T> lets a supertype-parameterised signal
        // accept a narrower valueType — e.g. a ValueSignal<Object> consuming
        // String values produced by the source.
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        ui.getElement().appendChild(input.getElement());

        ValueSignal<Object> signal = new ValueSignal<>("");
        DomEventTrigger trigger = new DomEventTrigger(input, "input");
        trigger.triggers(new SetSignalAction<>(signal, String.class,
                new PropertyInput<>(input, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add("hello");
        singleReturnChannel(ui).invoke(args);

        assertEquals("hello", signal.peek());
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
