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
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.bytebuddy.jar.asm.ClassReader;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.FrontendClassVisitor.EndPointData;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Represents the class dependency tree of the application.
 */
public class FrontendDependencies implements Serializable {

    private static final String LUMO = "com.vaadin.flow.theme.lumo.Lumo";

    /**
     * A wrapper for the Theme instance that use reflection for executing its
     * methods. This is needed because updaters can be executed from maven
     * plugins that use different classloaders for the running process and for
     * the project configuration.
     */
    private class ThemeWrapper implements AbstractTheme, Serializable {
        private final Object instance;

        public ThemeWrapper(Class<? extends AbstractTheme> theme) throws InstantiationException, IllegalAccessException {
            instance = theme.newInstance();
        }

        @Override
        public String getBaseUrl() {
            return invoke(instance, "getBaseUrl");
        }

        @Override
        public String getThemeUrl() {
            return invoke(instance, "getThemeUrl");
        }

        @Override
        public Map<String, String> getHtmlAttributes(String variant) {
            return invoke(instance, "getHtmlAttributes", variant);
        }

        @Override
        public List<String> getHeaderInlineContents() {
            return invoke(instance, "getHeaderInlineContents");
        }

        @Override
        public String translateUrl(String url) {
            return invoke(instance, "translateUrl", url);
        }

        @SuppressWarnings("unchecked")
        private <T> T invoke(Object instance, String methodName, Object... arguments) {
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

    private final ClassFinder finder;
    private final HashMap<String, EndPointData> endPoints = new HashMap<>();
    private final Class<?> hasElementClass;
    private final Class<?> abstractThemeClass;
    private final Class<? extends Annotation> routeClass;
    private ThemeDefinition themeDefinition;
    private AbstractTheme themeInstance;

    /**
     * Default constructor.
     *
     * @param finder
     *            the class finder used in the application
     */
    public FrontendDependencies(ClassFinder finder) {
        this.finder = finder;
        try {
            // Because of different classLoaders we need compare against class
            // references loaded by the specific class finder loader
            this.hasElementClass = finder.loadClass(HasElement.class.getName());
            this.abstractThemeClass = finder.loadClass(AbstractTheme.class.getName());
            routeClass = finder.loadClass(Route.class.getName());

            for (Class<?> route : finder.getAnnotatedClasses(routeClass) ) {
                String className = route.getName();
                EndPointData data = new EndPointData(route);
                endPoints.put(className, visitClass(className, data));

                // if this is the root level view, use its theme
                if (data.route.isEmpty() && !data.notheme) {
                    Class<? extends AbstractTheme> theme = null;
                    String variant = "";
                    if (data.theme == null) {
                        // Try Lumo if it's in classpath
                        try {
                            finder.loadClass(LUMO);
                        } catch (ClassNotFoundException ignore) { //NOSONAR
                        }
                    } else {
                        theme = finder.loadClass(data.theme);
                        variant = data.variant != null ? data.variant : "";
                    }
                    if (theme != null) {
                        themeDefinition = new ThemeDefinition(theme, variant);
                        themeInstance = new ThemeWrapper(theme);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * get all npm packages the application depends on.
     *
     * @return the set of npm packages
     */
    public Set<String> getPackages() {
        Set<String> all = new HashSet<>();
        for (FrontendClassVisitor.EndPointData r : endPoints.values()) {
            all.addAll(r.packages);
        }
        return all;
    }
    /**
     * get all ES6 modules needed for run the application.
     *
     * @return the set of JS modules
     */
    public Set<String> getModules() {
        Set<String> all = new HashSet<>();
        for (FrontendClassVisitor.EndPointData r : endPoints.values()) {
            all.addAll(r.modules);
        }
        return all;
    }
    /**
     * get all JS files used by the application.
     *
     * @return the set of JS files
     */
    public Set<String> getScripts() {
        Set<String> all = new HashSet<>();
        for (FrontendClassVisitor.EndPointData r : endPoints.values()) {
            all.addAll(r.scripts);
        }
        return all;
    }

    /**
     * get all HTML imports used in the application. It excludes imports from
     * classes that are already annotated with {@link NpmPackage} or
     * {@link JsModule}
     *
     * @return the set of HTML imports
     */
    public Set<String> getImports() {
        Set<String> all = new HashSet<>();
        for (FrontendClassVisitor.EndPointData r : endPoints.values()) {
            for (Entry<String, Set<String>> e : r.imports.entrySet()) {
                if (!r.npmClasses.contains(e.getKey())) {
                    all.addAll(e.getValue());
                }
            }
        }
        return all;
    }

    /**
     * get the {@link ThemeDefinition} of the application.
     *
     * @return the theme definition
     */
    public ThemeDefinition getThemeDefinition() {
        return themeDefinition;
    }

    /**
     * get the {@link AbstractTheme} instance used in the application.
     *
     * @return the theme instance
     */
    public AbstractTheme getTheme() {
        return themeInstance;
    }

    private boolean isVisitable(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        try {
            Class<?> clazz = finder.loadClass(className);
            // Visit only components and themes
            return hasElementClass.isAssignableFrom(clazz) || abstractThemeClass.isAssignableFrom(clazz);
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
    private EndPointData visitClass(String className, FrontendClassVisitor.EndPointData endPoint)
                    throws IOException, ClassNotFoundException {

        if (endPoint.classes.contains(className)) {
            return endPoint;
        }
        endPoint.classes.add(className);

        URL url = getUrl(className);
        if (url == null) {
            return endPoint;
        }

        FrontendClassVisitor visitor = new FrontendClassVisitor(className, endPoint);
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
