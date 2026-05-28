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
        // Body forwards the source-fn's value straight into the channel call;
        // both are exposed by name via withParameter.
        assertEquals("let source=$1;let channel=$0;channel(source(event));",
                action.getBody());
        assertEquals(2, action.getCaptures().size());
        assertTrue(
                action.getCaptures()
                        .get(0) instanceof ReturnChannelRegistration,
                "Expected the return channel as the first capture");
        assertTrue(action.getCaptures().get(1) instanceof JsFunction,
                "Expected the source input JsFunction as the second capture");
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
