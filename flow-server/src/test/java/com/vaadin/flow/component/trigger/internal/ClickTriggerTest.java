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

        ClickTrigger click = new ClickTrigger(button);
        click.triggers(
                new SetPropertyAction<>(xField, "value", click.screenX()),
                new SetPropertyAction<>(yField, "value", click.screenY()));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // HandlerInput renders as a JsFunction taking `event` and capturing
        // the property name; the body itself is a constant.
        List<JsFunction> installs = installFns(ui);
        JsFunction source0 = (JsFunction) actionOf(installs.get(0))
                .getCaptures().get(2);
        assertEquals(List.of("event"), source0.getArgumentNames());
        assertEquals("let propertyName=$0;return event[propertyName]",
                source0.getBody());
        assertEquals("screenX", source0.getCaptures().get(0));

        JsFunction source1 = (JsFunction) actionOf(installs.get(1))
                .getCaptures().get(2);
        assertEquals("let propertyName=$0;return event[propertyName]",
                source1.getBody());
        assertEquals("screenY", source1.getCaptures().get(0));
    }

    @Test
    void argumentFromOtherTrigger_isRejectedAtBuildTime() {
        TagComponent button1 = new TagComponent("button");
        TagComponent button2 = new TagComponent("button");
        TagComponent field = new TagComponent("input");

        ClickTrigger click1 = new ClickTrigger(button1);
        ClickTrigger click2 = new ClickTrigger(button2);

        // Input made by click1 cannot be used in click2's handler.
        assertThrows(IllegalArgumentException.class, () -> click2.triggers(
                new SetPropertyAction<>(field, "value", click1.screenX())));
    }

    @Test
    void argumentUsedInOwningTrigger_acceptedAcrossMultipleTriggersCalls() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent xField = new TagComponent("input");
        TagComponent yField = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), xField.getElement(),
                yField.getElement());

        ClickTrigger click = new ClickTrigger(button);
        // Same Input instance used across two separate triggers() calls on
        // its owning trigger.
        Action.Input<Integer> x = click.screenX();
        click.triggers(new SetPropertyAction<>(xField, "value", x));
        click.triggers(new SetPropertyAction<>(yField, "value", x));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pending = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(2, pending.size());
    }

}
