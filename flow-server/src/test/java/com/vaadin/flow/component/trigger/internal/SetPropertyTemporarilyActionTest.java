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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetPropertyTemporarilyActionTest {

    @Test
    void defaultTimeout_isOneSecond() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetPropertyTemporarilyAction<>(target, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        // $3 is the timeout in ms; default is 1000.
        assertEquals(1000L, action.getCaptures().get(3));
    }

    @Test
    void customTimeout_isCapturedAsMillis() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyTemporarilyAction<>(target, "value",
                        "x", Duration.ofMillis(500)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        // Capture order: $0 target element, $1 property name, $2 source
        // JsFunction, $3 timeout ms.
        assertSame(target.getElement(), action.getCaptures().get(0));
        assertEquals("value", action.getCaptures().get(1));
        assertEquals(500L, action.getCaptures().get(3));
    }

    @Test
    void zeroTimeout_isAllowed() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyTemporarilyAction<>(target, "value",
                        "x", Duration.ZERO));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(0L, action.getCaptures().get(3));
    }

    @Test
    void negativeTimeout_rejected() {
        TagComponent target = new TagComponent("input");

        assertThrows(IllegalArgumentException.class,
                () -> new SetPropertyTemporarilyAction<>(target, "value", "x",
                        Duration.ofMillis(-1)));
    }
}
