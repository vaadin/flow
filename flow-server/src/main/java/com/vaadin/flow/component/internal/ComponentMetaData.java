/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Immutable meta data related to a component class.
 *
 * @author Vaadin Ltd
 */
public class ComponentMetaData {

    /**
     * Dependencies defined for a {@link Component} class.
     * <p>
     * Framework internal class, thus package-private.
     */
    public static class DependencyInfo {
        private final List<HtmlImport> htmlImports = new ArrayList<>();
        private final List<JavaScript> javaScripts = new ArrayList<>();
        private final List<StyleSheet> styleSheets = new ArrayList<>();

        List<HtmlImport> getHtmlImports() {
            return Collections.unmodifiableList(htmlImports);
        }

        List<JavaScript> getJavaScripts() {
            return Collections.unmodifiableList(javaScripts);
        }

        List<StyleSheet> getStyleSheets() {
            return Collections.unmodifiableList(styleSheets);
        }

    }

    /**
     * Synchronized properties defined for a {@link Component} class.
     * <p>
     * Framework internal class, thus package-private.
     */
    public static class SynchronizedPropertyInfo {
        private final String property;
        private final String[] eventNames;

        SynchronizedPropertyInfo(String property, String[] eventNames) {
            this.property = property;
            this.eventNames = eventNames;
        }

        public String getProperty() {
            return property;
        }

        public Stream<String> getEventNames() {
            return Stream.of(eventNames);
        }
    }

    private Collection<SynchronizedPropertyInfo> synchronizedProperties;
    private DependencyInfo dependencyInfo;

    /**
     * Scans the given component class and creates a new instance based on found
     * annotations.
     *
     * @param componentClass
     *            the component class to scan
     */
    public ComponentMetaData(Class<? extends Component> componentClass) {
        synchronizedProperties = findSynchronizedProperties(componentClass);
        dependencyInfo = findDependencies(componentClass);
    }

    /**
     * Finds all dependencies (HTML, JavaScript, StyleSheet) for the class.
     * Includes dependencies for all classes referred by an {@link Uses}
     * annotation.
     *
     * @return an information object containing all the dependencies
     */
    private static DependencyInfo findDependencies(
            Class<? extends Component> componentClass) {
        DependencyInfo dependencyInfo = new DependencyInfo();
        findDependencies(componentClass, dependencyInfo, new HashSet<>());
        return dependencyInfo;
    }

    private static DependencyInfo findDependencies(
            Class<? extends Component> componentClass,
            DependencyInfo dependencyInfo,
            Set<Class<? extends Component>> scannedClasses) {
        assert !scannedClasses.contains(componentClass);

        scannedClasses.add(componentClass);

        dependencyInfo.htmlImports.addAll(
                AnnotationReader.getHtmlImportAnnotations(componentClass));
        dependencyInfo.javaScripts.addAll(
                AnnotationReader.getJavaScriptAnnotations(componentClass));
        dependencyInfo.styleSheets.addAll(
                AnnotationReader.getStyleSheetAnnotations(componentClass));

        List<Uses> usesList = AnnotationReader.getAnnotationsFor(componentClass,
                Uses.class);
        for (Uses uses : usesList) {
            Class<? extends Component> otherClass = uses.value();
            if (!scannedClasses.contains(otherClass)) {
                findDependencies(otherClass, dependencyInfo, scannedClasses);
            }
        }
        return dependencyInfo;
    }

    /**
     * Gets the properties that are marked to be synchronized and corresponding
     * events.
     * <p>
     * Framework internal data, thus package-private.
     *
     * @return a collection of information objects about properties to be
     *         synchronized
     */
    public Collection<SynchronizedPropertyInfo> getSynchronizedProperties() {
        return Collections.unmodifiableCollection(synchronizedProperties);
    }

    /**
     * Gets the dependencies, defined using annotations ({@link HtmlImport},
     * {@link JavaScript}, {@link StyleSheet} and {@link Uses}).
     * <p>
     * Framework internal data, thus package-private.
     *
     * @return the dependencies for the given class
     */
    public DependencyInfo getDependencyInfo() {
        return dependencyInfo;
    }

    /**
     * Scans the class for {@link Synchronize} annotations and gathers the data.
     *
     * @param componentClass
     *            the class to scan
     * @return a set of information objects about properties to be synchronized
     */
    private static Collection<SynchronizedPropertyInfo> findSynchronizedProperties(
            Class<? extends Component> componentClass) {
        Map<String, SynchronizedPropertyInfo> infos = new HashMap<>();
        collectSynchronizedProperties(componentClass, infos);
        return infos.values();
    }

    private static void collectSynchronizedProperties(Class<?> clazz,
            Map<String, SynchronizedPropertyInfo> infos) {
        if (clazz == null || clazz.equals(Object.class)) {
            return;
        }
        doCollectSynchronizedProperties(clazz, infos);

        Class<?> superclass = clazz.getSuperclass();
        collectSynchronizedProperties(superclass, infos);

        Stream.of(clazz.getInterfaces())
                .forEach(iface -> collectSynchronizedProperties(iface, infos));
    }

    private static void doCollectSynchronizedProperties(Class<?> clazz,
            Map<String, SynchronizedPropertyInfo> infos) {
        for (Method method : clazz.getDeclaredMethods()) {
            Synchronize annotation = method.getAnnotation(Synchronize.class);
            if (annotation == null) {
                continue;
            }

            if (!ReflectTools.isGetter(method)) {
                throw new IllegalStateException(method + " is annotated with @"
                        + Synchronize.class.getSimpleName()
                        + " even though it's not a getter.");
            }

            if (infos.containsKey(method.getName())) {
                continue;
            }

            String propertyName;
            if (annotation.property().isEmpty()) {
                propertyName = ReflectTools.getPropertyName(method);
            } else {
                propertyName = annotation.property();
            }

            String[] eventNames = annotation.value();
            infos.put(method.getName(),
                    new SynchronizedPropertyInfo(propertyName, eventNames));
        }
    }

}
