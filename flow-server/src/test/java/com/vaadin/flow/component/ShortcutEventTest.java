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
package com.vaadin.flow.component;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ShortcutEventTest {

    private ShortcutEvent eventNoModifiers = event(Key.KEY_F);
    private ShortcutEvent eventOneModifier = event(Key.KEY_F, KeyModifier.ALT);
    private ShortcutEvent eventTwoModifiers = event(Key.KEY_F, KeyModifier.ALT,
            KeyModifier.CONTROL);

    private ShortcutEvent eventWithAltAndAltGr = event(Key.KEY_F,
            KeyModifier.ALT, KeyModifier.ALT_GRAPH);

    @Test
    public void matches() {
        assertFalse(eventNoModifiers.matches(null),
                "Null key should return false");
        assertFalse(eventNoModifiers.matches(Key.KEY_F, KeyModifier.ALT),
                "Extra modifier should return false");
        assertFalse(eventOneModifier.matches(Key.KEY_F),
                "Missing modifier should return false");
        assertFalse(
                eventWithAltAndAltGr.matches(Key.KEY_F, KeyModifier.ALT,
                        KeyModifier.ALT_GRAPH, KeyModifier.CONTROL),
                "Matching key and two modifiers (Alt, Alt_Gr) plus an extra one (Control), "
                        + "should return false");

        assertTrue(eventNoModifiers.matches(Key.KEY_F),
                "Matching key should return true");
        assertTrue(eventOneModifier.matches(Key.KEY_F, KeyModifier.ALT),
                "Matching key and modifier should return true");
        assertTrue(
                eventTwoModifiers.matches(Key.KEY_F, KeyModifier.ALT,
                        KeyModifier.CONTROL),
                "Matching key and two modifiers should return true");
        assertTrue(
                eventWithAltAndAltGr.matches(Key.KEY_F, KeyModifier.ALT_GRAPH,
                        KeyModifier.ALT),
                "Matching key and two modifiers (Alt_Gr, Alt) should return true");
    }

    private static ShortcutEvent event(Key key, KeyModifier... modifiers) {
        Component component = mock(Component.class);
        return new ShortcutEvent(component, component, key,
                new HashSet<>(Arrays.asList(modifiers)));
    }
}
