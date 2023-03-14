package com.vaadin.flow.component;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ShortcutEventTest {

    private ShortcutEvent eventNoModifiers = event(Key.KEY_F);
    private ShortcutEvent eventOneModifier = event(Key.KEY_F, KeyModifier.ALT);
    private ShortcutEvent eventTwoModifiers = event(Key.KEY_F, KeyModifier.ALT,
            KeyModifier.CONTROL);

    private ShortcutEvent eventWithAltAndAltGr = event(Key.KEY_F,
            KeyModifier.ALT, KeyModifier.ALT_GRAPH);

    @Test
    public void matches() {
        assertFalse("Null key should return false",
                eventNoModifiers.matches(null));
        assertFalse("Extra modifier should return false",
                eventNoModifiers.matches(Key.KEY_F, KeyModifier.ALT));
        assertFalse("Missing modifier should return false",
                eventOneModifier.matches(Key.KEY_F));
        assertFalse(
                "Matching key and two modifiers (Alt, Alt_Gr) plus an extra one (Control), "
                        + "should return false",
                eventWithAltAndAltGr.matches(Key.KEY_F, KeyModifier.ALT,
                        KeyModifier.ALT_GRAPH, KeyModifier.CONTROL));

        assertTrue("Matching key should return true",
                eventNoModifiers.matches(Key.KEY_F));
        assertTrue("Matching key and modifier should return true",
                eventOneModifier.matches(Key.KEY_F, KeyModifier.ALT));
        assertTrue("Matching key and two modifiers should return true",
                eventTwoModifiers.matches(Key.KEY_F, KeyModifier.ALT,
                        KeyModifier.CONTROL));
        assertTrue(
                "Matching key and two modifiers (Alt_Gr, Alt) should return true",
                eventWithAltAndAltGr.matches(Key.KEY_F, KeyModifier.ALT_GRAPH,
                        KeyModifier.ALT));
    }

    private static ShortcutEvent event(Key key, KeyModifier... modifiers) {
        Component component = mock(Component.class);
        return new ShortcutEvent(component, component, key,
                new HashSet<>(Arrays.asList(modifiers)));
    }
}
