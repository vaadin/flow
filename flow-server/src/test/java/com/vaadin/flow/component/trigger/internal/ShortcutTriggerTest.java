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
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShortcutTriggerTest {

    @Test
    void ctrlS_emitsExactModifierGuardKeyFilterAndPreventDefault() {
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(host.getElement(), target.getElement());

        new ShortcutTrigger(host, Key.KEY_S, KeyModifier.CONTROL)
                .triggers(new SetPropertyAction<>(target, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Modifier guard first (exact match: Ctrl pressed, Shift/Alt/Meta
        // NOT pressed), then the key filter against event.key or event.code,
        // then preventDefault + stopPropagation, then the action.
        JsFunction install = singleInstallFn(ui);
        assertEquals("const h=e=>{"
                + "if(!e.ctrlKey||e.shiftKey||e.altKey||e.metaKey)return;"
                + "if(!$2.includes(e.key)&&!$2.includes(e.code))return;"
                + "e.preventDefault();e.stopPropagation();$0(e);};"
                + "this.addEventListener($1, h);"
                + "return () => this.removeEventListener($1, h);",
                install.getBody());
        assertEquals("keydown", install.getCaptures().get(1));
        // Key.KEY_S is an event.code key; the filter list includes "KeyS"
        // and the guard's event.code branch matches a real S press.
        assertEquals(List.of("KeyS"), install.getCaptures().get(2));
    }

    @Test
    void ctrlShiftS_requiresBothModifiersPressed() {
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(host.getElement(), target.getElement());

        new ShortcutTrigger(host, Key.KEY_S, KeyModifier.CONTROL,
                KeyModifier.SHIFT)
                .triggers(new SetPropertyAction<>(target, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction install = singleInstallFn(ui);
        // Ctrl AND Shift required (both negated checks), Alt and Meta still
        // forbidden — exact match keeps Ctrl+S free for a different binding.
        assertEquals("const h=e=>{"
                + "if(!e.ctrlKey||!e.shiftKey||e.altKey||e.metaKey)return;"
                + "if(!$2.includes(e.key)&&!$2.includes(e.code))return;"
                + "e.preventDefault();e.stopPropagation();$0(e);};"
                + "this.addEventListener($1, h);"
                + "return () => this.removeEventListener($1, h);",
                install.getBody());
    }

    @Test
    void noModifier_requiresAllModifierFlagsFalse() {
        // Plain-key shortcut: every modifier must NOT be pressed, so the
        // shortcut only fires on a bare Escape — not Ctrl+Escape, Shift+
        // Escape, etc.
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        TagComponent target = new TagComponent("input");
        ui.getElement().appendChild(host.getElement(), target.getElement());

        new ShortcutTrigger(host, Key.ESCAPE)
                .triggers(new SetPropertyAction<>(target, "disabled", true));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction install = singleInstallFn(ui);
        assertEquals("const h=e=>{"
                + "if(e.ctrlKey||e.shiftKey||e.altKey||e.metaKey)return;"
                + "if(!$2.includes(e.key)&&!$2.includes(e.code))return;"
                + "e.preventDefault();e.stopPropagation();$0(e);};"
                + "this.addEventListener($1, h);"
                + "return () => this.removeEventListener($1, h);",
                install.getBody());
    }

    @Test
    void altGraphModifier_rejectedAtConstruction() {
        TagComponent host = new TagComponent("div");
        assertThrows(IllegalArgumentException.class,
                () -> new ShortcutTrigger(host, Key.KEY_S,
                        KeyModifier.ALT_GRAPH));
    }

}
