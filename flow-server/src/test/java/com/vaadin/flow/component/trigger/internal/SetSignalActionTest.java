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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleReturnChannel;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
