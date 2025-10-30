/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShortCutRegistrationFilterTest {

    /**
     * This method is used to test the generateEventModifierFilter method in the
     * ShortcutRegistration class. This method is private, so we need to use
     * reflection to invoke it.
     *
     * @param list
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private static String invokeGenerateEventModifierFilter(List<Key> list)
            throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method privateMethod = ShortcutRegistration.class.getDeclaredMethod(
                "generateEventModifierFilter", Collection.class);
        privateMethod.setAccessible(true);
        // invoke the private method for testing
        String result = (String) privateMethod.invoke(null, list);
        return result;
    }

    /**
     * This method is used to get the HashableKey object from the
     * ShortcutRegistration class. This is needed because the HashableKey class
     * is private, and we need to create an instance of it to test the
     * generateEventModifierFilter method.
     *
     * @param keyModifier
     * @return
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private static Object getHashableKey(KeyModifier keyModifier)
            throws ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        final Class<?> nestedClass = Class.forName(
                "com.vaadin.flow.component.ShortcutRegistration$HashableKey");
        final Constructor<?> ctor = nestedClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(keyModifier);
    }

    private static ArrayList<String> getModifierFilterConditions(
            String eventModifierFilter, int expectedFilterCount) {
        ArrayList<String> filterConditions = new ArrayList<>(Arrays
                .asList(eventModifierFilter.split(" && ", Integer.MAX_VALUE)));
        assertTrue(
                "Split filter conditions should not contain '&' character or blank strings.",
                filterConditions.stream()
                        .filter(c -> c.contains("&") || StringUtils.isBlank(c))
                        .collect(Collectors.toList()).isEmpty());
        assertEquals(expectedFilterCount, filterConditions.size());
        return filterConditions;
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAlt()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(Collections
                .singletonList(((Key) getHashableKey(KeyModifier.ALT))));
        ArrayList<String> conditions = getModifierFilterConditions(result, 4);
        assertTrue(conditions.contains("event.getModifierState('Alt')"));
        assertTrue(conditions.contains("!event.getModifierState('Control')"));
        assertFalse(conditions.contains("event.getModifierState('AltGraph')"));
        assertFalse(conditions.contains("!event.getModifierState('AltGraph')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltGr()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(Collections
                .singletonList(((Key) getHashableKey(KeyModifier.ALT_GRAPH))));
        ArrayList<String> conditions = getModifierFilterConditions(result, 5);
        assertTrue(conditions.contains("event.getModifierState('AltGraph')"));
        assertTrue(conditions.contains("!event.getModifierState('Alt')"));
        assertTrue(conditions.contains("!event.getModifierState('Control')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltAndAltGr()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(
                Arrays.asList((Key) getHashableKey(KeyModifier.ALT),
                        (Key) getHashableKey(KeyModifier.ALT_GRAPH)));
        ArrayList<String> conditions = getModifierFilterConditions(result, 5);
        assertTrue(conditions.contains("event.getModifierState('Alt')"));
        assertTrue(conditions.contains("event.getModifierState('AltGraph')"));
        assertTrue(conditions.contains("!event.getModifierState('Control')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltGrAndCtrl()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(
                Arrays.asList((Key) getHashableKey(KeyModifier.ALT_GRAPH),
                        (Key) getHashableKey(KeyModifier.CONTROL)));
        ArrayList<String> conditions = getModifierFilterConditions(result, 5);
        assertTrue(conditions.contains("event.getModifierState('AltGraph')"));
        assertTrue(conditions.contains("event.getModifierState('Control')"));
        assertTrue(conditions.contains("!event.getModifierState('Alt')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltAndCtrl()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(
                Arrays.asList((Key) getHashableKey(KeyModifier.ALT),
                        (Key) getHashableKey(KeyModifier.CONTROL)));
        ArrayList<String> conditions = getModifierFilterConditions(result, 4);
        assertTrue(conditions.contains("event.getModifierState('Alt')"));
        assertTrue(conditions.contains("event.getModifierState('Control')"));
        assertFalse(conditions.contains("event.getModifierState('AltGraph')"));
        assertFalse(conditions.contains("!event.getModifierState('AltGraph')"));
    }
}
