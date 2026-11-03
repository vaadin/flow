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
package com.vaadin.flow.internal;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

/**
 * Utility class for Java Beans information access.
 * <p>
 * Properties are found by their accessor methods with plain reflection, as
 * {@code java.beans.Introspector} does, so that Flow does not need the
 * {@code java.desktop} module.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class BeanUtil implements Serializable {
    // Prevent instantiation of util class
    private BeanUtil() {
    }

    /**
     * Returns the properties of a class, a record, or an interface.
     * <p>
     * The properties of a record are its components, in their order. The
     * properties of a class or an interface are found by their public getters
     * and setters, including the inherited ones and the default methods of
     * interfaces, as {@code java.beans.Introspector} finds them: a getter is a
     * method that {@link ReflectTools#isGetter(Method)} accepts, and a setter
     * is a method that {@link ReflectTools#isSetter(Method)} accepts and that
     * takes the type of the getter.
     * <p>
     * The properties are in the order of their fields in the source code, the
     * fields of a superclass first, as {@link Class#getDeclaredFields()}
     * returns them in practice, followed by the properties without a field of
     * the same name, sorted by name. The order of methods is not kept by the
     * JVM, so a property without a field has no order of its own.
     *
     * @param beanType
     *            the type whose properties to query
     * @return a list of the properties of the given type
     */
    public static List<BeanProperty> getBeanProperties(
            final Class<?> beanType) {
        List<BeanProperty> properties = new ArrayList<>();
        if (beanType == null) {
            return properties;
        }
        if (beanType.isRecord()) {
            for (RecordComponent component : beanType.getRecordComponents()) {
                properties.add(new BeanProperty(component.getName(),
                        component.getAccessor(), null));
            }
            return properties;
        }

        Map<String, Method> readMethods = new HashMap<>();
        Map<String, List<Method>> writeMethods = new HashMap<>();
        for (Method method : beanType.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String readName = getReadPropertyName(method);
            if (readName != null) {
                readMethods.merge(readName, method, BeanUtil::preferReadMethod);
            }
            String writeName = getWritePropertyName(method);
            if (writeName != null) {
                writeMethods.computeIfAbsent(writeName, k -> new ArrayList<>())
                        .add(method);
            }
        }

        TreeSet<String> withoutField = new TreeSet<>(readMethods.keySet());
        withoutField.addAll(writeMethods.keySet());
        List<String> names = new ArrayList<>();
        for (String field : getFieldNames(beanType)) {
            if (withoutField.remove(field)) {
                names.add(field);
            }
        }
        names.addAll(withoutField);
        for (String name : names) {
            Method readMethod = readMethods.get(name);
            Method writeMethod = findWriteMethod(writeMethods.get(name),
                    readMethod);
            if (readMethod != null || writeMethod != null) {
                properties.add(new BeanProperty(name, readMethod, writeMethod));
            }
        }
        return properties;
    }

    /**
     * Returns the property with the given name of a class, a record, or an
     * interface. The property name may refer to a nested property, e.g.
     * "property.subProperty" or "property.subProperty1.subProperty2". The
     * property must have a public read method (or a chain of read methods in
     * case of a nested property), which is not declared by {@link Object}.
     *
     * @param beanType
     *            the type declaring the property
     * @param propertyName
     *            the name of the property
     * @return the property, or {@code null} if there is no such property
     */
    public static BeanProperty getBeanProperty(Class<?> beanType,
            String propertyName) {
        if (propertyName.contains(".")) {
            String[] parts = propertyName.split("\\.", 2);
            // Get the type of the field in the bean class
            BeanProperty parent = getBeanProperty(beanType, parts[0]);
            // Find the rest from the sub type
            return getBeanProperty(parent == null ? null : parent.type(),
                    parts[1]);
        }
        for (BeanProperty property : getBeanProperties(beanType)) {
            Method readMethod = property.readMethod();
            if (property.name().equals(propertyName) && readMethod != null
                    && readMethod.getDeclaringClass() != Object.class) {
                return property;
            }
        }
        return null;
    }

    /**
     * Returns the property descriptors of a class, a record, or an interface.
     *
     * @param beanType
     *            the type whose properties to query
     * @return a list of property descriptors of the given type
     * @throws IntrospectionException
     *             if a descriptor cannot be created
     * @deprecated Use {@link #getBeanProperties(Class)}, which needs no
     *             {@code java.desktop} module
     */
    @Deprecated(since = "25.4", forRemoval = true)
    public static List<PropertyDescriptor> getBeanPropertyDescriptors(
            final Class<?> beanType) throws IntrospectionException {
        List<PropertyDescriptor> descriptors = new ArrayList<>();
        for (BeanProperty property : getBeanProperties(beanType)) {
            descriptors.add(toPropertyDescriptor(property));
        }
        return descriptors;
    }

    /**
     * Returns the type of the property with the given name and declaring class.
     * The property name may refer to a nested property, e.g.
     * "property.subProperty" or "property.subProperty1.subProperty2". The
     * property must have a public read method (or a chain of read methods in
     * case of a nested property).
     *
     * @param beanType
     *            the type declaring the property
     * @param propertyName
     *            the name of the property
     * @return the property type, or {@code null} if there is no such property
     */
    public static Class<?> getPropertyType(Class<?> beanType,
            String propertyName) {
        BeanProperty property = getBeanProperty(beanType, propertyName);
        return property == null ? null : property.type();
    }

    /**
     * Returns the property descriptor for the property of the given name and
     * declaring class. The property name may refer to a nested property, e.g.
     * "property.subProperty" or "property.subProperty1.subProperty2". The
     * property must have a public read method (or a chain of read methods in
     * case of a nested property).
     *
     * @param beanType
     *            the type declaring the property
     * @param propertyName
     *            the name of the property
     * @return the corresponding descriptor
     * @throws IntrospectionException
     *             if a descriptor cannot be created
     * @deprecated Use {@link #getBeanProperty(Class, String)}, which needs no
     *             {@code java.desktop} module
     */
    @Deprecated(since = "25.4", forRemoval = true)
    public static PropertyDescriptor getPropertyDescriptor(Class<?> beanType,
            String propertyName) throws IntrospectionException {
        BeanProperty property = getBeanProperty(beanType, propertyName);
        return property == null ? null : toPropertyDescriptor(property);
    }

    // No catch clause for IntrospectionException in this class: the JVM loads
    // the type of a catch clause when it verifies the class, so this class
    // could not be used without the java.desktop module
    private static PropertyDescriptor toPropertyDescriptor(
            BeanProperty property) throws IntrospectionException {
        return new PropertyDescriptor(property.name(), property.readMethod(),
                property.writeMethod());
    }

    /**
     * Gets the names of the instance fields of a class and its superclasses,
     * the fields of a superclass first.
     */
    private static Set<String> getFieldNames(Class<?> type) {
        Set<String> names = new LinkedHashSet<>();
        if (type.getSuperclass() != null) {
            names.addAll(getFieldNames(type.getSuperclass()));
        }
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                names.add(field.getName());
            }
        }
        return names;
    }

    private static String getReadPropertyName(Method method) {
        if (!ReflectTools.isGetter(method)) {
            return null;
        }
        String name = method.getName();
        return decapitalize(name.substring(name.startsWith("is") ? 2 : 3));
    }

    private static String getWritePropertyName(Method method) {
        if (!ReflectTools.isSetter(method)) {
            return null;
        }
        return decapitalize(method.getName().substring(3));
    }

    /**
     * Chooses between two getters of the same property: {@code isX} over
     * {@code getX}, as {@code Introspector} does, and the one with the most
     * specific return type, which is not the bridge method of a covariant
     * return type.
     */
    private static Method preferReadMethod(Method method1, Method method2) {
        boolean is1 = method1.getName().startsWith("is");
        boolean is2 = method2.getName().startsWith("is");
        if (is1 != is2) {
            return is1 ? method1 : method2;
        }
        Class<?> type1 = method1.getReturnType();
        Class<?> type2 = method2.getReturnType();
        if (type1 != type2) {
            return type2.isAssignableFrom(type1) ? method1 : method2;
        }
        return method2.isBridge() ? method1 : method2;
    }

    /**
     * Chooses the setter of a property from its candidates: the one that takes
     * the type of the getter, if there is a getter.
     */
    private static Method findWriteMethod(List<Method> candidates,
            Method readMethod) {
        if (candidates == null) {
            return null;
        }
        return candidates.stream().filter(method -> readMethod == null
                || method.getParameterTypes()[0] == readMethod.getReturnType())
                .min(Comparator.comparing(Method::isBridge).thenComparing(
                        method -> method.getParameterTypes()[0].getName()))
                .orElse(null);
    }

    /**
     * Converts the part of an accessor name after its prefix to a property
     * name, as {@code Introspector.decapitalize} does: "FooBar" becomes
     * "fooBar", "X" becomes "x", but "URL" stays "URL".
     */
    private static String decapitalize(String name) {
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
                && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Collects all property paths for a given class, including nested
     * properties. This is useful for generating field paths like "event.button"
     * or "data.user.name" for a nested bean structure.
     * <p>
     * The method recursively traverses bean/record properties and returns dot-
     * separated paths for all simple types (primitives, wrappers, String).
     * Complex types like collections, arrays, and maps are returned as-is
     * without recursion into their elements.
     *
     * @param type
     *            the class to introspect
     * @return a list of property paths (e.g., ["event.button", "event.clientX",
     *         "type"])
     * @since 25.0
     */
    public static List<String> getBeanPropertyPaths(Class<?> type) {
        List<String> paths = new ArrayList<>();
        collectPropertyPaths("", type, paths);
        return paths;
    }

    /**
     * Helper method to recursively collect property paths.
     *
     * @param prefix
     *            the current path prefix (e.g., "event" or "event.detail")
     * @param type
     *            the class to inspect
     * @param paths
     *            the list to accumulate paths into
     */
    private static void collectPropertyPaths(String prefix, Class<?> type,
            List<String> paths) {
        for (BeanProperty property : getBeanProperties(type)) {
            // Skip "class" property
            if ("class".equals(property.name())) {
                continue;
            }

            String propertyName = property.name();
            String fieldPath = prefix.isEmpty() ? propertyName
                    : prefix + "." + propertyName;
            Class<?> propertyType = property.type();

            if (isSimpleType(propertyType)) {
                // Leaf node - add path
                paths.add(fieldPath);
            } else if (propertyType.isArray()
                    || java.util.Collection.class.isAssignableFrom(propertyType)
                    || java.util.Map.class.isAssignableFrom(propertyType)) {
                // Collection/array/map - add as-is, don't recurse
                paths.add(fieldPath);
            } else if (!getBeanProperties(propertyType).isEmpty()) {
                // Complex bean/record type with properties, recurse
                collectPropertyPaths(fieldPath, propertyType, paths);
            } else {
                // No properties, add as-is
                paths.add(fieldPath);
            }
        }
    }

    /**
     * Checks if a class is a simple type (primitive, wrapper, or String).
     *
     * @param type
     *            the class to check
     * @return true if the type is simple
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type == String.class
                || type == Boolean.class || type == Byte.class
                || type == Character.class || type == Short.class
                || type == Integer.class || type == Long.class
                || type == Float.class || type == Double.class
                || Number.class.isAssignableFrom(type);
    }

    /**
     * Returns whether an implementation of JSR-303 version 1.0 or 1.1 is
     * present on the classpath. If this method returns false, trying to create
     * a {@code BeanValidator} instance will throw an
     * {@code IllegalStateException}. If an implementation is not found, logs a
     * level {@code FINE} message the first time it is run.
     *
     * @return {@code true} if bean validation is available, {@code false}
     *         otherwise.
     */
    public static boolean checkBeanValidationAvailable() {
        return LazyValidationAvailability.BEAN_VALIDATION_AVAILABLE;
    }

    private static class LazyValidationAvailability implements Serializable {
        private static final boolean BEAN_VALIDATION_AVAILABLE = isAvailable();

        private LazyValidationAvailability() {
        }

        private static boolean isAvailable() {
            try {
                Class<?> clazz = Class.forName("jakarta.validation.Validation");
                Method method = clazz.getMethod("buildDefaultValidatorFactory");
                method.invoke(null);
                return true;
            } catch (ClassNotFoundException | NoSuchMethodException
                    | InvocationTargetException e) {
                LoggerFactory.getLogger("com.vaadin.validator.BeanValidator")
                        .info("A JSR-303 bean validation implementation not found on the classpath or could not be initialized. BeanValidator cannot be used.",
                                e);
                return false;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException(
                        "Unable to invoke jakarta.validation.Validation.buildDefaultValidatorFactory()",
                        e);
            }
        }
    }
}
