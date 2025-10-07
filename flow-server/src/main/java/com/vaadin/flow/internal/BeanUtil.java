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
package com.vaadin.flow.internal;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

/**
 * Utility class for Java Beans information access.
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
     * Returns the property descriptors of a class, a record, or an interface.
     *
     * For an interface, superinterfaces are also iterated as Introspector does
     * not take them into account (Oracle Java bug 4275879), but in that case,
     * both the setter and the getter for a property must be in the same
     * interface and should not be overridden in subinterfaces for the discovery
     * to work correctly.
     * <p>
     * NOTE : This utility method relies on introspection (and returns
     * PropertyDescriptor) which is a part of java.beans package. The latter
     * package could require bigger JDK in the future (with Java 9+). So it may
     * be changed in the future.
     * <p>
     * For interfaces, the iteration is depth first and the properties of
     * superinterfaces are returned before those of their subinterfaces.
     *
     * @param beanType
     *            the type whose properties to query
     * @return a list of property descriptors of the given type
     * @throws IntrospectionException
     *             if the introspection fails
     */
    public static List<PropertyDescriptor> getBeanPropertyDescriptors(
            final Class<?> beanType) throws IntrospectionException {
        List<PropertyDescriptor> descriptorsWithDuplicates = internalGetBeanPropertyDescriptors(
                beanType);

        // As we scan for default methods, we might get duplicates
        // for the same property, so we need to remove them.
        // We prefer to keep a class property over an interface
        // property.
        LinkedHashMap<String, PropertyDescriptor> descriptors = new LinkedHashMap<>();
        for (PropertyDescriptor descriptor : descriptorsWithDuplicates) {
            String name = descriptor.getName();
            if (descriptors.containsKey(name)) {
                PropertyDescriptor existing = descriptors.get(name);
                // If the existing descriptor is from a class, keep it
                // otherwise replace it with the new one.
                if (existing.getReadMethod() != null && !existing
                        .getReadMethod().getDeclaringClass().isInterface()) {
                    continue;
                }
            }
            descriptors.put(name, descriptor);
        }

        return new ArrayList<>(descriptors.values());
    }

    private static List<PropertyDescriptor> internalGetBeanPropertyDescriptors(
            Class<?> beanType) throws IntrospectionException {

        if (beanType.isRecord()) {
            List<PropertyDescriptor> propertyDescriptors = new ArrayList<>();

            for (RecordComponent component : beanType.getRecordComponents()) {
                propertyDescriptors.add(new PropertyDescriptor(
                        component.getName(), component.getAccessor(), null));
            }
            return propertyDescriptors;
        }
        // Introspector does not consider superinterfaces of
        // an interface nor does it consider default methods of interfaces.
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>();

        for (Class<?> cls : beanType.getInterfaces()) {
            propertyDescriptors.addAll(internalGetBeanPropertyDescriptors(cls));
        }

        BeanInfo info = Introspector.getBeanInfo(beanType);
        propertyDescriptors.addAll(getPropertyDescriptors(info));

        return propertyDescriptors;
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
     * @return the property type
     * @throws IntrospectionException
     *             if the introspection fails
     */
    public static Class<?> getPropertyType(Class<?> beanType,
            String propertyName) throws IntrospectionException {
        PropertyDescriptor descriptor = getPropertyDescriptor(beanType,
                propertyName);
        if (descriptor != null) {
            return descriptor.getPropertyType();
        } else {
            return null;
        }
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
     *             if the introspection fails
     */
    public static PropertyDescriptor getPropertyDescriptor(Class<?> beanType,
            String propertyName) throws IntrospectionException {
        if (propertyName.contains(".")) {
            String[] parts = propertyName.split("\\.", 2);
            // Get the type of the field in the bean class
            Class<?> propertyBean = getPropertyType(beanType, parts[0]);
            // Find the rest from the sub type
            return getPropertyDescriptor(propertyBean, parts[1]);
        } else {
            List<PropertyDescriptor> descriptors = getBeanPropertyDescriptors(
                    beanType);

            for (PropertyDescriptor descriptor : descriptors) {
                final Method getMethod = descriptor.getReadMethod();
                if (descriptor.getName().equals(propertyName)
                        && getMethod != null
                        && getMethod.getDeclaringClass() != Object.class) {
                    return descriptor;
                }
            }
            return null;
        }
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
     * @throws IntrospectionException
     *             if bean introspection fails
     */
    public static List<String> getBeanPropertyPaths(Class<?> type)
            throws IntrospectionException {
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
     * @throws IntrospectionException
     *             if bean introspection fails
     */
    private static void collectPropertyPaths(String prefix, Class<?> type,
            List<String> paths) throws IntrospectionException {
        List<PropertyDescriptor> properties = getBeanPropertyDescriptors(type);

        for (PropertyDescriptor property : properties) {
            // Skip "class" property
            if ("class".equals(property.getName())) {
                continue;
            }

            String propertyName = property.getName();
            String fieldPath = prefix.isEmpty() ? propertyName
                    : prefix + "." + propertyName;
            Class<?> propertyType = property.getPropertyType();

            if (isSimpleType(propertyType)) {
                // Leaf node - add path
                paths.add(fieldPath);
            } else if (propertyType.isArray()
                    || java.util.Collection.class.isAssignableFrom(propertyType)
                    || java.util.Map.class.isAssignableFrom(propertyType)) {
                // Collection/array/map - add as-is, don't recurse
                paths.add(fieldPath);
            } else {
                // Complex bean/record type - try to recurse
                try {
                    List<PropertyDescriptor> nestedProps = getBeanPropertyDescriptors(
                            propertyType);
                    if (!nestedProps.isEmpty()) {
                        // Has properties, recurse
                        collectPropertyPaths(fieldPath, propertyType, paths);
                    } else {
                        // No properties, add as-is
                        paths.add(fieldPath);
                    }
                } catch (IntrospectionException e) {
                    // If introspection fails, just add the field as-is
                    paths.add(fieldPath);
                }
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

    // Workaround for Java6 bug JDK-6788525. Do nothing for JDK7+.
    private static List<PropertyDescriptor> getPropertyDescriptors(
            BeanInfo beanInfo) {
        PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        List<PropertyDescriptor> result = new ArrayList<>(descriptors.length);
        for (PropertyDescriptor descriptor : descriptors) {
            try {
                result.add(fixPropertyDescriptor(descriptor));
            } catch (SecurityException exception) {
                LoggerFactory.getLogger(BeanUtil.class.getName()).info(null,
                        exception);
                // handle next descriptor
            } catch (IntrospectionException e) {
                LoggerFactory.getLogger(BeanUtil.class.getName()).info(null, e);
                result.add(descriptor);
            }
        }
        return result;
    }

    private static PropertyDescriptor fixPropertyDescriptor(
            PropertyDescriptor descriptor) throws IntrospectionException {
        Method readMethod = getMethodFromBridge(descriptor.getReadMethod());
        if (readMethod != null) {
            Method writeMethod = getMethodFromBridge(
                    descriptor.getWriteMethod(), readMethod.getReturnType());
            if (writeMethod == null) {
                writeMethod = descriptor.getWriteMethod();
            }
            return new PropertyDescriptor(descriptor.getName(), readMethod,
                    writeMethod);
        } else {
            return descriptor;
        }
    }

    /**
     * Return declared method for which {@code bridgeMethod} is generated. If
     * {@code bridgeMethod} is not a bridge method then return null.
     */
    private static Method getMethodFromBridge(Method bridgeMethod)
            throws SecurityException {
        if (bridgeMethod == null) {
            return null;
        }
        return getMethodFromBridge(bridgeMethod,
                bridgeMethod.getParameterTypes());
    }

    /**
     * Return declared method for which {@code bridgeMethod} is generated using
     * its {@code paramTypes}. If {@code bridgeMethod} is not a bridge method
     * then return null.
     */
    private static Method getMethodFromBridge(Method bridgeMethod,
            Class<?>... paramTypes) throws SecurityException {
        if (bridgeMethod == null || !bridgeMethod.isBridge()) {
            return null;
        }
        try {
            return bridgeMethod.getDeclaringClass()
                    .getMethod(bridgeMethod.getName(), paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
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
