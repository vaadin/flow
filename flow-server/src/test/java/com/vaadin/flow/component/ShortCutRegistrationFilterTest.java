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
        assertFalse(result.endsWith("&& "));
        assertFalse(result.startsWith(" &&"));
        assertFalse(result.contains("&& &&"));
        assertFalse(result.contains("&&&&"));
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
        assertFalse(result.endsWith("&& "));
        assertFalse(result.startsWith(" &&"));
        assertFalse(result.contains("&& &&"));
        assertFalse(result.contains("&&&&"));
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
        assertFalse(result.endsWith("&& "));
        assertFalse(result.startsWith(" &&"));
        assertFalse(result.contains("&& &&"));
        assertFalse(result.contains("&&&&"));
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
        assertFalse(result.endsWith("&& "));
        assertFalse(result.startsWith(" &&"));
        assertFalse(result.contains("&& &&"));
        assertFalse(result.contains("&&&&"));
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
        assertFalse(result.endsWith("&& "));
        assertFalse(result.startsWith(" &&"));
        assertFalse(result.contains("&& &&"));
        assertFalse(result.contains("&&&&"));
    }
}
