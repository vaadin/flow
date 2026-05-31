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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceChildrenTemporarilyActionTest {

    @Test
    void replacementsRegisteredAsVirtualChildren() {
        TagComponent target = new TagComponent("vaadin-button");
        TagComponent icon = new TagComponent("vaadin-icon");
        TagComponent text = new TagComponent("span");

        new ReplaceChildrenTemporarilyAction(target, icon, text);

        assertTrue(icon.getElement().isVirtualChild());
        assertTrue(text.getElement().isVirtualChild());
        assertEquals(target.getElement(), icon.getElement().getParent());
        assertEquals(target.getElement(), text.getElement().getParent());
    }

    @Test
    void defaultTimeout_isOneSecond() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("vaadin-button");
        TagComponent target = new TagComponent("vaadin-button");
        TagComponent text = new TagComponent("span");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new ReplaceChildrenTemporarilyAction(target, text));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        // Capture layout from Action.applyTemporarily: $0 stashElement,
        // $1 stashKey, $2 snapshot, $3 apply, $4 revert, $5 timeoutMs.
        assertEquals(1000L, action.getCaptures().get(5));
        assertSame(target.getElement(), action.getCaptures().get(0));
        assertEquals("children", action.getCaptures().get(1));
    }

    @Test
    void customTimeout_isCapturedAsMillis() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("vaadin-button");
        TagComponent target = new TagComponent("vaadin-button");
        TagComponent text = new TagComponent("span");
        ui.getElement().appendChild(button.getElement(), target.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new ReplaceChildrenTemporarilyAction(target,
                        Duration.ofMillis(500), text));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(500L, action.getCaptures().get(5));
    }

    @Test
    void negativeTimeout_rejected() {
        TagComponent target = new TagComponent("vaadin-button");
        TagComponent text = new TagComponent("span");

        assertThrows(IllegalArgumentException.class,
                () -> new ReplaceChildrenTemporarilyAction(target,
                        Duration.ofMillis(-1), text));
    }
}
