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
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomEventTriggerTest {

    @Test
    void setProperty_emitsAddEventListenerOnAttach() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetPropertyAction<>(field, "value", ""));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Install JS references action at $0 and event name at $1 — no user
        // content leaks into the body, both are captures.
        JsFunction install = singleInstallFn(ui);
        assertEquals(
                "this.addEventListener($1, $0);"
                        + "return () => this.removeEventListener($1, $0);",
                install.getBody());
        assertEquals("click", install.getCaptures().get(1));

        // The install $0 is the action's JsFunction directly — no intermediate
        // composed handler layer. The action is also the DOM event listener.
        JsFunction action = actionOf(install);
        assertEquals(List.of("event"), action.getArgumentNames());

        // SetPropertyAction body shape; target captured as $0, property name
        // string capture at $1, source JsFunction invoked as $2(event).
        assertEquals("$0[$1] = $2(event)", action.getBody());
        assertSame(field.getElement(), action.getCaptures().get(0));
        assertEquals("value", action.getCaptures().get(1));
    }

}
