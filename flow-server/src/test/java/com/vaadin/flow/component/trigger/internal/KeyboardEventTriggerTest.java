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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.installFns;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyboardEventTriggerTest {

    @Test
    void keyAndModifier_renderEventProperties() {
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(input.getElement(), field.getElement());

        new KeyboardEventTrigger(input, "keydown").triggers(
                new SetPropertyAction<>(field, "value",
                        KeyboardEventTrigger.EventData.key),
                new SetPropertyAction<>(field, "disabled",
                        KeyboardEventTrigger.EventData.shiftKey));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<JsFunction> installs = installFns(ui);
        JsFunction source0 = (JsFunction) actionOf(installs.get(0))
                .getCaptures().get(2);
        assertEquals("return event[$0]", source0.getBody());
        assertEquals("key", source0.getCaptures().get(0));

        JsFunction source1 = (JsFunction) actionOf(installs.get(1))
                .getCaptures().get(2);
        assertEquals("return event[$0]", source1.getBody());
        assertEquals("shiftKey", source1.getCaptures().get(0));
    }

    @Test
    void defaultEventName_isKeydown() {
        // The single-arg constructor must default to keydown so the trigger
        // works for shortcuts without the caller specifying an event name.
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(input.getElement(), field.getElement());

        new KeyboardEventTrigger(input).triggers(new SetPropertyAction<>(field,
                "value", KeyboardEventTrigger.EventData.key));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("keydown", singleInstallFn(ui).getCaptures().get(1));
    }

    @Test
    void forKeys_gatesActionInBrowserHandler() {
        // The filter goes into the install JS as an early-return guard, so the
        // action never runs (and the server never hears) for non-matching
        // keys. The list of allowed event.key values is a capture, not
        // concatenated into the body.
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(input.getElement(), field.getElement());

        new KeyboardEventTrigger(input).forKeys(Key.ENTER, Key.ESCAPE)
                .triggers(new SetPropertyAction<>(field, "value",
                        KeyboardEventTrigger.EventData.key));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction install = singleInstallFn(ui);
        // Guard checks event.key OR event.code so the filter works for both
        // event.key-style names (Enter) and event.code-style names (KeyS) in
        // the same list — see KeyboardEventTrigger#appendHandlerBody.
        assertEquals(
                "const h=e=>{if(!$2.includes(e.key)&&!$2.includes(e.code))"
                        + "return;$0(e);};" + "this.addEventListener($1, h);"
                        + "return () => this.removeEventListener($1, h);",
                install.getBody());
        assertEquals("keydown", install.getCaptures().get(1));
        // Key.ESCAPE carries the legacy "Esc" alias too; including every
        // event.key alias in the filter makes the guard match whichever the
        // browser actually reports.
        assertEquals(List.of("Enter", "Escape", "Esc"),
                install.getCaptures().get(2));
    }

    @Test
    void forKeys_combinedWithPreventDefault_runsGuardFirst() {
        // preventDefault must not run for keys the filter rejected — the guard
        // sits before preventDefault in the wrapper body so an unrelated key
        // press is forwarded to the browser unmodified.
        UI ui = new MockUI();
        TagComponent input = new TagComponent("input");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(input.getElement(), field.getElement());

        new KeyboardEventTrigger(input).forKeys(Key.ENTER).preventDefault()
                .triggers(new SetPropertyAction<>(field, "value",
                        KeyboardEventTrigger.EventData.key));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction install = singleInstallFn(ui);
        assertEquals("const h=e=>{if(!$2.includes(e.key)&&"
                + "!$2.includes(e.code))return;" + "e.preventDefault();$0(e);};"
                + "this.addEventListener($1, h);"
                + "return () => this.removeEventListener($1, h);",
                install.getBody());
    }

    @Test
    void keyboardEventData_rejectedInMouseEventTrigger() {
        // KeyboardEventTrigger.EventData.key is bound to KeyboardEventTrigger;
        // wiring it through a ClickTrigger (MouseEventTrigger family) must
        // fail at triggers() time.
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");

        ClickTrigger click = new ClickTrigger(button);
        assertThrows(IllegalArgumentException.class,
                () -> click.triggers(new SetPropertyAction<>(field, "value",
                        KeyboardEventTrigger.EventData.key)));
    }

}
