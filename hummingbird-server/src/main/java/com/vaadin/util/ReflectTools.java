/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.regex.Pattern;

import com.vaadin.shared.util.SharedUtil;

/**
 * An util class with helpers for reflection operations. Used internally by
 * Vaadin and should not be used by application developers. Subject to change at
 * any time.
 *
 * @since 6.2
 */
public class ReflectTools implements Serializable {

    private static final Pattern GETTER_STARTS = Pattern
            .compile("^(get)\\p{Lu}");
    private static final Pattern IS_STARTS = Pattern.compile("^(is)\\p{Lu}");
    private static final Pattern SETTER_STARTS = Pattern.compile("^set\\p{Lu}");
    private static final Pattern SETTER_GETTER_STARTS = Pattern
            .compile("^(set|get|is)");
    static final String CREATE_INSTANCE_FAILED = "Unable to create an instance of {0}. Make sure it has a no-arg constructor";
    static final String CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS = "Unable to create an instance of {0}. Make sure the class is static if it is an inner class.";
    static final String CREATE_INSTANCE_FAILED_ACCESS_EXCEPTION = "Unable to create an instance of {0}. Make sure the class is public and that is has a public no-arg constructor.";
    static final String CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR = "Unable to create an instance of {0}. Make sure the class has a public no-arg constructor.";
    static final String CREATE_INSTANCE_FAILED_CONSTRUCTOR_THREW_EXCEPTION = "Unable to create an instance of {0}. The constructor threw an exception.";

    /**
     * Locates the method in the given class. Returns null if the method is not
     * found. Throws an ExceptionInInitializerError if there is a problem
     * locating the method as this is mainly called from static blocks.
     *
     * @param cls
     *            Class that contains the method
     * @param methodName
     *            The name of the method
     * @param parameterTypes
     *            The parameter types for the method.
     * @return A reference to the method
     * @throws ExceptionInInitializerError
     *             Wraps any exception in an {@link ExceptionInInitializerError}
     *             so this method can be called from a static initializer.
     */
    public static Method findMethod(Class<?> cls, String methodName,
            Class<?>... parameterTypes) throws ExceptionInInitializerError {
        try {
            return cls.getDeclaredMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Returns the value of the java field.
     * <p>
     * Uses getter if present, otherwise tries to access even private fields
     * directly.
     *
     * @param object
     *            The object containing the field
     * @param field
     *            The field we want to get the value for
     * @return The value of the field in the object
     * @throws InvocationTargetException
     *             If the value could not be retrieved
     * @throws IllegalAccessException
     *             If the value could not be retrieved
     * @throws IllegalArgumentException
     *             If the value could not be retrieved
     */
    public static Object getJavaFieldValue(Object object,
            java.lang.reflect.Field field) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        PropertyDescriptor pd;
        try {
            pd = new PropertyDescriptor(field.getName(), object.getClass());
            Method getter = pd.getReadMethod();
            if (getter != null) {
                return getter.invoke(object, (Object[]) null);
            }
        } catch (IntrospectionException e1) {
            // Ignore this and try to get directly using the field
        }

        // Try to get the value or throw an exception
        if (!field.isAccessible()) {
            // Try to gain access even if field is private
            field.setAccessible(true);
        }
        return field.get(object);
    }

    /**
     * Returns the value of the java field that is assignable to the property
     * type.
     * <p>
     * Uses getter if a getter for the correct return type is present, otherwise
     * tries to access even private fields directly. If the java field is not
     * assignable to the property type throws an IllegalArgumentException.
     *
     * @param object
     *            The object containing the field
     * @param field
     *            The field we want to get the value for
     * @param propertyType
     *            The type the field must be assignable to
     * @return The value of the field in the object
     * @throws InvocationTargetException
     *             If the value could not be retrieved
     * @throws IllegalAccessException
     *             If the value could not be retrieved
     * @throws IllegalArgumentException
     *             If the value could not be retrieved
     */
    public static Object getJavaFieldValue(Object object,
            java.lang.reflect.Field field, Class<?> propertyType)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        PropertyDescriptor pd;
        try {
            pd = new PropertyDescriptor(field.getName(), object.getClass());
            if (propertyType.isAssignableFrom(pd.getPropertyType())) {
                Method getter = pd.getReadMethod();
                if (getter != null) {
                    return getter.invoke(object, (Object[]) null);
                }
            }
        } catch (IntrospectionException e1) {
            // Ignore this and try to get directly using the field
        }
        // If the field's type cannot be casted in to the requested type
        if (!propertyType.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException();
        }
        // Try to get the value or throw an exception
        if (!field.isAccessible()) {
            // Try to gain access even if field is private
            field.setAccessible(true);
        }
        return field.get(object);
    }

    /**
     * Sets the value of a java field.
     *
     * @param object
     *            The object containing the field
     * @param field
     *            The field we want to set the value for
     * @param value
     *            The value to set
     * @throws IllegalArgumentException
     *             If the value could not be assigned to the field
     */
    public static void setJavaFieldValue(Object object,
            java.lang.reflect.Field field, Object value)
            throws IllegalArgumentException {
        // Try to set the value directly to the field or throw an exception
        if (!field.isAccessible()) {
            // Try to gain access even if field is private
            field.setAccessible(true);
        }
        try {
            field.set(object, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unable to assign the new value to the field "
                            + field.getName() + " in "
                            + object.getClass().getName()
                            + ". Make sure the field type and value type are compatible.",
                    e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "Unable to assign the new value to the field "
                            + field.getName() + " in "
                            + object.getClass().getName()
                            + ". Make sure the field is not final.",
                    e);
        }
    }

    /**
     * Converts the given primitive type to its boxed version.
     *
     * @param type
     *            the primitive type to convert
     * @return the corresponding boxed type
     * @since 7.4
     */
    public static Class<?> convertPrimitiveType(Class<?> type) {
        // Gets the return type from get method
        if (type.isPrimitive()) {
            if (type.equals(Boolean.TYPE)) {
                type = Boolean.class;
            } else if (type.equals(Integer.TYPE)) {
                type = Integer.class;
            } else if (type.equals(Float.TYPE)) {
                type = Float.class;
            } else if (type.equals(Double.TYPE)) {
                type = Double.class;
            } else if (type.equals(Byte.TYPE)) {
                type = Byte.class;
            } else if (type.equals(Character.TYPE)) {
                type = Character.class;
            } else if (type.equals(Short.TYPE)) {
                type = Short.class;
            } else if (type.equals(Long.TYPE)) {
                type = Long.class;
            }
        }
        return type;
    }

    /**
     * Checks whether the given method is a valid setter according to the
     * JavaBeans Specification.
     *
     * @param method
     *            the method to check
     * @return <code>true</code> if the method is a setter, <code>false</code>
     *         if not
     */
    public static boolean isSetter(Method method) {
        final String methodName = method.getName();
        final Class<?> returnType = method.getReturnType();
        final Type[] argTypes = method.getParameterTypes();

        return returnType == void.class && argTypes.length == 1
                && SETTER_STARTS.matcher(methodName).find();
    }

    /**
     * Checks whether the given method is a valid getter according to JavaBeans
     * Specification.
     *
     * @param method
     *            the method to check
     * @return <code>true</code> if the method is a getter, <code>false</code>
     *         if not
     */
    public static boolean isGetter(Method method) {
        final String methodName = method.getName();
        final Class<?> returnType = method.getReturnType();
        final Type[] argTypes = method.getParameterTypes();

        return returnType != void.class && argTypes.length == 0
                && (GETTER_STARTS.matcher(methodName).find()
                        || (IS_STARTS.matcher(methodName).find()
                                && returnType == boolean.class));
    }

    /**
     * Parses the property name from the given getter or setter method.
     * <p>
     * If the given method does not have a valid setter or getter name, this
     * method may produce unexpected results.
     *
     * @see #isSetter(Method)
     * @see #isGetter(Method)
     * @param method
     *            the method to parse
     * @return the name of the property
     */
    public static String getPropertyName(Method method) {
        String methodName = method.getName();
        assert isGetter(method)
                || isSetter(method) : "Method is not a valid getter or setter";

        String propertyName = SETTER_GETTER_STARTS.matcher(methodName)
                .replaceFirst("");
        return SharedUtil.firstToLower(propertyName);
    }

    /**
     * Creates a instance of the given class with a no-arg constructor.
     * <p>
     * Catches all exceptions which might occur and wraps them in a
     * {@link IllegalArgumentException} with a descriptive error message hinting
     * of what might be wrong with the class that could not be instantiated.
     *
     * @param cls
     *            the class to instantiate
     * @return an instance of the class
     */
    public static <T> T createInstance(Class<T> cls) {
        Constructor<T> constructor;
        try {
            constructor = cls.getConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            if (cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers())) {
                throw new IllegalArgumentException(MessageFormat.format(
                        CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                        cls.getName()), e);
            } else {
                throw new IllegalArgumentException(MessageFormat.format(
                        CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR,
                        cls.getName()), e);
            }
        } catch (InstantiationException e) {
            if (cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers())) {
                throw new IllegalArgumentException(MessageFormat.format(
                        CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                        cls.getName()), e);
            } else {
                throw new IllegalArgumentException(MessageFormat
                        .format(CREATE_INSTANCE_FAILED, cls.getName()), e);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(MessageFormat.format(
                    CREATE_INSTANCE_FAILED_ACCESS_EXCEPTION, cls.getName()), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    MessageFormat.format(CREATE_INSTANCE_FAILED, cls.getName()),
                    e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(MessageFormat.format(
                    CREATE_INSTANCE_FAILED_CONSTRUCTOR_THREW_EXCEPTION,
                    cls.getName()), e);
        }
    }

}
