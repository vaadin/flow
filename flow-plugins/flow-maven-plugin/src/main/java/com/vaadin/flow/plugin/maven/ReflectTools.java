package com.vaadin.flow.plugin.maven;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Unified util class with helpers for reflection operations.
 */
public final class ReflectTools {
    public static Method findMethodAndMakeAccessible(Class<?> clazz, final String methodName)
            throws NoSuchMethodException {
        while (clazz != null && clazz != Object.class) {
            try {
                final Method method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (final NoSuchMethodException e) {
                // ignore
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }

    public static Field findField(Class<?> clazz, final String fieldName) throws NoSuchFieldException {
        while (clazz != null && !clazz.equals(Object.class)) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    public static void setJavaFieldValue(
            final Object object,
            final String fieldName,
            final Object value)
            throws NoSuchFieldException {
        setJavaFieldValue(object, findField(object.getClass(), fieldName), value);
    }

    public static void setJavaFieldValue(
            final Object object,
            final Field field,
            final Object value) {
        com.vaadin.flow.internal.ReflectTools.setJavaFieldValue(object, field, value);
    }

    public static Object getJavaFieldValue(final Object object, final String field)
            throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        return com.vaadin.flow.internal.ReflectTools.getJavaFieldValue(object, findField(object.getClass(), field));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getJavaFieldValue(
            final Object object,
            final String field,
            final Class<T> propertyType)
            throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        return (T) com.vaadin.flow.internal.ReflectTools.getJavaFieldValue(
                object,
                findField(object.getClass(), field),
                propertyType);
    }

    private ReflectTools() {
    }
}
