package com.vaadin.flow.component;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShortCutRegistrationFilterTest {

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

    private static Object getHashableKey(KeyModifier keyModifier)
            throws ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        final Class<?> nestedClass = Class.forName(
                "com.vaadin.flow.component.ShortcutRegistration$HashableKey");
        final Constructor<?> ctor = nestedClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        final Object instance = ctor.newInstance(keyModifier);
        return instance;
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAlt()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(Collections
                .singletonList(((Key) getHashableKey(KeyModifier.ALT))));
        assertTrue(result.contains("&& event.getModifierState('Alt')"));
        assertTrue(result.contains("&& !event.getModifierState('Control')"));
        assertFalse(result.contains("AltGraph"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltGr()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(Collections
                .singletonList(((Key) getHashableKey(KeyModifier.ALT_GRAPH))));
        assertTrue(result.contains("&& event.getModifierState('AltGraph')"));
        assertTrue(result.contains("&& !event.getModifierState('Alt')"));
        assertTrue(result.contains("&& !event.getModifierState('Control')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltAndAltGr()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(
                Arrays.asList((Key) getHashableKey(KeyModifier.ALT),
                        (Key) getHashableKey(KeyModifier.ALT_GRAPH)));
        assertTrue(result.contains("&& event.getModifierState('Alt')"));
        assertTrue(result.contains("&& event.getModifierState('AltGraph')"));
        assertTrue(result.contains("&& !event.getModifierState('Control')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltGrAndCtrl()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(
                Arrays.asList((Key) getHashableKey(KeyModifier.ALT_GRAPH),
                        (Key) getHashableKey(KeyModifier.CONTROL)));
        assertTrue(result.contains("&& event.getModifierState('AltGraph')"));
        assertTrue(result.contains("&& event.getModifierState('Control')"));
        assertTrue(result.contains("&& !event.getModifierState('Alt')"));
    }

    @Test
    public void testGenerateEventModifierFilterWithModifierKeyAltAndCtrl()
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        String result = invokeGenerateEventModifierFilter(
                Arrays.asList((Key) getHashableKey(KeyModifier.ALT),
                        (Key) getHashableKey(KeyModifier.CONTROL)));
        assertTrue(result.contains("&& event.getModifierState('Alt')"));
        assertTrue(result.contains("&& event.getModifierState('Control')"));
        assertFalse(result.contains("'AltGraph'"));
    }
}
