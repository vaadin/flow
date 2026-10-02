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
package com.vaadin.flow.component.trigger;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the client-action seam: an action bound to a sink instead of to a
 * component, so a renderer can fire it once per element it renders on the
 * client while the server holds a single binding.
 */
class ClientActionTest {

    private static class Div extends Component {
        Div() {
            super(new Element("div"));
        }
    }

    /**
     * Stands in for the renderer: keeps the rendered action functions so they
     * can be handed to the client-side template.
     */
    private static class RecordingSink implements ClientActionSink {

        private final List<JsFunction> installed = new ArrayList<>();

        @Override
        public Registration install(JsFunction action) {
            installed.add(action);
            return () -> installed.remove(action);
        }
    }

    @Test
    void itemProperty_actionReadsValueFromTheRowItFiredFor() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();

        Clipboard.write().text(ClientValue.itemProperty("email"))
                .bindTo(container.getElement(), sink);

        // One binding for the whole renderer, whatever the row count.
        assertEquals(1, sink.installed.size());
        JsFunction action = sink.installed.get(0);

        // The renderer calls the function from its own event binding and
        // supplies the row it fired for as the context argument.
        assertEquals(List.of("event", "context"), action.getArgumentNames());
        assertEquals(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event, context), $1(event, context), $2(event, context))",
                action.getBody());

        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals(List.of("event", "context"), text.getArgumentNames());
        assertEquals("return context[$0][$1]", text.getBody());
        assertEquals(List.of("item", "email"), text.getCaptures());
    }

    @Test
    void unarmedCheckPasses_andRegistrationDetachesTheBinding() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();

        Registration registration = Clipboard.write()
                .text(ClientValue.itemProperty("email"))
                .bindTo(container.getElement(), sink);

        // The action is committed as part of binding, so the trigger is armed
        // and the before-response check does not fail.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        registration.remove();
        assertTrue(sink.installed.isEmpty(),
                "removing the registration must detach the installed function");
    }

    @Test
    void observedWrite_reportsOutcomeThroughTheContainerNode() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();
        List<String> copied = new ArrayList<>();

        Clipboard.write()
                .text(ClientValue.itemProperty("email"), copied::add,
                        error -> copied.add("error: " + error.name()))
                .bindTo(container.getElement(), sink);

        // The outcome comes back over a return channel on the renderer's own
        // node — the row identity is not part of it, only the copied value.
        JsFunction action = sink.installed.get(0);
        Object channel = action.getCaptures().stream()
                .filter(ReturnChannelRegistration.class::isInstance).findFirst()
                .orElseThrow();
        assertInstanceOf(ReturnChannelRegistration.class, channel);
        assertFalse(action.getBody().contains("context["),
                "the outcome wrapper itself carries no context");
    }

    @Test
    void contextValue_onATriggerWithoutContext_failsOnTheServer() {
        // A context-dependent value bound to a plain click trigger would
        // evaluate to undefined in the browser; it is rejected here instead.
        UI ui = new MockUI();
        Div button = new Div();
        ui.add(button);

        assertThrows(IllegalArgumentException.class,
                () -> new ClickTrigger(button)
                        .triggers(new WriteToClipboardAction(
                                ClientValue.itemProperty("email").getInput(),
                                null)));
    }
}
