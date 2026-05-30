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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CallMethodActionTest {

    @Test
    void noArguments_rendersTargetDotMethodCall() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent input = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), input.getElement());

        new ClickTrigger(button).triggers(new CallMethodAction(input, "focus"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // $0 = target element, $1 = method name string capture. No arguments
        // means the call is just `$0[$1]()` — no $2, no event references.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0[$1]()", action.getBody());
        assertSame(input.getElement(), action.getCaptures().get(0));
        assertEquals("focus", action.getCaptures().get(1));
        assertEquals(2, action.getCaptures().size());
    }

    @Test
    void multipleArguments_eachInputRenderedAsCallArgument() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        // Mix a literal and a handler-scoped input to confirm both kinds of
        // inputs are rendered uniformly into the call argument list.
        new ClickTrigger(button)
                .triggers(new CallMethodAction(panel, "scrollTo",
                        new LiteralInput<>(0), ClickTrigger.EventData.clientY));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0[$1]($2(event), $3(event))", action.getBody());
        assertSame(panel.getElement(), action.getCaptures().get(0));
        assertEquals("scrollTo", action.getCaptures().get(1));
        // $2 / $3 are the inputs' own JsFunctions — the action wires them in,
        // it does not flatten their captures into its own.
        assertEquals(4, action.getCaptures().size());
    }
}
