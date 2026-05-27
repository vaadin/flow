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

class DoubleClickTriggerTest {

    @Test
    void usesDblclickAndSharesMouseEventOutput() {
        UI ui = new MockUI();
        TagComponent panel = new TagComponent("div");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(panel.getElement(), field.getElement());

        new DoubleClickTrigger(panel).triggers(new SetPropertyAction<>(field,
                "value", DoubleClickTrigger.Output.clientX));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Install body is the generic DomEventTrigger install; the event name
        // is a capture at $1, distinguishing dblclick from click only via that
        // capture.
        JsFunction install = singleInstallFn(ui);
        assertEquals(
                "this.addEventListener($1, $0);"
                        + "return () => this.removeEventListener($1, $0);",
                install.getBody());
        assertEquals("dblclick", install.getCaptures().get(1));

        // The Output field is inherited from MouseEventTrigger.Output and
        // resolves to the same event[clientX] expression as on ClickTrigger.
        JsFunction source = (JsFunction) actionOf(install).getCaptures().get(2);
        assertEquals("clientX", source.getCaptures().get(0));
    }
}
