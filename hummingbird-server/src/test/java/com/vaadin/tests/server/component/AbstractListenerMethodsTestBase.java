package com.vaadin.tests.server.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Assert;

import com.vaadin.event.EventSource;
import com.vaadin.tests.VaadinClasses;
import com.vaadin.ui.Component;

import junit.framework.TestCase;

public abstract class AbstractListenerMethodsTestBase extends TestCase {

    public static void main(String[] args) {
        findAllListenerMethods();
    }

    private static void findAllListenerMethods() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (Class<?> c : VaadinClasses.getAllServerSideClasses()) {
            while (c != null && c.getName().startsWith("com.vaadin.")) {
                classes.add(c);
                c = c.getSuperclass();
            }
        }

        for (Class<?> c : classes) {
            boolean found = false;
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals("addListener")) {
                    if (m.getParameterTypes().length != 1) {
                        continue;
                    }
                    String packageName = "com.vaadin.tests.server";
                    if (Component.class.isAssignableFrom(c)) {
                        packageName += ".component."
                                + c.getSimpleName().toLowerCase();
                        continue;
                    }

                    if (!found) {
                        found = true;
                        System.out.println("package " + packageName + ";");

                        System.out.println("import "
                                + AbstractListenerMethodsTestBase.class
                                        .getName()
                                + ";");
                        System.out.println("import " + c.getName() + ";");
                        System.out
                                .println(
                                        "public class " + c.getSimpleName()
                                                + "Listeners extends "
                                                + AbstractListenerMethodsTestBase.class
                                                        .getSimpleName()
                                                + " {");
                    }

                    String listenerClassName = m.getParameterTypes()[0]
                            .getSimpleName();
                    String eventClassName = listenerClassName
                            .replaceFirst("Listener$", "Event");
                    System.out.println("public void test" + listenerClassName
                            + "() throws Exception {");
                    System.out.println("    testListener(" + c.getSimpleName()
                            + ".class, " + eventClassName + ".class, "
                            + listenerClassName + ".class);");
                    System.out.println("}");
                }
            }
            if (found) {
                System.out.println("}");
                System.out.println();
            }
        }
    }

    protected void testListenerAddGetRemove(
            Class<? extends EventSource> testClass,
            Class<? extends EventObject> eventClass,
            Class<? extends EventListener> listenerClass) throws Exception {
        // Create a component for testing
        EventSource c = testClass.newInstance();
        testListenerAddGetRemove(testClass, eventClass, listenerClass, c);

    }

    protected void testListenerAddGetRemove(Class<? extends EventSource> cls,
            Class<? extends EventObject> eventClass,
            Class<? extends EventListener> listenerClass, EventSource c)
                    throws Exception {

        Object mockListener1 = EasyMock.createMock(listenerClass);
        Object mockListener2 = EasyMock.createMock(listenerClass);

        // Verify we start from no listeners
        verifyListeners(c, listenerClass);

        // Add one listener and verify
        addListener(c, mockListener1, listenerClass);
        verifyListeners(c, listenerClass, mockListener1);

        // Add another listener and verify
        addListener(c, mockListener2, listenerClass);
        verifyListeners(c, listenerClass, mockListener1, mockListener2);

        // Remove the first and verify
        removeListener(c, mockListener1, listenerClass);
        verifyListeners(c, listenerClass, mockListener2);

        // Remove the remaining and verify
        removeListener(c, mockListener2, listenerClass);
        verifyListeners(c, listenerClass);

    }

    @Deprecated
    protected void testNonEventSourceListenerAddGetRemove(Class<?> testClass,
            Class<?> eventClass, Class<? extends EventListener> listenerClass)
                    throws Exception {
        // Create a component for testing
        Object c = testClass.newInstance();
        testNonEventSourceListenerAddGetRemove(testClass, eventClass,
                listenerClass, c);

    }

    @Deprecated
    protected void testNonEventSourceListenerAddGetRemove(Class<?> cls,
            Class<?> eventClass, Class<? extends EventListener> listenerClass,
            Object c) throws Exception {

        Object mockListener1 = EasyMock.createMock(listenerClass);
        Object mockListener2 = EasyMock.createMock(listenerClass);

        // Verify we start from no listeners
        verifyNonEventSourceListeners(c, listenerClass);

        // Add one listener and verify
        addListener(c, mockListener1, listenerClass);
        verifyNonEventSourceListeners(c, listenerClass, mockListener1);

        // Add another listener and verify
        addListener(c, mockListener2, listenerClass);
        verifyNonEventSourceListeners(c, listenerClass, mockListener1,
                mockListener2);

        // Remove the first and verify
        removeListener(c, mockListener1, listenerClass);
        verifyNonEventSourceListeners(c, listenerClass, mockListener2);

        // Remove the remaining and verify
        removeListener(c, mockListener2, listenerClass);
        verifyNonEventSourceListeners(c, listenerClass);

    }

    private void removeListener(Object c, Object listener,
            Class<?> listenerClass) throws IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException,
                    SecurityException, NoSuchMethodException {
        Method method = getRemoveListenerMethod(c.getClass(), listenerClass);
        method.invoke(c, listener);

    }

    private void addListener(Object c, Object listener1, Class<?> listenerClass)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, SecurityException,
            NoSuchMethodException {
        Method method = getAddListenerMethod(c.getClass(), listenerClass);
        method.invoke(c, listener1);
    }

    private Collection<?> getListeners(Object c,
            Class<? extends EventListener> listenerType)
                    throws IllegalArgumentException, IllegalAccessException,
                    InvocationTargetException, SecurityException,
                    NoSuchMethodException {
        Method method = getGetListenersMethod(c.getClass());
        return (Collection<?>) method.invoke(c, listenerType);
    }

    private Method getGetListenersMethod(Class<? extends Object> cls)
            throws SecurityException, NoSuchMethodException {
        return cls.getMethod("getListeners", Class.class);
    }

    private Method getAddListenerMethod(Class<?> cls, Class<?> listenerClass)
            throws SecurityException, NoSuchMethodException {
        String methodName = "add" + listenerClass.getSimpleName();
        return cls.getMethod(methodName, listenerClass);
    }

    private Method getRemoveListenerMethod(Class<?> cls, Class<?> listenerClass)
            throws SecurityException, NoSuchMethodException {
        String methodName = "remove" + listenerClass.getSimpleName();

        return cls.getMethod(methodName, listenerClass);
    }

    private void verifyListeners(EventSource c,
            Class<? extends EventListener> listenerType,
            Object... expectedListeners) throws IllegalArgumentException,
                    SecurityException, IllegalAccessException,
                    InvocationTargetException, NoSuchMethodException {
        Collection<? extends EventListener> registeredListeners = c
                .getListeners(listenerType);
        assertEquals("Number of listeners", expectedListeners.length,
                registeredListeners.size());

        Assert.assertArrayEquals(expectedListeners,
                registeredListeners.toArray());
    }

    private void verifyNonEventSourceListeners(Object c,
            Class<? extends EventListener> listenerClass,
            Object... expectedListeners) throws IllegalArgumentException,
                    SecurityException, IllegalAccessException,
                    InvocationTargetException, NoSuchMethodException {
        Collection<?> registeredListeners = getListeners(c, listenerClass);
        assertEquals("Number of listeners", expectedListeners.length,
                registeredListeners.size());

        Assert.assertArrayEquals(expectedListeners,
                registeredListeners.toArray());
    }

}
