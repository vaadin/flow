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
package com.vaadin.flow.server.scanner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * A class finder using io.github.classgraph.
 * <p>
 * This implementation uses the ClassGraph library for fast classpath scanning.
 * ClassGraph parses bytecode directly without loading classes, making it
 * significantly faster than reflection-based approaches.
 * <p>
 * Note: Despite the class name, this implementation uses ClassGraph library,
 * not the org.reflections library. The class name is maintained for backward
 * compatibility.
 *
 * @since 2.0
 */
public class ReflectionsClassFinder implements ClassFinder {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ReflectionsClassFinder.class);
    private final transient ClassLoader classLoader;

    // Cache all discovered classes by annotation
    private final transient Map<String, Set<String>> annotatedClassCache;
    // Cache all discovered subclasses by parent type
    private final transient Map<String, Set<String>> subtypeCache;
    // Scanned packages for filtering
    private final transient Set<String> scannedPackages;

    /**
     * Constructor.
     *
     * @param urls
     *            the list of urls for finding classes.
     */
    public ReflectionsClassFinder(URL... urls) {
        this(new URLClassLoader(urls,
                Thread.currentThread().getContextClassLoader()), urls);
    }

    /**
     * Constructor with explicit class loader.
     *
     * @param classLoader
     *            the class loader to use for loading classes
     * @param urls
     *            the list of urls for finding classes
     */
    public ReflectionsClassFinder(ClassLoader classLoader, URL... urls) {
        this.classLoader = classLoader;
        long startTime = System.currentTimeMillis();

        // When URLs are empty or null, it means scan nothing (isolation mode)
        // Initialize with empty caches instead of scanning
        if (urls == null || urls.length == 0) {
            this.scannedPackages = Collections.emptySet();
            this.annotatedClassCache = Collections.emptyMap();
            this.subtypeCache = Collections.emptyMap();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "ClassFinder initialized with empty scan URLs: 0 classes scanned");
            }
            return;
        }

        // Configure ClassGraph scanner with provided URLs
        ClassGraph classGraph = new ClassGraph()
                .overrideClasspath((Object[]) urls).addClassLoader(classLoader)
                .enableClassInfo().enableAnnotationInfo().enableMemoryMapping()
                .ignoreClassVisibility() // Scan non-public classes
                .filterClasspathElements(
                        path -> !path.endsWith("module-info.class"));

        // Scan and extract all data, then close immediately
        int classCount;
        try (ScanResult scanResult = classGraph.scan()) {
            classCount = scanResult.getAllClasses().size();

            // Extract scanned packages
            this.scannedPackages = scanResult.getAllClasses().stream()
                    .map(classInfo -> extractPackageName(classInfo.getName()))
                    .filter(pkg -> !pkg.isEmpty()).collect(Collectors.toSet());

            // Pre-cache all annotated classes
            this.annotatedClassCache = buildAnnotatedClassCache(scanResult);

            // Pre-cache all subtype relationships
            this.subtypeCache = buildSubtypeCache(scanResult);
        } // scanResult automatically closed here

        if (LOGGER.isDebugEnabled()) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.debug(
                    "ClassFinder initialized: {} classes scanned, {} annotation types cached, {} subtype relationships cached, took {}ms",
                    classCount, annotatedClassCache.size(), subtypeCache.size(),
                    duration);
        }
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> clazz) {
        Set<Class<?>> classes = new LinkedHashSet<>();

        // Get directly annotated classes from cache
        Set<String> classNames = annotatedClassCache
                .getOrDefault(clazz.getName(), Collections.emptySet());

        for (String className : classNames) {
            try {
                classes.add(classLoader.loadClass(className));
            } catch (Throwable e) {
                LOGGER.debug("Can't load class {}", className, e);
            }
        }

        // Handle @Repeatable annotations
        classes.addAll(getAnnotatedByRepeatedAnnotation(clazz));

        return sortedByClassName(classes);
    }

    private Set<Class<?>> getAnnotatedByRepeatedAnnotation(
            AnnotatedElement annotationClass) {
        Repeatable repeatableAnnotation = annotationClass
                .getAnnotation(Repeatable.class);
        if (repeatableAnnotation != null) {
            Set<Class<?>> classes = new LinkedHashSet<>();
            Set<String> classNames = annotatedClassCache.getOrDefault(
                    repeatableAnnotation.value().getName(),
                    Collections.emptySet());

            for (String className : classNames) {
                try {
                    classes.add(classLoader.loadClass(className));
                } catch (Throwable e) {
                    LOGGER.debug("Can't load class {}", className, e);
                }
            }
            return classes;
        }
        return Collections.emptySet();
    }

    @Override
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    @Override
    public boolean shouldInspectClass(String className) {
        String packageName = extractPackageName(className);
        if (scannedPackages.contains(packageName)) {
            return classLoader.getResource(
                    className.replace('.', '/') + ".class") != null;
        }
        return false;
    }

    private static String extractPackageName(String className) {
        int dot = className.lastIndexOf('.');
        if (dot != -1) {
            return className.substring(0, dot);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> loadClass(String name) throws ClassNotFoundException {
        return (Class<T>) classLoader.loadClass(name);
    }

    @Override
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        Set<String> subtypeNames = subtypeCache.getOrDefault(type.getName(),
                Collections.emptySet());

        Set<Class<? extends T>> classes = new LinkedHashSet<>();
        for (String className : subtypeNames) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends T> clazz = (Class<? extends T>) classLoader
                        .loadClass(className);
                classes.add(clazz);
            } catch (Throwable e) {
                LOGGER.debug("Can't load class {}", className, e);
            }
        }

        return sortedByClassName(classes);
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private <T> Set<Class<? extends T>> sortedByClassName(
            Set<Class<? extends T>> source) {
        return source.stream().sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Builds cache of all classes grouped by their annotations. Maps annotation
     * class name -> Set of annotated class names
     */
    private Map<String, Set<String>> buildAnnotatedClassCache(
            ScanResult scanResult) {
        Map<String, Set<String>> cache = new HashMap<>();

        // Get all classes with any annotation
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            for (AnnotationInfo annotationInfo : classInfo
                    .getAnnotationInfo()) {
                String annotationName = annotationInfo.getName();
                cache.computeIfAbsent(annotationName,
                        k -> new LinkedHashSet<>()).add(classInfo.getName());
            }
        }

        return cache;
    }

    /**
     * Builds cache of all subtype relationships. Maps parent class/interface
     * name -> Set of subclass/implementor names
     */
    private Map<String, Set<String>> buildSubtypeCache(ScanResult scanResult) {
        Map<String, Set<String>> cache = new HashMap<>();

        // For each scanned class, register it under all its supertypes
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            String className = classInfo.getName();

            // Register under all superclasses
            ClassInfoList superclasses = classInfo.getSuperclasses();
            for (ClassInfo superclass : superclasses) {
                cache.computeIfAbsent(superclass.getName(),
                        k -> new LinkedHashSet<>()).add(className);
            }

            // Register under all implemented interfaces
            ClassInfoList interfaces = classInfo.getInterfaces();
            for (ClassInfo iface : interfaces) {
                cache.computeIfAbsent(iface.getName(),
                        k -> new LinkedHashSet<>()).add(className);
            }
        }

        return cache;
    }
}
