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
package com.vaadin.flow.server.frontend.scanner;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bytebuddy.jar.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.scanner.FrontendClassVisitor.VALUE;
import static com.vaadin.flow.server.frontend.scanner.FrontendClassVisitor.VERSION;

/**
 * Represents the class dependency tree of the application.
 */
public class FrontendDependencies implements Serializable {

    public static final String LUMO = "com.vaadin.flow.theme.lumo.Lumo";

    /**
     * A wrapper for the Theme instance that use reflection for executing its
     * methods. This is needed because updaters can be executed from maven
     * plugins that use different classloaders for the running process and for
     * the project configuration.
     */
    private static class ThemeWrapper implements AbstractTheme, Serializable {
        private final Serializable instance;

        public ThemeWrapper(Class<? extends AbstractTheme> theme)
                throws InstantiationException, IllegalAccessException {
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
        private <T> T invoke(Object instance, String methodName,
                Object... arguments) {
            try {
                for (Method m : instance.getClass().getMethods()) {
                    if (m.getName().equals(methodName)) {
                        return (T) m.invoke(instance, arguments);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
            return null;
        }
    }

    private final ClassFinder finder;
    private final HashMap<String, EndPointData> endPoints = new HashMap<>();
    private ThemeDefinition themeDefinition;
    private AbstractTheme themeInstance;
    private final HashMap<String, String> packages = new HashMap<>();
    private final Set<String> visited = new HashSet<>();

    /**
     * Default Constructor.
     *
     * @param finder
     *            the class finder
     */
    public FrontendDependencies(ClassFinder finder) {
        this(finder, true);
    }

    /**
     * Secondary constructor, which allows declaring whether embeddable web
     * components should be checked for resource dependencies.
     *
     * @param finder
     *            the class finder
     * @param generateEmbeddableWebComponents
     *            {@code true} checks the
     *            {@link com.vaadin.flow.component.WebComponentExporter} classes
     *            for dependencies. {@code true} is default for
     *            {@link FrontendDependencies#FrontendDependencies(ClassFinder)}
     */
    public FrontendDependencies(ClassFinder finder,
            boolean generateEmbeddableWebComponents) {
        log().info(
                "Scanning classes to find frontend configurations and dependencies...");
        long start = System.nanoTime();
        this.finder = finder;
        try {
            computeEndpoints();
            computeApplicationTheme(endPoints);
            computePackages();
            if (generateEmbeddableWebComponents) {
                computeExporters();
            }
            long ms = (System.nanoTime() - start) / 1000000;
            log().info("Visited {} classes. Took {} ms.", visited.size(), ms);
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | IOException e) {
            throw new IllegalStateException(
                    "Unable to compute frontend dependencies", e);
        }
    }

    /**
     * Get all npm packages the application depends on.
     *
     * @return the set of npm packages
     */
    public Map<String, String> getPackages() {
        return packages;
    }

    /**
     * Get all ES6 modules needed for run the application. Modules that are
     * theme dependencies are guaranteed to precede other modules in the result.
     *
     * @return list of JS modules
     */
    public List<String> getModules() {
        // A module may appear in both data.getThemeModules and data.getModules,
        // depending on how the classes were visited, hence the LinkedHashSet
        LinkedHashSet<String> all = new LinkedHashSet<>();
        for (EndPointData data : endPoints.values()) {
            all.addAll(data.getThemeModules());
        }
        for (EndPointData data : endPoints.values()) {
            all.addAll(data.getModules());
        }
        return new ArrayList<>(all);
    }

    /**
     * Get all the JS files used by the application.
     *
     * @return the set of JS files
     */
    public Set<String> getScripts() {
        Set<String> all = new HashSet<>();
        for (EndPointData data : endPoints.values()) {
            all.addAll(data.getScripts());
        }
        return all;
    }

    /**
     * Get all the CSS files used by the application.
     *
     * @return the set of CSS files
     */
    public Set<CssData> getCss() {
        Set<CssData> all = new HashSet<>();
        for (EndPointData data : endPoints.values()) {
            all.addAll(data.getCss());
        }
        return all;
    }

    /**
     * Get all Java classes considered when looking for used dependencies.
     *
     * @return the set of JS files
     */
    public Set<String> getClasses() {
        return visited;
    }

    /**
     * Get all entryPoints in the application.
     *
     * @return the set of JS files
     */
    public Collection<EndPointData> getEndPoints() {
        return endPoints.values();
    }

    /**
     * Get the {@link ThemeDefinition} of the application.
     *
     * @return the theme definition
     */
    public ThemeDefinition getThemeDefinition() {
        return themeDefinition;
    }

    /**
     * Get the {@link AbstractTheme} instance used in the application.
     *
     * @return the theme instance
     */
    public AbstractTheme getTheme() {
        return themeInstance;
    }

    /**
     * Visit all application entry points classes (e.g. annotated with
     * {@link Route}, {@link UIInitListener} instances, etc.) and update an
     * {@link EndPointData} object with the info found.
     * <p>
     * At the same time when the root level view is visited, compute the theme
     * to use and create its instance.
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void computeEndpoints() throws ClassNotFoundException, IOException {
        // Because of different classLoaders we need compare against class
        // references loaded by the specific class finder loader
        Class<? extends Annotation> routeClass = finder
                .loadClass(Route.class.getName());
        for (Class<?> route : finder.getAnnotatedClasses(routeClass)) {
            collectEndpoints(route);
        }

        for (Class<?> initListener : finder.getSubTypesOf(
                finder.loadClass(UIInitListener.class.getName()))) {
            collectEndpoints(initListener);
        }
    }

    private void collectEndpoints(Class<?> entry) throws IOException {
        String className = entry.getName();
        EndPointData data = new EndPointData(entry);
        endPoints.put(className, visitClass(className, data, false));
    }

    // Visit all end-points and compute the theme for the application.
    // It fails in the case that there are multiple themes for the application
    // or in the
    // case of Theme and NoTheme found in the application.
    // If no theme is found, it uses lumo if found in the class-path
    private void computeApplicationTheme(
            HashMap<String, EndPointData> endPoints)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {

        // Re-visit theme related classes, because they might be skipped
        // when they where already added to the visited list during other
        // entry-point visits
        for (EndPointData endPoint : endPoints.values()) {
            if (endPoint.getLayout() != null) {
                visitClass(endPoint.getLayout(), endPoint, true);
            }
            if (endPoint.getTheme() != null) {
                visitClass(endPoint.getTheme().getName(), endPoint, true);
            }
        }

        Set<ThemeData> themes = endPoints.values().stream()
                // consider only endPoints with theme information
                .filter(data -> data.getTheme().getName() != null
                        || data.getTheme().isNotheme())
                .map(data -> data.getTheme())
                // Remove duplicates by returning a set
                .collect(Collectors.toSet());

        if (themes.size() > 1) {
            String names = String.join("\n      ",
                    endPoints.values().stream()
                            .filter(data -> data.getTheme().getName() != null
                                    || data.getTheme().isNotheme())
                            .map(data -> "found '"
                                    + (data.getTheme().isNotheme()
                                            ? NoTheme.class.getName()
                                            : data.getTheme().getName())
                                    + "' in '" + data.getName() + "'")
                            .collect(Collectors.toList()));
            throw new IllegalStateException(
                    "\n Multiple Theme configuration is not supported:\n      "
                            + names);
        }

        Class<? extends AbstractTheme> theme = null;
        String variant = "";
        if (themes.isEmpty()) {
            theme = getDefaultTheme();
        } else {
            // we have a proper theme or no-theme for the app
            ThemeData themeData = themes.iterator().next();
            if (!themeData.isNotheme()) {
                variant = themeData.getVariant();
                theme = finder.loadClass(themeData.getName());
            }

        }

        // theme could be null when lumo is not found or when a NoTheme found
        if (theme != null) {
            themeDefinition = new ThemeDefinition(theme, variant);
            themeInstance = new ThemeWrapper(theme);
        }
    }

    private Class<? extends AbstractTheme> getDefaultTheme()
            throws IOException {
        // No theme annotation found by the scanner
        final Class<? extends AbstractTheme> defaultTheme = getLumoTheme();
        // call visitClass on the default theme using the first available
        // endpoint. If not endpoint is available, default theme won't be
        // set.
        if (defaultTheme != null) {
            Optional<EndPointData> endPointData = endPoints.values().stream()
                    .findFirst();
            if (endPointData.isPresent()) {
                visitClass(defaultTheme.getName(), endPointData.get(), true);
                return defaultTheme;
            }
        }
        return null;
    }

    /**
     * Visit all classes annotated with {@link NpmPackage} and update the list
     * of dependencies and their versions.
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void computePackages() throws ClassNotFoundException, IOException {
        FrontendAnnotatedClassVisitor npmPackageVisitor = new FrontendAnnotatedClassVisitor(
                finder, NpmPackage.class.getName());

        for (Class<?> component : finder
                .getAnnotatedClasses(NpmPackage.class.getName())) {
            npmPackageVisitor.visitClass(component.getName());
        }

        Set<String> dependencies = npmPackageVisitor.getValues(VALUE);
        for (String dependency : dependencies) {
            Set<String> versions = npmPackageVisitor.getValuesForKey(VALUE,
                    dependency, VERSION);
            String version = versions.iterator().next();
            if (versions.size() > 1) {
                String foundVersions = versions.toString();
                log().warn(
                        "Multiple npm versions for {} found:  {}. First version found '{}' will be considered.",
                        dependency, foundVersions, version);
            }
            packages.put(dependency, version);
        }
    }

    private static Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }

    /**
     * Visits all classes extending
     * {@link com.vaadin.flow.component.WebComponentExporter} and update an
     * {@link EndPointData} object with the info found.
     * <p>
     * The limitation with {@code WebComponentExporters} is that only one theme
     * can be defined. If the more than one {@code @Theme} annotation is found
     * on the exporters, {@code IllegalStateException} will be thrown. Having
     * {@code @Theme} and {@code @NoTheme} is considered as two theme
     * annotations. However, if no theme is found, {@code Lumo} is used, if
     * available.
     *
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    private void computeExporters() throws ClassNotFoundException, IOException,
            IllegalAccessException, InstantiationException {
        // Because of different classLoaders we need compare against class
        // references loaded by the specific class finder loader
        Class<? extends Annotation> routeClass = finder
                .loadClass(Route.class.getName());
        Class<WebComponentExporter<? extends Component>> exporterClass = finder
                .loadClass(WebComponentExporter.class.getName());
        Set<? extends Class<? extends WebComponentExporter<? extends Component>>> exporterClasses = finder
                .getSubTypesOf(exporterClass);

        // if no exporters in the project, return
        if (exporterClasses.isEmpty()) {
            return;
        }

        HashMap<String, EndPointData> exportedPoints = new HashMap<>();

        for (Class<?> exporter : exporterClasses) {
            String exporterClassName = exporter.getName();
            EndPointData exporterData = new EndPointData(exporter);
            exportedPoints.put(exporterClassName,
                    visitClass(exporterClassName, exporterData, false));

            if (!Modifier.isAbstract(exporter.getModifiers())) {
                Class<? extends Component> componentClass = (Class<? extends Component>) ReflectTools
                        .getGenericInterfaceType(exporter, exporterClass);
                if (componentClass != null
                        && !componentClass.isAnnotationPresent(routeClass)) {
                    String componentClassName = componentClass.getName();
                    EndPointData configurationData = new EndPointData(
                            componentClass);
                    exportedPoints.put(componentClassName, visitClass(
                            componentClassName, configurationData, false));
                }
            }
        }

        computeApplicationTheme(exportedPoints);
        endPoints.putAll(exportedPoints);
    }

    /**
     * Recursive method for visiting class names using bytecode inspection.
     *
     * @param className
     * @param endPoint
     * @return
     * @throws IOException
     */
    private EndPointData visitClass(String className, EndPointData endPoint,
            boolean themeScope) throws IOException {

        // In theme scope, we want to revisit already visited classes to have
        // theme modules collected separately (in turn required for module
        // sorting, #5729)
        if (!isVisitable(className)
                || (!themeScope && endPoint.getClasses().contains(className))) {
            return endPoint;
        }
        endPoint.getClasses().add(className);

        URL url = getUrl(className);
        if (url == null) {
            return endPoint;
        }

        FrontendClassVisitor visitor = new FrontendClassVisitor(className,
                endPoint, themeScope);
        ClassReader cr = new ClassReader(url.openStream());
        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        // all classes visited by the scanner, used for performance (#5933)
        visited.add(className);

        for (String clazz : visitor.getChildren()) {
            // Since we only have an entry point for the app, it is all right to
            // skip the visit to the the same class in other end-points, because
            // we output all dependencies at once. When we implement
            // chunks, this will need to be considered.
            if (!visited.contains(clazz)) {
                visitClass(clazz, endPoint, themeScope);
            }
        }

        return endPoint;
    }

    private Class<? extends AbstractTheme> getLumoTheme() {
        try {
            return finder.loadClass(LUMO);
        } catch (ClassNotFoundException ignore) { // NOSONAR
            return null;
        }
    }

    private boolean isVisitable(String className) {
        // We should visit only those classes that might have NpmPackage,
        // JsImport, JavaScript and HtmlImport annotations, basically
        // HasElement, and AbstractTheme classes, but that prevents the usage of
        // factories. This is the reason of having just a blacklist of some
        // common name-spaces that would not have components.
        return className != null && // @formatter:off
                !className.matches(
                    "(^$|"
                    + ".*(slf4j).*|"
                    // #5803
                    + "^(java|sun|elemental|javax|org.(apache|atmosphere|jsoup|jboss|w3c|spring|joda|hibernate|glassfish|hsqldb)|com.(helger|spring|gwt|lowagie|fasterxml)|net.(sf|bytebuddy)).*|"
                    + ".*(Exception)$"
                    + ")"); // @formatter:on
    }

    private URL getUrl(String className) {
        return finder.getResource(className.replace(".", "/") + ".class");
    }

    @Override
    public String toString() {
        return endPoints.toString();
    }
}
