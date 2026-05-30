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

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceTriggerTest {

    @Test
    void install_emitsPositionStateAndMatchOrRestartBranches() {
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(host.getElement(), target.getElement());

        new SequenceTrigger(host, Key.KEY_H, Key.KEY_I)
                .triggers(new SetPropertyAction<>(target, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Install JS declares `let i=0;` outside the wrapper so the counter
        // survives across keydowns. On each event, the body either advances
        // i (and returns without firing) or resets — only the final advance
        // reaches the trailing $0(e) that DomEventTrigger#install appends.
        JsFunction install = singleInstallFn(ui);
        assertEquals(
                "let i=0;const h=e=>{"
                        + "const o=s=>s.includes(e.key)||s.includes(e.code);"
                        + "if(!o($2[i])){i=o($2[0])?1:0;return;}"
                        + "if(++i!==$2.length)return;" + "i=0;$0(e);};"
                        + "this.addEventListener($1, h);"
                        + "return () => this.removeEventListener($1, h);",
                install.getBody());
        assertEquals("keydown", install.getCaptures().get(1));
        // Each slot lists the strings the browser might report. Key.KEY_H
        // and Key.KEY_I are event.code-named keys (matched via the e.code
        // branch).
        assertEquals(List.of(List.of("KeyH"), List.of("KeyI")),
                install.getCaptures().get(2));
    }

    @Test
    void emptySequence_rejected() {
        TagComponent host = new TagComponent("div");
        assertThrows(IllegalArgumentException.class,
                () -> new SequenceTrigger(host));
    }

    @Test
    void sequenceCapturesAllNamesPerSlot() {
        // A Key with multiple printable representations (e.g. Key.SHIFT's
        // "Shift" + "ShiftLeft" + "ShiftRight") preserves all of them in the
        // matched-strings list for that slot, so the client matches whichever
        // value the browser actually reports.
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(host.getElement(), target.getElement());

        new SequenceTrigger(host, Key.ESCAPE, Key.SHIFT)
                .triggers(new SetPropertyAction<>(target, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals(
                List.of(List.of("Escape", "Esc"),
                        List.of("Shift", "ShiftLeft", "ShiftRight")),
                singleInstallFn(ui).getCaptures().get(2));
    }

}
