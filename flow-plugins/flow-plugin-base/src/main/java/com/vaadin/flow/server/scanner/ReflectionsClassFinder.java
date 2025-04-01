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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javassist.bytecode.ClassFile;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryBuilder;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * A class finder using org.reflections.
 *
 * @since 2.0
 */
public class ReflectionsClassFinder implements ClassFinder {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ReflectionsClassFinder.class);
    private final transient ClassLoader classLoader;

    private final transient Reflections reflections;

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

    public ReflectionsClassFinder(ClassLoader classLoader, URL... urls) {
        this.classLoader = classLoader;
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .addClassLoaders(classLoader).setExpandSuperTypes(false)
                .addUrls(urls);

        ConfigurationBuilder.DEFAULT_SCANNERS
                .forEach(configurationBuilder::addScanners);
        configurationBuilder.addScanners(PackageScanner.INSTANCE);
        configurationBuilder
                .setInputsFilter(resourceName -> resourceName.endsWith(".class")
                        && !resourceName.endsWith("module-info.class"));

        // Adding the custom URL type handler at the end, as a last resort to
        // prevent warning messages on server logs
        // Vfs.getDefaultUrlTypes() gets the internal mutable collection
        List<Vfs.UrlType> defaultUrlTypes = Vfs.getDefaultUrlTypes();
        if (!defaultUrlTypes.contains(IGNORE_NOT_HANDLED_FILES)) {
            defaultUrlTypes.add(IGNORE_NOT_HANDLED_FILES);
        }
        try {
            reflections = new LoggingReflections(configurationBuilder);
        } finally {
            defaultUrlTypes.remove(IGNORE_NOT_HANDLED_FILES);
        }
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> clazz) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.addAll(reflections.getTypesAnnotatedWith(clazz, true));
        classes.addAll(getAnnotatedByRepeatedAnnotation(clazz));
        return sortedByClassName(classes);

    }

    private Set<Class<?>> getAnnotatedByRepeatedAnnotation(
            AnnotatedElement annotationClass) {
        Repeatable repeatableAnnotation = annotationClass
                .getAnnotation(Repeatable.class);
        if (repeatableAnnotation != null) {
            return reflections
                    .getTypesAnnotatedWith(repeatableAnnotation.value(), true);
        }
        return Collections.emptySet();
    }

    @Override
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    @Override
    public boolean shouldInspectClass(String className) {
        if (!reflections
                .get(PackageScanner.INSTANCE
                        .of(PackageScanner.extractPackageName(className)))
                .isEmpty()) {
            return classLoader.getResource(
                    className.replace('.', '/') + ".class") != null;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> loadClass(String name) throws ClassNotFoundException {
        return (Class<T>) classLoader.loadClass(name);
    }

    @Override
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        return sortedByClassName(reflections.getSubTypesOf(type));
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

    private static class LoggingReflections extends Reflections {

        LoggingReflections(Configuration configuration) {
            super(configuration);
        }

        // Classloading errors may cause the frontend-build to fail, but
        // without any useful information.
        // Copy-pasting the super method, with addition of exception logging
        // to help in troubleshooting build issues
        @Override
        public Class<?> forClass(String typeName, ClassLoader... loaders) {
            if (primitiveNames.contains(typeName)) {
                return primitiveTypes.get(primitiveNames.indexOf(typeName));
            } else {
                String type;
                if (typeName.contains("[")) {
                    int i = typeName.indexOf("[");
                    type = typeName.substring(0, i);
                    String array = typeName.substring(i).replace("]", "");
                    if (primitiveNames.contains(type)) {
                        type = primitiveDescriptors
                                .get(primitiveNames.indexOf(type));
                    } else {
                        type = "L" + type + ";";
                    }
                    type = array + type;
                } else {
                    type = typeName;
                }

                for (ClassLoader classLoader : ClasspathHelper
                        .classLoaders(loaders)) {
                    if (type.contains("[")) {
                        try {
                            return Class.forName(type, false, classLoader);
                        } catch (Throwable ignored) {
                            LOGGER.debug("Can't find class {}", type, ignored);
                        }
                    }
                    try {
                        return classLoader.loadClass(type);
                    } catch (Throwable ignored) {
                        LOGGER.debug("Can't load class {}", type, ignored);
                    }
                }
                return null;
            }
        }
    }

    private static final Vfs.UrlType IGNORE_NOT_HANDLED_FILES = new Vfs.UrlType() {

        public boolean matches(URL url) {
            // This handler is the last one to be checked.
            // Valid "file:" URLs should have already been handled by default
            // URL type handlers.
            return "file".equals(url.getProtocol());
        }

        public Vfs.Dir createDir(final URL url) {
            LOGGER.debug(
                    "Class finder cannot scan {} URL. Probably pointing to a not existing folder.",
                    url);
            return new Vfs.Dir() {
                @Override
                public String getPath() {
                    return url.getPath().replace("\\", "/");
                }

                @Override
                public Iterable<Vfs.File> getFiles() {
                    return Collections.emptyList();
                }
            };
        }
    };

    private static class PackageScanner
            implements Scanner, QueryBuilder, NameHelper {

        private final static PackageScanner INSTANCE = new PackageScanner();

        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            String packageName = extractPackageName(classFile.getName());
            if (!packageName.isEmpty()) {
                return List.of(entry(packageName, packageName));
            }
            return List.of();
        }

        @Override
        public String index() {
            return "PackageScanner";
        }

        static String extractPackageName(String className) {
            int dot = className.lastIndexOf('.');
            if (dot != -1) {
                return className.substring(0, dot);
            }
            return "";
        }
    }
}
