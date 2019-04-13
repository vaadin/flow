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

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.bytebuddy.jar.asm.ClassReader;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import com.vaadin.flow.server.frontend.FlowClassVisitor.EndPoint;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Represents the class dependency tree of the application.
 */
public class FrontendDependencies implements Serializable {

    private final ClassFinder finder;
    private final HashMap<String, EndPoint> endPoints = new HashMap<>();
    private final Class<?> hasElement;
    private final Class<?> abstractTheme;

    /**
     * Default constructor.
     *
     * @param finder
     *            the class finder used in the application
     */
    public FrontendDependencies(ClassFinder finder) {
        this.finder = finder;
        try {
            this.hasElement = finder.loadClass(HasElement.class.getName());
            this.abstractTheme = finder.loadClass(AbstractTheme.class.getName());

            for (Class<?> route : finder.getAnnotatedClasses(Route.class) ) {
                String className = route.getName();
                endPoints.put(className, visitClass(className, new FlowClassVisitor.EndPoint(route)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public Set<String> getAllPackages() {
        Set<String> all = new HashSet<>();
        for (FlowClassVisitor.EndPoint r : endPoints.values()) {
            all.addAll(r.packages);
        }
        return all;
    }
    public Set<String> getAllModules() {
        Set<String> all = new HashSet<>();
        for (FlowClassVisitor.EndPoint r : endPoints.values()) {
            all.addAll(r.modules);
        }
        return all;
    }
    public Set<String> getAllScripts() {
        Set<String> all = new HashSet<>();
        for (FlowClassVisitor.EndPoint r : endPoints.values()) {
            all.addAll(r.scripts);
        }
        return all;
    }
    public Set<String> getAllImports() {
        Set<String> all = new HashSet<>();
        for (FlowClassVisitor.EndPoint r : endPoints.values()) {
            for (Entry<String, Set<String>> e : r.imports.entrySet()) {
                if (!r.npmClasses.contains(e.getKey())) {
                    all.addAll(e.getValue());
                }
            }
        }
        return all;
    }

    public Class<?> getTheme() {
        ThemeDefinition theme = getTheme(null);
        return theme == null ? null : theme.getTheme();
    }

    public ThemeDefinition getTheme(String defaultTheme) {
        for (FlowClassVisitor.EndPoint r : endPoints.values()) {
            if (r.route.isEmpty() && !r.notheme) {
                String theme = r.theme != null ? r.theme : defaultTheme;
                if (theme != null) {
                    try {
                        return new ThemeDefinition(finder.loadClass(theme), r.variant != null ? r.variant : "");
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(
                                "Unable to load application theme because it's not in the classpath", e);
                    }
                }
            }
        }
        return null;
    }

    private boolean isVisitable(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        try {
            Class<?> clazz = finder.loadClass(className);
            // Visit only components and themes
            return hasElement.isAssignableFrom(clazz) || abstractTheme.isAssignableFrom(clazz);
        } catch (Throwable ignore) { //NOSONAR
            // Ignore classes that cannot be loaded by the finder
            return false;
        }
    }

    private URL getUrl(String className) {
        return finder.getResource(className.replace(".", "/") + ".class");
    }

    /**
     * Recursive method for visiting class names using bytecode inspection.
     *
     * @param className
     * @param endPoint
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private EndPoint visitClass(String className, FlowClassVisitor.EndPoint endPoint)
                    throws IOException, ClassNotFoundException {

        if (endPoint.classes.contains(className)) {
            return endPoint;
        }
        endPoint.classes.add(className);

        URL url = getUrl(className);
        if (url == null) {
            return endPoint;
        }

        FlowClassVisitor visitor = new FlowClassVisitor(className, endPoint);
        ClassReader cr = new ClassReader(url.openStream());
        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        for (String s : visitor.getChildren()) {
            if (isVisitable(s)) {
                visitClass(s, endPoint);
            }
        }

        if (className.equals(endPoint.name)) {
            if (!endPoint.notheme && endPoint.route.isEmpty() && endPoint.theme != null) {
                visitClass(endPoint.theme, endPoint);
            }
        }

        return endPoint;
    }

    @Override
    public String toString() {
        return endPoints.toString();
    }
}
