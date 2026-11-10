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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShortCutRegistrationFilterTest {

    /**
     * Invokes the private getModifierNames method of ShortcutRegistration,
     * which lists the modifiers the shortcut filter requires to be pressed or
     * released.
     */
    private static String invokeGetModifierNames(boolean pressed,
            KeyModifier... modifiers) throws ReflectiveOperationException {
        Method privateMethod = ShortcutRegistration.class.getDeclaredMethod(
                "getModifierNames", Collection.class, boolean.class);
        privateMethod.setAccessible(true);
        List<Key> keys = Arrays.stream(modifiers).map(modifier -> {
            try {
                return getHashableKey(modifier);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }).toList();
        return (String) privateMethod.invoke(null, keys, pressed);
    }

    /**
     * Wraps the modifier like ShortcutRegistration stores it, in its private
     * HashableKey class.
     */
    private static Key getHashableKey(KeyModifier keyModifier)
            throws ReflectiveOperationException {
        final Class<?> nestedClass = Class.forName(
                "com.vaadin.flow.component.ShortcutRegistration$HashableKey");
        final Constructor<?> ctor = nestedClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return (Key) ctor.newInstance(keyModifier);
    }

    @Test
    void testGenerateEventModifierFilterWithModifierKeyAlt()
            throws ReflectiveOperationException {
        assertEquals("Alt", invokeGetModifierNames(true, KeyModifier.ALT));
        // AltGraph is neither required nor forbidden, as browsers differ
        // in reporting it for the Option key
        assertEquals("Shift Control Meta",
                invokeGetModifierNames(false, KeyModifier.ALT));
    }

    @Test
    void testGenerateEventModifierFilterWithModifierKeyAltGr()
            throws ReflectiveOperationException {
        assertEquals("AltGraph",
                invokeGetModifierNames(true, KeyModifier.ALT_GRAPH));
        assertEquals("Shift Control Alt Meta",
                invokeGetModifierNames(false, KeyModifier.ALT_GRAPH));
    }

    @Test
    void testGenerateEventModifierFilterWithModifierKeyAltAndAltGr()
            throws ReflectiveOperationException {
        assertEquals("Alt AltGraph", invokeGetModifierNames(true,
                KeyModifier.ALT, KeyModifier.ALT_GRAPH));
        assertEquals("Shift Control Meta", invokeGetModifierNames(false,
                KeyModifier.ALT, KeyModifier.ALT_GRAPH));
    }

    @Test
    void testGenerateEventModifierFilterWithModifierKeyAltGrAndCtrl()
            throws ReflectiveOperationException {
        assertEquals("Control AltGraph", invokeGetModifierNames(true,
                KeyModifier.ALT_GRAPH, KeyModifier.CONTROL));
        assertEquals("Shift Alt Meta", invokeGetModifierNames(false,
                KeyModifier.ALT_GRAPH, KeyModifier.CONTROL));
    }

    @Test
    void testGenerateEventModifierFilterWithModifierKeyAltAndCtrl()
            throws ReflectiveOperationException {
        assertEquals("Control Alt", invokeGetModifierNames(true,
                KeyModifier.ALT, KeyModifier.CONTROL));
        assertEquals("Shift Meta", invokeGetModifierNames(false,
                KeyModifier.ALT, KeyModifier.CONTROL));
    }
}
