package com.vaadin.tests.server.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;

import org.easymock.EasyMock;
import org.junit.Assert;

import com.vaadin.event.ElementEvents;
import com.vaadin.event.ElementEvents.DOMEventForwarder;
import com.vaadin.event.EventSource;
import com.vaadin.event.HasEventRouter;
import com.vaadin.hummingbird.kernel.DomEventListener;
import com.vaadin.ui.Component;

import junit.framework.TestCase;

public abstract class AbstractListenerMethodsTestBase extends TestCase {

    protected void testDOMListenerAddGetRemove(
            Class<? extends Component> testClass,
            Class<? extends EventObject> eventClass,
            Class<? extends EventListener> listenerClass) throws Exception {
        // Create a component for testing
        Component c = testClass.newInstance();
        testDOMListenerAddGetRemove(testClass, eventClass, listenerClass, c);

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
        verifyListeners(c, eventClass);

        // Add one listener and verify
        addListener(c, mockListener1, listenerClass);
        verifyListeners(c, eventClass, mockListener1);

        // Add another listener and verify
        addListener(c, mockListener2, listenerClass);
        verifyListeners(c, eventClass, mockListener1, mockListener2);

        // Remove the first and verify
        removeListener(c, mockListener1, listenerClass);
        verifyListeners(c, eventClass, mockListener2);

        // Remove the remaining and verify
        removeListener(c, mockListener2, listenerClass);
        verifyListeners(c, eventClass);

    }

    protected void testDOMListenerAddGetRemove(Class<? extends Component> cls,
            Class<? extends EventObject> eventClass,
            Class<? extends EventListener> listenerClass, Component c)
                    throws Exception {

        EventListener mockListener1 = EasyMock.createMock(listenerClass);
        EventListener mockListener2 = EasyMock.createMock(listenerClass);
        DOMEventForwarder[] noDOMForwarders = new DOMEventForwarder[] {};
        DOMEventForwarder[] oneDOMForwarder = new DOMEventForwarder[] {
                new DOMEventForwarder(eventClass, c.getElement(),
                        (HasEventRouter) c) };

        // Verify we start from no listeners
        verifyDOMListeners(c, eventClass, noDOMForwarders);

        // Add one listener and verify
        addListener(c, mockListener1, listenerClass);

        verifyDOMListeners(c, eventClass, oneDOMForwarder, mockListener1);

        // Add another listener and verify
        addListener(c, mockListener2, listenerClass);
        verifyDOMListeners(c, eventClass, oneDOMForwarder, mockListener1,
                mockListener2);

        // Remove the first and verify
        removeListener(c, mockListener1, listenerClass);
        verifyDOMListeners(c, eventClass, oneDOMForwarder, mockListener2);

        // Remove the remaining and verify
        removeListener(c, mockListener2, listenerClass);
        verifyDOMListeners(c, eventClass, noDOMForwarders);

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
        verifyNonEventSourceListeners(c, eventClass);

        // Add one listener and verify
        addListener(c, mockListener1, listenerClass);
        verifyNonEventSourceListeners(c, eventClass, mockListener1);

        // Add another listener and verify
        addListener(c, mockListener2, listenerClass);
        verifyNonEventSourceListeners(c, eventClass, mockListener1,
                mockListener2);

        // Remove the first and verify
        removeListener(c, mockListener1, listenerClass);
        verifyNonEventSourceListeners(c, eventClass, mockListener2);

        // Remove the remaining and verify
        removeListener(c, mockListener2, listenerClass);
        verifyNonEventSourceListeners(c, eventClass);

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

    private Collection<?> getListeners(Object c, Class<?> eventType)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, SecurityException,
            NoSuchMethodException {
        Method method = getGetListenersMethod(c.getClass());
        return (Collection<?>) method.invoke(c, eventType);
    }

    private Method getGetListenersMethod(Class<? extends Object> cls)
            throws SecurityException, NoSuchMethodException {
        return cls.getMethod("getListeners", Class.class);
    }

    private Method getAddListenerMethod(Class<?> cls, Class<?> listenerClass)
            throws SecurityException, NoSuchMethodException {
        String methodName = "add" + listenerClass.getSimpleName();
        try {
            return cls.getMethod(methodName, listenerClass);
        } catch (NoSuchMethodException e) {
            return cls.getMethod(methodName,
                    com.vaadin.event.EventListener.class);
        }

    }

    private Method getRemoveListenerMethod(Class<?> cls, Class<?> listenerClass)
            throws SecurityException, NoSuchMethodException {
        String methodName = "remove" + listenerClass.getSimpleName();
        try {
            return cls.getMethod(methodName, listenerClass);
        } catch (NoSuchMethodException e) {
            return cls.getMethod(methodName,
                    com.vaadin.event.EventListener.class);
        }
    }

    private void verifyListeners(EventSource c,
            Class<? extends EventObject> eventType, Object... expectedListeners)
                    throws IllegalArgumentException, SecurityException,
                    IllegalAccessException, InvocationTargetException,
                    NoSuchMethodException {
        Collection<? extends EventListener> registeredListeners = c
                .getListeners(eventType);
        assertEquals("Number of listeners", expectedListeners.length,
                registeredListeners.size());

        Assert.assertArrayEquals(expectedListeners,
                registeredListeners.toArray());
    }

    private <E extends EventObject> void verifyDOMListeners(Component c,
            Class<E> eventType, DOMEventForwarder<E>[] expectedForwarders,
            EventListener... expectedListeners) throws IllegalArgumentException,
                    SecurityException, IllegalAccessException,
                    InvocationTargetException, NoSuchMethodException {
        Collection<?> registeredListeners = getListeners(c, eventType);
        assertEquals("Number of listeners", expectedListeners.length,
                registeredListeners.size());

        Collection<DomEventListener> registeredDomListeners = c.getElement()
                .getEventListeners(ElementEvents.getDomEventType(eventType));
        Assert.assertArrayEquals(expectedForwarders,
                registeredDomListeners.toArray());
    }

    private void verifyNonEventSourceListeners(Object c, Class<?> eventType,
            Object... expectedListeners) throws IllegalArgumentException,
                    SecurityException, IllegalAccessException,
                    InvocationTargetException, NoSuchMethodException {
        Collection<?> registeredListeners = getListeners(c, eventType);
        assertEquals("Number of listeners", expectedListeners.length,
                registeredListeners.size());

        Assert.assertArrayEquals(expectedListeners,
                registeredListeners.toArray());
    }

}
