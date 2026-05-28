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

        // Install JS exposes the action and event name as named parameters;
        // captures still carry the values, no user content leaks into the
        // body.
        JsFunction install = singleInstallFn(ui);
        assertEquals("let eventName=$1;let action=$0;"
                + "this.addEventListener(eventName, action);"
                + "return () => this.removeEventListener(eventName, action);",
                install.getBody());
        assertEquals("click", install.getCaptures().get(1));

        // The first install capture is the action's JsFunction directly — no
        // intermediate composed handler layer. The action is also the DOM
        // event listener.
        JsFunction action = actionOf(install);
        assertEquals(List.of("event"), action.getArgumentNames());

        // SetPropertyAction body shape; target, property name, and source
        // input are all exposed by name via withParameter — captures stay
        // (target, propertyName, source).
        assertEquals(
                "let source=$2;let propertyName=$1;let target=$0;"
                        + "target[propertyName] = source(event)",
                action.getBody());
        assertSame(field.getElement(), action.getCaptures().get(0));
        assertEquals("value", action.getCaptures().get(1));
    }

}
