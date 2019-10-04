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
package com.vaadin.flow.server.osgi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;

import org.osgi.framework.Bundle;
import org.slf4j.LoggerFactory;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.HasErrorParameter;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

/**
 * Manages scanned classes inside OSGi container.
 * <p>
 * It doesn't do anything outside of OSGi.
 *
 * @author Vaadin Ltd
 * @since 1.2
 * 
 * @see #getInstance()
 */
public final class OSGiAccess {
    private static final OSGiAccess INSTANCE = new OSGiAccess();

    private final ServletContext context = LazyOSGiDetector.IS_IN_OSGI
            ? createOSGiServletContext()
            : null;

    private final AtomicReference<Collection<Class<? extends ServletContainerInitializer>>> initializerClasses = LazyOSGiDetector.IS_IN_OSGI
            ? new AtomicReference<>()
            : null;

    private final Map<Long, Collection<Class<?>>> cachedClasses = LazyOSGiDetector.IS_IN_OSGI
            ? new ConcurrentHashMap<>()
            : null;

    private OSGiAccess() {
        // The class is a singleton. Avoid instantiation outside of the class.
    }

    /**
     * This is internal class and is not intended to be used.
     * <p>
     * It's public only because it needs to be proxied.
     * <p>
     * This class represents a singleton servlet context instance which is not a
     * real servlet context.
     */
    public abstract static class OSGiServletContext implements ServletContext {

        private final Map<String, Object> attributes = new HashMap<>();

        @Override
        public void setAttribute(String name, Object object) {
            attributes.put(name, object);
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public void log(String msg) {
            // This method is used by Atmosphere initiailizer
            LoggerFactory.getLogger(OSGiAccess.class).warn(msg);
        }

        @Override
        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            // This method is used by Atmosphere initiailizer
            return Collections.emptyMap();
        }

    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static OSGiAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Gets a servlet context instance which is used to track registries which
     * are storage of scanned classes.
     * <p>
     * This is not a real servlet context. It's just a proxied unique instance
     * which is used to be able to access registries in a generic way via some
     * {@code getInstance(ServletContext)} method.
     *
     * @return
     */
    public ServletContext getOsgiServletContext() {
        return context;
    }

    /**
     * Sets the discovered servlet context initializer classes.
     * <p>
     * The OSGi bundle tracker is used to scan all classes in bundles and it
     * also scans <b>flow-server</b> module for servlet initializer classes.
     * They are set using this method once they are collected.
     *
     * @param contextInitializers
     *            servlet context initializer classes
     */
    public void setServletContainerInitializers(
            Collection<Class<? extends ServletContainerInitializer>> contextInitializers) {
        assert contextInitializers != null;
        initializerClasses.set(new ArrayList<>(contextInitializers));
    }

    /**
     * Checks whether the servlet initializers are discovered.
     *
     * @return {@code true} if servlet initializers are set, {@code false}
     *         otherwise
     */
    public boolean hasInitializers() {
        return initializerClasses.get() != null;
    }

    /**
     * Adds scanned classes in active bundles.
     * <p>
     * The map contains a bundle id as a key and classes discovered in the
     * bundle as a value.
     *
     * @param extenderClasses
     *            a map with discovered classes in active bundles
     */
    public void addScannedClasses(
            Map<Long, Collection<Class<?>>> extenderClasses) {
        cachedClasses.putAll(extenderClasses);
        resetContextInitializers();
    }

    /**
     * Removes classes from the bundle identified by the {@code bundleId}.
     * <p>
     * When a bundle becomes inactive its classes should not be used anymore.
     * This method removes the classes from the bundle from the collection of
     * discovered classes.
     *
     * @param bundleId
     *            the bundle identifier
     */
    public void removeScannedClasses(Long bundleId) {
        cachedClasses.remove(bundleId);
        resetContextInitializers();
    }

    private void resetContextInitializers() {
        initializerClasses.get().stream().map(ReflectTools::createInstance)
                .forEach(this::handleTypes);
    }

    private void handleTypes(ServletContainerInitializer initializer) {
        Optional<HandlesTypes> handleTypes = AnnotationReader
                .getAnnotationFor(initializer.getClass(), HandlesTypes.class);
        try {
            initializer.onStartup(filterClasses(handleTypes.orElse(null)),
                    getOsgiServletContext());
        } catch (ServletException e) {
            throw new RuntimeException(
                    "Couldn't run servlet context initializer "
                            + initializer.getClass(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Class<?>> filterClasses(HandlesTypes typesAnnotation) {
        Set<Class<?>> result = new HashSet<>();
        if (typesAnnotation == null) {
            cachedClasses.forEach((bundle, classes) -> result.addAll(classes));
        } else {
            Class<?>[] requestedTypes = typesAnnotation.value();

            Predicate<Class<?>> isAnnotation = Class::isAnnotation;

            List<Class<? extends Annotation>> annotations = Stream
                    .of(requestedTypes).filter(isAnnotation)
                    .map(clazz -> (Class<? extends Annotation>) clazz)
                    .collect(Collectors.toList());

            List<Class<?>> superTypes = Stream.of(requestedTypes)
                    .filter(isAnnotation.negate()).collect(Collectors.toList());

            Predicate<Class<?>> hasType = clazz -> annotations.stream()
                    .anyMatch(annotation -> AnnotationReader
                            .getAnnotationFor(clazz, annotation).isPresent())
                    || superTypes.stream()
                            .anyMatch(superType -> GenericTypeReflector
                                    .isSuperType(HasErrorParameter.class,
                                            clazz));

            cachedClasses.forEach((bundle, classes) -> result.addAll(classes
                    .stream().filter(hasType).collect(Collectors.toList())));

        }
        return result;
    }

    private ServletContext createOSGiServletContext() {
        Builder<OSGiServletContext> builder = new ByteBuddy()
                .subclass(OSGiServletContext.class);

        Class<? extends OSGiServletContext> osgiServletContextClass = builder
                .make().load(OSGiServletContext.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        return ReflectTools.createProxyInstance(osgiServletContextClass,
                ServletContext.class);
    }

    private static final class LazyOSGiDetector {
        private static final boolean IS_IN_OSGI = isInOSGi();

        private static boolean isInOSGi() {
            try {
                Class.forName("org.osgi.framework.FrameworkUtil");

                UsageStatistics.markAsUsed("flow/osgi", getOSGiVersion());

                return true;
            } catch (ClassNotFoundException exception) {
                return false;
            }
        }

        /**
         * Tries to detect the version of the OSGi framework used.
         *
         * @return the used OSGi version or {@code null} if not able to detect
         *         it
         */
        private static String getOSGiVersion() {
            try {
                Bundle osgiBundle = org.osgi.framework.FrameworkUtil
                        .getBundle(Bundle.class);
                return osgiBundle.getVersion().toString();
            } catch (Throwable throwable) {
                // just eat it so that any failure in the version detection
                // doesn't break OSGi usage
                LoggerFactory.getLogger(OSGiAccess.class).info(
                        "Unable to detect used OSGi framework version due to "
                                + throwable.getMessage());
            }
            return null;
        }

    }
}
