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
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleReturnChannel;
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

        JsFunction action = actionOf(singleInstallFn(ui));
        // $0 = the return channel; $1 = the source input's JsFunction. The
        // body forwards the source-fn's value straight into the channel call.
        assertEquals("$0($1(event));", action.getBody());
        assertEquals(2, action.getCaptures().size());
        assertTrue(
                action.getCaptures()
                        .get(0) instanceof ReturnChannelRegistration,
                "Expected $0 to be the return channel");
        assertTrue(action.getCaptures().get(1) instanceof JsFunction,
                "Expected $1 to be the source input JsFunction");
    }

    @Test
    void valueLess_handlerCallsChannelWithNoArgsAndRunsCallback() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        List<String> received = new ArrayList<>();
        DomEventTrigger trigger = new DomEventTrigger(button, "click");
        trigger.triggers(new CallbackAction<>(() -> received.add("ran")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // No value to forward, so the channel ($0) is called with no arguments.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0();", action.getBody());
        assertTrue(
                action.getCaptures()
                        .get(0) instanceof ReturnChannelRegistration,
                "Expected $0 to be the return channel");

        ((ReturnChannelRegistration) action.getCaptures().get(0))
                .invoke(JacksonUtils.createArrayNode());

        assertEquals(List.of("ran"), received);
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

}
