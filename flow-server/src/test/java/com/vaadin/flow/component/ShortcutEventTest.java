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

    @Test
    public void matches() {
        assertFalse("Null key should return false",
                eventNoModifiers.matches(null));
        assertFalse("Extra modifier should return false",
                eventNoModifiers.matches(Key.KEY_F, KeyModifier.ALT));
        assertFalse("Missing modifier should return false",
                eventOneModifier.matches(Key.KEY_F));

        assertTrue("Matching key should return true",
                eventNoModifiers.matches(Key.KEY_F));
        assertTrue("Matching key and modifier should return true",
                eventOneModifier.matches(Key.KEY_F, KeyModifier.ALT));
        assertTrue("Matching key and two modifiers should return true",
                eventTwoModifiers.matches(Key.KEY_F, KeyModifier.ALT,
                        KeyModifier.CONTROL));
    }

    private static ShortcutEvent event(Key key, KeyModifier... modifiers) {
        Component component = mock(Component.class);
        return new ShortcutEvent(component, component, key,
                new HashSet<>(Arrays.asList(modifiers)));
    }
}
