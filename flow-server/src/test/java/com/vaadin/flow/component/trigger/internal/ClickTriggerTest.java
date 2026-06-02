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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.installFns;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClickTriggerTest {

    @Test
    void screenCoordinates_renderEventProperties() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent xField = new TagComponent("input");
        TagComponent yField = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), xField.getElement(),
                yField.getElement());

        new ClickTrigger(button).triggers(
                new SetPropertyAction<>(xField, "value",
                        ClickTrigger.EventData.screenX),
                new SetPropertyAction<>(yField, "value",
                        ClickTrigger.EventData.screenY));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // HandlerInput renders as a JsFunction taking `event` and capturing
        // the property name; the body itself is a constant.
        List<JsFunction> installs = installFns(ui);
        JsFunction source0 = (JsFunction) actionOf(installs.get(0))
                .getCaptures().get(2);
        assertEquals(List.of("event"), source0.getArgumentNames());
        assertEquals("return event[$0]", source0.getBody());
        assertEquals("screenX", source0.getCaptures().get(0));

        JsFunction source1 = (JsFunction) actionOf(installs.get(1))
                .getCaptures().get(2);
        assertEquals("return event[$0]", source1.getBody());
        assertEquals("screenY", source1.getCaptures().get(0));
    }

    @Test
    void mouseEventData_sharedAcrossInstances_renderedPerHandler() {
        // The same static EventData field is the source for two separate
        // ClickTrigger instances on different hosts — both renders succeed
        // because the input is bound to the trigger class, not an instance.
        UI ui = new MockUI();
        TagComponent button1 = new TagComponent("button");
        TagComponent button2 = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button1.getElement(), button2.getElement(),
                field.getElement());

        Action.Input<Integer> sharedX = ClickTrigger.EventData.screenX;
        new ClickTrigger(button1)
                .triggers(new SetPropertyAction<>(field, "value", sharedX));
        new ClickTrigger(button2)
                .triggers(new SetPropertyAction<>(field, "value", sharedX));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(2, pending.size());
    }

    @Test
    void mouseEventData_acceptedAcrossMultipleTriggersCalls() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent xField = new TagComponent("input");
        TagComponent yField = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), xField.getElement(),
                yField.getElement());

        ClickTrigger click = new ClickTrigger(button);
        // Same static EventData field used across two separate triggers() calls
        // on the same trigger instance.
        Action.Input<Integer> x = ClickTrigger.EventData.screenX;
        click.triggers(new SetPropertyAction<>(xField, "value", x));
        click.triggers(new SetPropertyAction<>(yField, "value", x));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(2, pending.size());
    }

    @Test
    void mouseEventData_rejectedInNonMouseEventTrigger() {
        // ClickTrigger.EventData.screenX is bound to MouseEventTrigger; a plain
        // DomEventTrigger is not a MouseEventTrigger so wiring it through such
        // a handler must fail at triggers() time.
        TagComponent input = new TagComponent("input");
        TagComponent field = new TagComponent("input");

        DomEventTrigger keypress = new DomEventTrigger(input, "keypress");
        assertThrows(IllegalArgumentException.class,
                () -> keypress.triggers(new SetPropertyAction<>(field, "value",
                        ClickTrigger.EventData.screenX)));
    }

}
