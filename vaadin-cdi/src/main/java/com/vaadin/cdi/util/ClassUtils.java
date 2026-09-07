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
package com.vaadin.cdi.util;

import jakarta.enterprise.inject.Typed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Typed()
public abstract class ClassUtils {
    /**
     * Constructor which prevents the instantiation of this class
     */
    private ClassUtils() {
        // prevent instantiation
    }

    /**
     * Detect the right ClassLoader. The lookup order is determined by:
     * <ol>
     * <li>ContextClassLoader of the current Thread</li>
     * <li>ClassLoader of the given Object 'o'</li>
     * <li>ClassLoader of this very ClassUtils class</li>
     * </ol>
     *
     * @param o
     *            if not <code>null</code> it may get used to detect the
     *            classloader.
     * @return The {@link ClassLoader} which should get used to create new
     *         instances
     */
    public static ClassLoader getClassLoader(Object o) {
        if (System.getSecurityManager() != null) {
            return AccessController
                    .doPrivileged(new ClassUtils.GetClassLoaderAction(o));
        } else {
            return getClassLoaderInternal(o);
        }
    }

    static class GetClassLoaderAction implements PrivilegedAction<ClassLoader> {
        private Object object;

        GetClassLoaderAction(Object object) {
            this.object = object;
        }

        @Override
        public ClassLoader run() {
            try {
                return getClassLoaderInternal(object);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static ClassLoader getClassLoaderInternal(Object o) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader == null && o != null) {
            loader = o.getClass().getClassLoader();
        }

        if (loader == null) {
            loader = ClassUtils.class.getClassLoader();
        }

        return loader;
    }

    /**
     * Checks whether the CDI rules for proxyable beans are met. See <a href=
     * "https://docs.jboss.org/cdi/spec/1.2/cdi-spec-with-assertions.html#unproxyable">
     * CDI spec unproxyable bean types</a>
     *
     * @param type
     * @return {@code true} if all proxy conditions are met, {@code false}
     *         otherwise
     */
    public static boolean isProxyableClass(Type type) {
        Class clazz = null;
        if (type instanceof Class) {
            clazz = (Class) type;
        }
        if (type instanceof ParameterizedType
                && ((ParameterizedType) type).getRawType() instanceof Class) {
            clazz = (Class) ((ParameterizedType) type).getRawType();
        }
        if (clazz == null) {
            return false;
        }

        // classes which don’t have a non-private constructor with no parameters
        try {
            Constructor constructor = clazz.getConstructor();
            if (Modifier.isPrivate(constructor.getModifiers())) {
                return false;
            }
        } catch (NoSuchMethodException e) {
            return false;
        }

        // classes which are declared final
        if (Modifier.isFinal(clazz.getModifiers())) {
            return false;
        }

        // classes which have non-static, final methods with public, protected
        // or default visibility,
        for (Method method : clazz.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            if (!method.isBridge() && !method.isSynthetic()
                    && !Modifier.isStatic(method.getModifiers())
                    && !Modifier.isPrivate(method.getModifiers())
                    && Modifier.isFinal(method.getModifiers())) {
                return false;
            }
        }

        // primitive types,
        // and array types.
        if (clazz.isPrimitive() || clazz.isArray()) {
            return false;

        }
        return true;
    }

    /**
     * Tries to load a class based on the given name and interface or abstract
     * class.
     * 
     * @param name
     *            name of the concrete class
     * @param targetType
     *            target type (interface or abstract class)
     * @param <T>
     *            current type
     * @return loaded class or null if it isn't in the classpath
     */
    public static <T> Class<T> tryToLoadClassForName(String name,
            Class<T> targetType) {
        return (Class<T>) tryToLoadClassForName(name);
    }

    /**
     * Tries to load a class based on the given name and interface or abstract
     * class.
     * 
     * @param name
     *            name of the concrete class
     * @param targetType
     *            target type (interface or abstract class)
     * @param classLoader
     *            The {@link ClassLoader}.
     * @param <T>
     *            current type
     * @return loaded class or null if it isn't in the classpath
     */
    public static <T> Class<T> tryToLoadClassForName(String name,
            Class<T> targetType, ClassLoader classLoader) {
        return (Class<T>) tryToLoadClassForName(name, classLoader);
    }

    /**
     * Tries to load a class based on the given name
     * 
     * @param name
     *            name of the class
     * @return loaded class or <code>null</code> if it isn't in the classpath
     */
    public static Class tryToLoadClassForName(String name) {
        try {
            return loadClassForName(name);
        } catch (ClassNotFoundException e) {
            // do nothing - it's just a try
            return null;
        }
    }

    /**
     * Tries to load a class based on the given name
     * 
     * @param name
     *            name of the class
     * @param classLoader
     *            The {@link ClassLoader}.
     * @return loaded class or <code>null</code> if it isn't in the classpath
     */
    public static Class tryToLoadClassForName(String name,
            ClassLoader classLoader) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            // do nothing - it's just a try
            return null;
        }
    }

    /**
     * Loads class for the given name
     * 
     * @param name
     *            name of the class
     * @return loaded class
     * @throws ClassNotFoundException
     *             if the class can't be loaded
     */
    public static Class loadClassForName(String name)
            throws ClassNotFoundException {
        try {
            // Try WebApp ClassLoader first
            return Class.forName(name, false, // do not initialize for faster
                                              // startup
                    getClassLoader(null));
        } catch (ClassNotFoundException ignore) {
            // fallback: Try ClassLoader for ClassUtils (i.e. the myfaces.jar
            // lib)
            return Class.forName(name, false, // do not initialize for faster
                                              // startup
                    ClassUtils.class.getClassLoader());
        }
    }

    /**
     * Instantiates a given class via the default constructor
     * 
     * @param targetClass
     *            class which should be instantiated
     * @param <T>
     *            current type
     * @return created instance or null if the instantiation failed
     */
    public static <T> T tryToInstantiateClass(Class<T> targetClass) {
        try {
            return targetClass.newInstance();
        } catch (InstantiationException e) {
            // do nothing - it was just a try
        } catch (IllegalAccessException e) {
            // do nothing - it was just a try
        }
        return null;
    }

    /**
     * Tries to instantiate a class for the given name and type via the default
     * constructor
     * 
     * @param className
     *            name of the class
     * @param targetType
     *            target type
     * @param <T>
     *            current type
     * @return created instance or null if the instantiation failed
     */
    public static <T> T tryToInstantiateClassForName(String className,
            Class<T> targetType) {
        Object result = tryToInstantiateClassForName(className);

        // noinspection unchecked
        return result != null ? (T) result : null;
    }

    /**
     * Tries to instantiate a class for the given name via the default
     * constructor
     * 
     * @param className
     *            name of the class
     * @return created instance or null if the instantiation failed
     */
    public static Object tryToInstantiateClassForName(String className) {
        try {
            return instantiateClassForName(className);
        } catch (Exception e) {
            // do nothing - it was just a try
        }
        return null;
    }

    /**
     * Creates an instance for the given class-name
     * 
     * @param className
     *            name of the class which should be instantiated
     * @return created instance
     * @throws ClassNotFoundException
     *             if the instantiation failed
     * @throws IllegalAccessException
     *             if the instantiation failed
     * @throws InstantiationException
     *             if the instantiation failed
     */
    public static Object instantiateClassForName(String className)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        return loadClassForName(className).newInstance();
    }

    /**
     * Reads the version of the jar which contains the given class
     * 
     * @param targetClass
     *            class within the jar
     * @return version-string which has been found in the manifest or null if
     *         there is no version information available
     */
    public static String getJarVersion(Class targetClass) {
        String manifestFileLocation = getManifestFileLocationOfClass(
                targetClass);

        try {
            return new Manifest(new URL(manifestFileLocation).openStream())
                    .getMainAttributes()
                    .getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Reads the VCS revision which was used for creating the jar
     * 
     * @param targetClass
     *            class within the jar
     * @return revision-string which has been found in the manifest or null if
     *         there is no information available
     */
    public static String getRevision(Class targetClass) {
        String manifestFileLocation = getManifestFileLocationOfClass(
                targetClass);

        try {
            return new Manifest(new URL(manifestFileLocation).openStream())
                    .getMainAttributes().getValue("Revision");
        } catch (Exception e) {
            return null;
        }
    }

    private static String getManifestFileLocationOfClass(Class targetClass) {
        String manifestFileLocation;

        try {
            manifestFileLocation = getManifestLocation(targetClass);
        } catch (Exception e) {
            // in this case we have a proxy
            manifestFileLocation = getManifestLocation(
                    targetClass.getSuperclass());
        }
        return manifestFileLocation;
    }

    private static String getManifestLocation(Class targetClass) {
        String classFilePath = targetClass.getCanonicalName().replace('.', '/')
                + ".class";
        String manifestFilePath = "/META-INF/MANIFEST.MF";

        String classLocation = targetClass
                .getResource(targetClass.getSimpleName() + ".class").toString();
        return classLocation.substring(0,
                classLocation.indexOf(classFilePath) - 1) + manifestFilePath;
    }

    /**
     * Checks if the given class contains a method with the same signature.
     *
     * @param targetClass
     *            The class to check
     * @param method
     *            The source method
     * @return if it contains a method with the same signature.
     */
    public static boolean containsMethod(Class<?> targetClass, Method method) {
        return extractMethod(targetClass, method) != null;
    }

    /**
     * Extracts a method with same signature as the source method.
     *
     * @param clazz
     *            The target class
     * @param sourceMethod
     *            The source method.
     * @return the extracted method or <code>null</code>
     */
    public static Method extractMethod(Class<?> clazz, Method sourceMethod) {
        try {
            String name = sourceMethod.getName();
            return clazz != null
                    ? clazz.getMethod(name, sourceMethod.getParameterTypes())
                    : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Extract a method with the given name and parameterTypes. Return
     * {@code null} if no such visible method exists on the given class.
     *
     * @param clazz
     * @param methodName
     * @param parameterTypes
     * @return
     */
    public static Method extractMethod(Class<?> clazz, String methodName,
            Class<?>... parameterTypes) {
        try {
            return clazz != null ? clazz.getMethod(methodName, parameterTypes)
                    : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Checks if the given class has a method with the same signature, taking in
     * to account generic types
     * 
     * @param targetClass
     * @param method
     * @return if it contains a method with the same signature.
     */
    public static boolean containsPossiblyGenericMethod(Class<?> targetClass,
            Method method) {
        return extractPossiblyGenericMethod(targetClass, method) != null;
    }

    /**
     * Extracts a method matching the source method, allowing generic type
     * parameters to be substituted as long as they are properly castable.
     *
     * @param clazz
     *            The target class
     * @param sourceMethod
     *            The source method.
     * @return the extracted method or <code>null</code>
     */
    public static Method extractPossiblyGenericMethod(Class<?> clazz,
            Method sourceMethod) {
        Method exactMethod = extractMethod(clazz, sourceMethod);
        if (exactMethod == null) {
            String methodName = sourceMethod.getName();
            Class<?>[] parameterTypes = sourceMethod.getParameterTypes();
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && allSameType(
                        method.getParameterTypes(), parameterTypes)) {
                    return method;
                }
            }
            return null;
        } else {
            return exactMethod;
        }
    }

    /**
     * Whether all of the parameters from left to right are equivalent. In order
     * to support generics, it takes the form of left.isAssignableFrom(right)
     * 
     * @param left
     *            left hand side to check
     * @param right
     *            right hand side to check
     * @return whether all of the left classes can be assigned to the right hand
     *         side types
     */
    private static boolean allSameType(Class<?>[] left, Class<?>[] right) {
        if (left.length != right.length) {
            return false;
        }
        for (int p = 0; p < left.length; p++) {
            if (!left[p].isAssignableFrom(right[p])) {
                return false;
            }
        }
        return true;
    }

    public static boolean returns(Method method, Class<?> clazz) {
        return method.getReturnType().isAssignableFrom(clazz);
    }
}
