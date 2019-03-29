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
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract class which allows to find classes in the project classpath.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class ClassPathIntrospector implements Serializable {

    /**
     * Interface for annotated class searches.
     */
    public interface ClassFinder extends Serializable {
        /**
         * Get annotated classes in the classloader.
         *
         * @param clazz the annotation
         * @return a set with all classes that are annotated
         */
        Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> clazz);
        /**
         * Get a resource from the classpath
         *
         * @param name class literal
         * @return the resource
         */
        URL getResource(String name);

        /**
         * Load a class in the classloader
         *
         * @param name
         *            the class literal
         * @return the class
         * @throws ClassNotFoundException
         *             when the class is not in the classpath
         */
        <T> Class<T> loadClass(String name) throws ClassNotFoundException ;
    }

    private final ClassFinder finder;

    /**
     * Creates a new instance of class.
     *
     * @param finder the ClassFinder instance to use for class searches.
     */
    protected ClassPathIntrospector(ClassFinder finder) {
        this.finder = finder;
    }

    public ClassPathIntrospector(ClassPathIntrospector otherIntrospector) {
        this.finder = otherIntrospector.finder;
    }

    /**
     * Returns a resource {@link URL} given a file name.
     * 
     * @param name
     *            the name of the resource
     * @return the URL with the resource or null if not found
     */
    protected URL getResource(String name) {
        return finder.getResource(name);
    }

    /**
     * Gets the classes annotated with the given
     * {@code annotationInProjectContext} annotation.
     *
     * @param annotationInProjectContext
     *            annotation class
     * @return all classes annotated with the given annotation
     */
    protected Stream<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> annotationInProjectContext) {
        return finder.getAnnotatedClasses(annotationInProjectContext).stream();
    }

    /**
     * Returns the annotation class by its FQN.
     * <p>
     * Note that the resulting class is loaded by project classloader ( not the
     * maven plugin classloader).
     *
     * @param className
     *            the FQN of the class to load
     * @param <T>
     *            the class to be loaded
     *
     * @return the class with the given {@code className} loaded by the project
     *         class loader
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> loadClassInProjectClassLoader(String className) {
        try {
            return (Class<T>) finder.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format(
                    "Failed to load class '%s' in custom classloader",
                    className), e);
        }
    }

    /**
     * Get the value of the method {@code methodName} from the
     * {@code target} annotation.
     *
     * @param target
     *            the target annotation
     * @param methodName
     *            the method name to execute
     * @return the Value of the execution result
     */
    @SuppressWarnings("unchecked")
    protected <T> T invokeAnnotationMethod(Annotation target, String methodName) {
        return (T) doInvokeAnnotationMethod(target, methodName);
    }

    /**
     * Get the value of the method {@code methodName} from the {@code target}
     * annotation.
     *
     * @param target
     *            the target annotation
     * @param methodName
     *            the method name to execute
     * @return the result of method {@code methodName} invocation on the
     *         {@code target}
     */
    protected Object doInvokeAnnotationMethod(Annotation target,
            String methodName) {
        try {
            return target.getClass().getDeclaredMethod(methodName)
                    .invoke(target);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(String.format(
                    "Failed to access method '%s' in annotation interface '%s', should not be happening due to JLS definition of annotation interface",
                    methodName, target), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(String.format(
                    "Got an exception by invoking method '%s' from annotation '%s'",
                    methodName, target), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    String.format("Annotation '%s' has no method named `%s",
                            target, methodName),
                    e);
        }
    }

    /**
     * Get the value of the the method {@code methodName} from the
     * {@code instance} with the given {@code arguments}.
     *
     * @param instance
     *            instance with the method to invoke
     * @param methodName
     *            the method name
     * @param arguments
     *            a variable list with the arguments for the method invocation
     * @return the object resulting from the method invocation
     */
    @SuppressWarnings("unchecked")
    public <T> T doInvokeMethod(Object instance, String methodName, Object... arguments) {
        try {
            for (Method m : instance.getClass().getMethods()) {
                if (m.getName().equals(methodName)) {
                    return (T) m.invoke(instance, arguments);
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
        return null;
    }
}
