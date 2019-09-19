/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.shared.util.SharedUtil;

/**
 * An util class with helpers for reflection operations. Used internally by
 * Vaadin and should not be used by application developers. Subject to change at
 * any time.
 *
 * @since 1.0
 */
public class ReflectTools implements Serializable {

    private static final Pattern GETTER_STARTS = Pattern
            .compile("^(get)\\p{Lu}");
    private static final Pattern IS_STARTS = Pattern.compile("^(is)\\p{Lu}");
    private static final Pattern SETTER_STARTS = Pattern.compile("^set\\p{Lu}");
    private static final Pattern SETTER_GETTER_STARTS = Pattern
            .compile("^(set|get|is)");
    static final String CREATE_INSTANCE_FAILED = "Unable to create an instance of '%s'. Make sure it has a no-arg constructor";
    static final String CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS = "Cannot instantiate '%s'. Make sure the class is static if it is an inner class.";
    static final String CREATE_INSTANCE_FAILED_ACCESS_EXCEPTION = "Unable to create an instance of '%s'. Make sure the class is public and that is has a public no-arg constructor.";
    static final String CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR = "Unable to create an instance of '%s'. Make sure the class has a public no-arg constructor.";
    static final String CREATE_INSTANCE_FAILED_LOCAL_CLASS = "Cannot instantiate local class '%s'. Move class declaration outside the method.";
    static final String CREATE_INSTANCE_FAILED_CONSTRUCTOR_THREW_EXCEPTION = "Unable to create an instance of '%s'. The constructor threw an exception.";

    private static final Predicate<Method> IS_SYNTHETIC = Method::isSynthetic;

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
     * Gets default value for given {@code primitiveType}.
     *
     * @param primitiveType
     *            the primitive type
     * @return the corresponding default value
     */
    public static Serializable getPrimitiveDefaultValue(
            Class<?> primitiveType) {
        if (primitiveType.equals(int.class)) {
            return 0;
        } else if (primitiveType.equals(double.class)) {
            return 0D;
        } else if (primitiveType.equals(boolean.class)) {
            return false;
        } else if (primitiveType.equals(float.class)) {
            return 0F;
        } else if (primitiveType.equals(byte.class)) {
            return (byte) 0;
        } else if (primitiveType.equals(char.class)) {
            return (char) 0;
        } else if (primitiveType.equals(short.class)) {
            return (short) 0;
        } else if (primitiveType.equals(long.class)) {
            return 0L;
        } else if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException(
                    "Provided type " + primitiveType + " is not primitive");
        }
        throw new IllegalStateException(
                "Unexpected primitive type: " + primitiveType);
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
                && isSetterName(methodName);
    }

    /**
     * Checks whether the given method name is a valid setter name according to
     * the JavaBeans Specification.
     *
     * @param methodName
     *            the method name to check
     * @return <code>true</code> if the method name is a setter name,
     *         <code>false</code> if not
     */
    public static boolean isSetterName(String methodName) {
        return SETTER_STARTS.matcher(methodName).find();
    }

    /**
     * Checks whether the given method name is a valid getter name according to
     * the JavaBeans Specification.
     *
     * @param methodName
     *            the method name to check
     * @param isBoolean
     *            whether the method is getter for boolean type
     * @return <code>true</code> if the method name is a getter name,
     *         <code>false</code> if not
     */
    public static boolean isGetterName(String methodName, boolean isBoolean) {
        return GETTER_STARTS.matcher(methodName).find()
                || (IS_STARTS.matcher(methodName).find() && isBoolean);
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
                && isGetterName(methodName, returnType == boolean.class);
    }

    /**
     * Return all the getter methods from the given type.
     * <p>
     * Any getter methods from {@link Object} are excluded.
     *
     * @param type
     *            the type to get getters from
     * @return a stream of getter methods
     */
    public static Stream<Method> getGetterMethods(Class<?> type) {
        return Stream.of(type.getMethods()).filter(IS_SYNTHETIC.negate())
                .filter(ReflectTools::isGetter)
                .filter(ReflectTools::isNotObjectMethod);
    }

    /**
     * Return all the setter methods from the given type.
     *
     * @param type
     *            the type to get setters from
     * @return a stream of setter methods
     */
    public static Stream<Method> getSetterMethods(Class<?> type) {
        return Stream.of(type.getMethods()).filter(IS_SYNTHETIC.negate())
                .filter(ReflectTools::isSetter);
    }

    /**
     * Returns whether the given method is <b>NOT</b> declared in {@link Object}
     * .
     *
     * @param method
     *            the method to check
     * @return <code>true</code> if method is NOT declared in Object,
     *         <code>false</code> if it is
     */
    public static boolean isNotObjectMethod(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass != Object.class;
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
                || isSetter(method) : "Method is not a valid getter or setter: "
                        + methodName;

        String propertyName = SETTER_GETTER_STARTS.matcher(methodName)
                .replaceFirst("");
        return SharedUtil.firstToLower(propertyName);
    }

    /**
     * Returns property type from the given getter or setter method.
     *
     * @see #isSetter(Method)
     * @see #isGetter(Method)
     * @param method
     *            the method to inspect
     * @return the property type
     */
    public static Type getPropertyType(Method method) {
        if (!isGetter(method) && !isSetter(method)) {
            throw new IllegalArgumentException(
                    "Method is not a valid getter or setter: " + method);
        }
        if (isGetter(method)) {
            return method.getGenericReturnType();
        } else {
            return method.getGenericParameterTypes()[0];
        }
    }

    /**
     * Creates an instance of the given class with a no-arg constructor.
     * <p>
     * Catches all exceptions which might occur and wraps them in a
     * {@link IllegalArgumentException} with a descriptive error message hinting
     * of what might be wrong with the class that could not be instantiated.
     *
     * @param <T>
     *            the instance type
     * @param cls
     *            the class to instantiate
     * @return an instance of the class
     */
    public static <T> T createInstance(Class<T> cls) {
        return createProxyInstance(cls, cls);
    }

    /**
     * Creates an instance of the given {@code proxyClass} with no-arg
     * constructor.
     * <p>
     * Catches all exceptions which might occur and wraps them in a
     * {@link IllegalArgumentException} with a descriptive error message hinting
     * of what might be wrong with the class that could not be instantiated.
     * Descriptive message is derived based on the information about the
     * {@code originalClass}.
     *
     * @param proxyClass
     *            the proxy class to instantiate
     * @param originalClass
     *            the class that is used to determine exception description, if
     *            creation fails
     * @param <T>
     *            type of a proxy class
     * @return instance of a proxyClass
     * @throws IllegalArgumentException
     *             if class instance creation fails
     */
    public static <T> T createProxyInstance(Class<T> proxyClass,
            Class<?> originalClass) {
        checkClassAccessibility(originalClass);
        try {
            Optional<Constructor<?>> constructor = Stream
                    .of(proxyClass.getConstructors())
                    .filter(ctor -> ctor.getParameterCount() == 0).findFirst();
            if (constructor.isPresent()) {
                return proxyClass.cast(constructor.get().newInstance());
            }

            constructor = Stream.of(proxyClass.getConstructors())
                    .filter(ctor -> ctor.getParameterCount() == 1)
                    .filter(ctor -> ctor.isVarArgs()).findFirst();
            if (constructor.isPresent()) {
                Class<?> paramType = constructor.get().getParameterTypes()[0];

                return proxyClass.cast(constructor.get().newInstance(
                        Array.newInstance(paramType.getComponentType(), 0)));
            }
        } catch (InstantiationException e) {
            if (originalClass.isMemberClass()
                    && !Modifier.isStatic(originalClass.getModifiers())) {
                throw new IllegalArgumentException(String.format(
                        CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                        originalClass.getName()), e);
            } else {
                throw new IllegalArgumentException(String.format(
                        CREATE_INSTANCE_FAILED, originalClass.getName()), e);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                    String.format(CREATE_INSTANCE_FAILED_ACCESS_EXCEPTION,
                            originalClass.getName()),
                    e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(
                    CREATE_INSTANCE_FAILED, originalClass.getName()), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(
                    CREATE_INSTANCE_FAILED_CONSTRUCTOR_THREW_EXCEPTION,
                    originalClass.getName()), e);
        }

        throw new IllegalArgumentException(String.format(
                CREATE_INSTANCE_FAILED_NO_PUBLIC_NOARG_CONSTRUCTOR,
                originalClass.getName()));
    }

    /**
     * Makes a check whether the {@code clazz} is externally accessible for
     * instantiation (e.g. it's not inner class (nested and not static) and is
     * not a local class).
     *
     * @param clazz
     *            type to check
     */
    public static void checkClassAccessibility(Class<?> clazz) {
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new IllegalArgumentException(String.format(
                    CREATE_INSTANCE_FAILED_FOR_NON_STATIC_MEMBER_CLASS,
                    clazz.getName()));
        } else if (clazz.isLocalClass()) {
            throw new IllegalArgumentException(String.format(
                    CREATE_INSTANCE_FAILED_LOCAL_CLASS, clazz.getName()));
        }
    }

    /**
     * Creates a parameterized type, e.g. {@code List<Bean>}.
     *
     * @param rawType
     *            the raw type, e.g. {@code List}
     * @param subType
     *            the sub type, e.g. {@code Bean}
     * @return a parameterized type
     */
    public static Type createParameterizedType(Class<?> rawType, Type subType) {
        return new ParameterizedType() {

            @Override
            public Type getRawType() {
                return rawType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { subType };
            }
        };
    }

    /**
     * Finds the Class type for the generic interface class extended by given
     * class if exists.
     *
     * @param clazz
     *            class that should extend interface
     * @param interfaceType
     *            class type of interface to get generic for
     * @return Class if found else {@code null}
     */
    public static Class<?> getGenericInterfaceType(Class<?> clazz,
            Class<?> interfaceType) {
        Type type = GenericTypeReflector.getTypeParameter(clazz,
                interfaceType.getTypeParameters()[0]);

        if (type instanceof Class || type instanceof ParameterizedType) {
            return GenericTypeReflector.erase(type);
        }
        return null;
    }

    /**
     * Finds a getter for a property in a bean type.
     *
     * @param beanClass
     *            the bean type, not <code>null</code>
     * @param propertyName
     *            the property name, not <code>null</code>
     * @return a getter method, or an empty optional if the bean type has no
     *         readable property with the provided name
     */
    public static Optional<Method> getGetter(Class<? extends Object> beanClass,
            String propertyName) {
        /*
         * Iterating all methods is no worse than what Class.getMethod
         * internally does, but in this way we don't have to deal with any
         * exceptions.
         */
        return getGetterMethods(beanClass)
                .filter(method -> propertyName.equals(getPropertyName(method)))
                .findFirst();
    }

    /**
     * Checks if the given exception class represents a checked exception.
     *
     * @param exceptionClass
     *            the class to check
     * @return <code>true</code> if the class represents a checked exception,
     *         false otherwise
     */
    public static boolean isCheckedException(Class<?> exceptionClass) {
        return !RuntimeException.class.isAssignableFrom(exceptionClass)
                && !Error.class.isAssignableFrom(exceptionClass);
    }

    /**
     * Get the functional interface method name defined for given interface
     * class.
     *
     * @param functionalClass
     *            interface class to get the functional method for
     *
     * @return functional interface method
     */
    public static Method getFunctionalMethod(Class<?> functionalClass) {
        assert functionalClass.getAnnotation(FunctionalInterface.class) != null;
        Method[] methods = functionalClass.getMethods();
        if (methods.length == 1) {
            return methods[0];
        }
        List<Method> filteredMethods = Stream.of(methods)
                .filter(method -> !Modifier.isStatic(method.getModifiers())
                        && !method.isDefault())
                .collect(Collectors.toList());
        assert filteredMethods.size() == 1;
        return filteredMethods.get(0);
    }

    /**
     * Collect all the integer values for public static final constants found
     * for the given class.
     *
     * @param clazz
     *            class to collect constants from
     * @return list of all integer constants in class
     */
    public static List<Integer> getConstantIntValues(Class<?> clazz) {
        List<Integer> integerConstants = new ArrayList<>();

        for (Field field : getConstants(clazz)) {
            if (field.getType().equals(int.class)) {
                try {
                    integerConstants.add(field.getInt(null));
                } catch (IllegalAccessException e) {
                    // Ignore this exception. Public fields should always be
                    // accessible.
                    String msg = String.format(
                            "Received access exception for public field '%s' in class '%s'",
                            field.getName(), clazz.getSimpleName());
                    assert false : msg;
                    LoggerFactory.getLogger(ReflectTools.class.getName())
                            .warn(msg, e);
                }
            }
        }

        return integerConstants;
    }

    /**
     * Finds the most specific class that both provided classes extend from.
     *
     * @param a
     *            one class to get the base type for, not <code>null</code>
     * @param b
     *            another class to get the base type for, not <code>null</code>
     * @return the most specific base class, not <code>null</code>
     * @throws IllegalArgumentException
     *             if a or b are interfaces or primitive types
     *
     */
    public static Class<?> findCommonBaseType(Class<?> a, Class<?> b) {
        if (a.isInterface()) {
            throw new IllegalArgumentException("a cannot be an interface");
        }
        if (b.isInterface()) {
            throw new IllegalArgumentException("b cannot be an interface");
        }
        if (a.isPrimitive()) {
            throw new IllegalArgumentException("a cannot be a primitive type");
        }
        if (b.isPrimitive()) {
            throw new IllegalArgumentException("b cannot be a primitive type");
        }

        if (a.isAssignableFrom(b)) {
            return a;
        } else if (b.isAssignableFrom(a)) {
            return b;
        }

        Class<?> currentClass = a;
        while (!currentClass.isAssignableFrom(b)) {
            currentClass = currentClass.getSuperclass();
        }

        return currentClass;
    }

    /**
     * Finds the common ancestor of the two {@code ClassLoaders}. The class
     * loaders themselves are acceptable ancestors; If they are equal, {@code
     * classLoaderA} is returned.
     * An empty optional is returned if the two class loaders aren't equal,
     * no shared ancestor is found, or the implementation of the class loader
     * treats bootstrap class loader as {@code null} when it is the parent of
     * a class loaders (see {@link ClassLoader#getParent()}.
     *
     * @param classLoaderA
     *            class loader A
     * @param classLoaderB
     *            class loader B
     * @return a common ancestor both class loaders share, or an empty optional
     *         if there is no shared ancestor. Or if the implementation treats
     *         bootstrap loaders as {@code null} (as per
     *         {@link ClassLoader#getParent()}).
     */
    public static Optional<ClassLoader> findClosestCommonClassLoaderAncestor(
            ClassLoader classLoaderA, ClassLoader classLoaderB) {
        if (classLoaderA == null || classLoaderB == null) {
            return Optional.empty();
        }
        HashSet<ClassLoader> parents = new HashSet<>();
        ClassLoader parentA = classLoaderA;
        ClassLoader parentB = classLoaderB;

        while (parentA != null || parentB != null) {
            if (parentA != null) {
                if (parents.contains(parentA)) {
                    return Optional.of(parentA);
                }
                parents.add(parentA);
                parentA = parentA.getParent();
            }
            if (parentB != null) {
                if (parents.contains(parentB)) {
                    return Optional.of(parentB);
                }
                parents.add(parentB);
                parentB = parentB.getParent();
            }
        }
        return Optional.empty();
    }

    private static List<Field> getConstants(Class<?> staticFields) {
        List<Field> staticFinalFields = new ArrayList<>();
        Field[] declaredFields = staticFields.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())) {
                staticFinalFields.add(field);
            }
        }
        return staticFinalFields;
    }
}
