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
package com.vaadin.flow.server.frontend.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.DependencyTrigger;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.scanner.FrontendClassVisitor.DEV;
import static com.vaadin.flow.server.frontend.scanner.FrontendClassVisitor.VALUE;
import static com.vaadin.flow.server.frontend.scanner.FrontendClassVisitor.VERSION;

/**
 * Represents the class dependency tree of the application.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class FrontendDependencies extends AbstractDependenciesScanner {

    //@formatter:off
    private static final Pattern NOT_VISITABLE_CLASS_PATTERN = Pattern.compile("(^$|"
            + ".*(slf4j).*|"
            // #5803
            + "^(java|sun|oracle|elemental|javax|javafx|jakarta|oshi|cglib|"
            + "org\\.(apache|antlr|atmosphere|aspectj|jsoup|jboss|w3c|spring|joda|hibernate|glassfish|hsqldb|osgi|jooq|springframework|bouncycastle|snakeyaml|keycloak|flywaydb)\\b|"
            + "com\\.(helger|spring|gwt|lowagie|fasterxml|sun|nimbusds|googlecode|ibm)\\b|"
            + "ch\\.quos\\.logback\\b|"
            + "io\\.(fabric8\\.kubernetes)\\b|"
            + "net\\.(sf|bytebuddy)\\b"
            + ").*|"
            + ".*(Exception)$"
            + ")");
    //@formatter:on

    private final HashMap<String, EntryPointData> entryPoints = new LinkedHashMap<>();
    private ThemeDefinition themeDefinition;
    private AbstractTheme themeInstance;
    private final HashMap<String, String> packages = new HashMap<>();
    private final HashMap<String, String> devPackages = new HashMap<>();
    private final Map<String, ClassInfo> visitedClasses = new HashMap<>();

    private PwaConfiguration pwaConfiguration;
    private Class<? extends Annotation> routeClass;
    private Set<String> eagerRoutes = null;

    public FrontendDependencies(ClassFinder finder,
            boolean generateEmbeddableWebComponents, FeatureFlags featureFlags,
            boolean reactEnabled) {
        super(finder, featureFlags);
        log().info(
                "Scanning classes to find frontend configurations and dependencies...");
        long start = System.nanoTime();
        try {
            routeClass = getFinder().loadClass(Route.class.getName());
            computeEagerRouteConfiguration();

            collectEntryPoints(generateEmbeddableWebComponents);
            visitEntryPoints();
            computeApplicationTheme();
            if (themeDefinition != null && themeDefinition.getTheme() != null) {
                Class<? extends AbstractTheme> themeClass = themeDefinition
                        .getTheme();
                if (!visitedClasses.containsKey(themeClass.getName())) {
                    addInternalEntryPoint(themeClass);
                    visitEntryPoint(entryPoints.get(themeClass.getName()));
                }
            }
            if (reactEnabled) {
                computeReactClasses(finder);
            }
            computePackages();
            computePwaConfiguration();
            aggregateEntryPointInformation();
            long ms = (System.nanoTime() - start) / 1000000;
            log().info("Visited {} classes. Took {} ms.", visitedClasses.size(),
                    ms);
        } catch (IllegalArgumentException ex) {
            StackTraceElement[] stackTrace = ex.getStackTrace();
            if (ex.getMessage() != null
                    && ex.getMessage().startsWith("Unsupported api ")
                    && stackTrace.length > 0 && stackTrace[0].getClassName()
                            .equals("org.objectweb.asm.ClassVisitor")) {
                log().error(
                        "Invalid asm library version. Please make sure that the project does not override org.ow2.asm:asm dependency defined by Vaadin with an incompatible version.");
            }
            throw ex;
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | IOException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException(
                    "Unable to compute frontend dependencies", e);
        }
    }

    private void computeReactClasses(ClassFinder finder) throws IOException {
        // Add ReactRouterOutlet and adapter as internal so it gets added to the
        // bundle if available.
        try {
            if (finder.getResource(
                    "META-INF/resources/frontend/ReactRouterOutletElement.tsx") != null
                    && !visitedClasses.containsKey(
                            "com.vaadin.flow.component.react.ReactRouterOutlet")) {
                Class<Object> entryPointClass = finder.loadClass(
                        "com.vaadin.flow.component.react.ReactRouterOutlet");
                addInternalEntryPoint(entryPointClass);
                visitEntryPoint(entryPoints.get(entryPointClass.getName()));
            }
            if (finder.getResource(
                    "com/vaadin/flow/server/frontend/ReactAdapter.template") != null
                    && !visitedClasses.containsKey(
                            "com.vaadin.flow.component.react.ReactAdapterComponent")) {
                Class<Object> entryPointClass = finder.loadClass(
                        "com.vaadin.flow.component.react.ReactAdapterComponent");
                addInternalEntryPoint(entryPointClass);
                visitEntryPoint(entryPoints.get(entryPointClass.getName()));
            }
        } catch (ClassNotFoundException cnfe) {
            // NO-OP
        }
    }

    private void aggregateEntryPointInformation() {
        for (Entry<String, EntryPointData> entry : entryPoints.entrySet()) {
            EntryPointData entryPoint = entry.getValue();
            for (String className : entryPoint.reachableClasses) {
                ClassInfo classInfo = visitedClasses.get(className);
                entryPoint.getModules().addAll(classInfo.modules);
                entryPoint.getModulesDevelopmentOnly()
                        .addAll(classInfo.modulesDevelopmentOnly);
                entryPoint.getCss().addAll(classInfo.css);
                entryPoint.getScripts().addAll(classInfo.scripts);
                entryPoint.getScriptsDevelopmentOnly()
                        .addAll(classInfo.scriptsDevelopmentOnly);
            }
        }

    }

    Set<String> collectReachableClasses(EntryPointData entryPointData) {
        Set<String> classes = new LinkedHashSet<>();
        collectReachableClasses(entryPointData.getName(), classes);

        return classes;
    }

    private void collectReachableClasses(String name, Set<String> classes) {
        if (classes.contains(name)) {
            return;
        }

        ClassInfo visitedClass = visitedClasses.get(name);
        if (visitedClass == null) {
            if (!shouldVisit(name)) {
                return;
            }

            throw new IllegalStateException("The class " + name
                    + " is reachable but its info was not collected");
        }

        classes.add(name);
        for (String className : visitedClass.children) {
            EntryPointData entryPointData = entryPoints.get(className);
            if (entryPointData == null) {
                collectReachableClasses(className, classes);
            } else if (entryPointData.reachableClasses != null) {
                classes.addAll(entryPointData.reachableClasses);
            }
        }

    }

    private void visitEntryPoints() throws IOException {
        for (Entry<String, EntryPointData> entry : entryPoints.entrySet()) {
            visitEntryPoint(entry.getValue());
        }

    }

    private void visitEntryPoint(EntryPointData entryPoint) throws IOException {
        visitClass(entryPoint.getName(), entryPoint);

        entryPoint.reachableClasses = collectReachableClasses(entryPoint);
        if (log().isDebugEnabled()) {
            log().debug("Classes reachable from {}: {}", entryPoint.getName(),
                    entryPoint.reachableClasses);
        }

    }

    /**
     * Get all npm packages the application depends on.
     *
     * @return the set of npm packages
     */
    @Override
    public Map<String, String> getPackages() {
        return packages;
    }

    @Override
    public Map<String, String> getDevPackages() {
        return devPackages;
    }

    /**
     * Get the PWA configuration of the application.
     *
     * @return the PWA configuration
     */
    @Override
    public PwaConfiguration getPwaConfiguration() {
        return this.pwaConfiguration;
    }

    /**
     * Get all JS modules needed for run the application.
     *
     * @return list of JS modules
     */
    @Override
    public Map<ChunkInfo, List<String>> getModules() {
        LinkedHashMap<ChunkInfo, List<String>> all = new LinkedHashMap<>();
        for (EntryPointData data : entryPoints.values()) {
            all.computeIfAbsent(getChunkInfo(data), k -> new ArrayList<>())
                    .addAll(data.getModules());
        }
        return all;
    }

    /**
     * Get all JS modules needed in development mode.
     *
     * @return list of JS modules
     */
    @Override
    public Map<ChunkInfo, List<String>> getModulesDevelopment() {
        LinkedHashMap<ChunkInfo, List<String>> all = new LinkedHashMap<>();
        for (EntryPointData data : entryPoints.values()) {
            all.computeIfAbsent(getChunkInfo(data), k -> new ArrayList<>())
                    .addAll(data.getModulesDevelopmentOnly());
        }
        return all;
    }

    private ChunkInfo getChunkInfo(EntryPointData data) {
        if (data.getType() == EntryPointType.INTERNAL) {
            return ChunkInfo.GLOBAL;
        }
        return new ChunkInfo(data.getType(), data.getName(),
                data.getDependencyTriggers(), data.isEager());
    }

    /**
     * Get all the JS files used by the application.
     *
     * @return the set of JS files
     */
    @Override
    public Map<ChunkInfo, List<String>> getScripts() {
        Map<ChunkInfo, List<String>> all = new LinkedHashMap<>();
        for (EntryPointData data : entryPoints.values()) {
            all.computeIfAbsent(getChunkInfo(data), k -> new ArrayList<>())
                    .addAll(data.getScripts());
        }
        return all;
    }

    /**
     * Get all the JS files needed in development mode.
     *
     * @return the set of JS files
     */
    @Override
    public Map<ChunkInfo, List<String>> getScriptsDevelopment() {
        Map<ChunkInfo, List<String>> all = new LinkedHashMap<>();
        for (EntryPointData data : entryPoints.values()) {
            all.computeIfAbsent(getChunkInfo(data), k -> new ArrayList<>())
                    .addAll(data.getScriptsDevelopmentOnly());
        }
        return all;
    }

    /**
     * Get all the CSS files used by the application.
     *
     * @return the set of CSS files
     */
    @Override
    public Map<ChunkInfo, List<CssData>> getCss() {
        Map<ChunkInfo, List<CssData>> all = new LinkedHashMap<>();
        for (EntryPointData data : entryPoints.values()) {
            all.computeIfAbsent(getChunkInfo(data), k -> new ArrayList<>())
                    .addAll(data.getCss());
        }
        return all;
    }

    /**
     * Get all Java classes considered when looking for used dependencies.
     *
     * @return the set of JS files
     */
    @Override
    public Set<String> getClasses() {
        return visitedClasses.keySet();
    }

    /**
     * Get all entryPoints in the application.
     *
     * @return the set of JS files
     */
    public Collection<EntryPointData> getEntryPoints() {
        return entryPoints.values();
    }

    /**
     * Get the {@link ThemeDefinition} of the application.
     *
     * @return the theme definition
     */
    @Override
    public ThemeDefinition getThemeDefinition() {
        return themeDefinition;
    }

    /**
     * Get the {@link AbstractTheme} instance used in the application.
     *
     * @return the theme instance
     */
    @Override
    public AbstractTheme getTheme() {
        return themeInstance;
    }

    /**
     * Finds all the entry points in the application (e.g. annotated with
     * {@link Route}, {@link UIInitListener} instances, etc.) and create
     * {@link EntryPointData} objects.
     *
     * @param generateEmbeddableWebComponents
     *
     * @throws ClassNotFoundException
     *             if there is a problem loading a class
     */
    private void collectEntryPoints(boolean generateEmbeddableWebComponents)
            throws ClassNotFoundException {
        // Because of different classLoaders we need compare against class
        // references loaded by the specific class finder loader
        Class<? extends Annotation> triggerClass = getFinder()
                .loadClass(DependencyTrigger.class.getName());

        List<Class<?>> routeClasses = new ArrayList<>(
                getFinder().getAnnotatedClasses(routeClass));
        routeClasses.sort(this::compareEntryPoints);

        for (Class<?> route : routeClasses) {
            List<String> triggerClasses = getDependencyTriggers(route,
                    triggerClass);
            boolean eager = isEagerRoute(route);
            addEntryPoint(route, EntryPointType.ROUTE, triggerClasses, eager);
        }

        for (Class<?> initListener : getFinder().getSubTypesOf(
                getFinder().loadClass(UIInitListener.class.getName()))) {
            addInternalEntryPoint(initListener);
        }

        for (Class<?> initListener : getFinder().getSubTypesOf(getFinder()
                .loadClass(VaadinServiceInitListener.class.getName()))) {
            addInternalEntryPoint(initListener);
        }

        for (Class<?> layout : getFinder().getAnnotatedClasses(
                getFinder().loadClass(Layout.class.getName()))) {
            addInternalEntryPoint(layout);
        }

        for (Class<?> appShell : getFinder().getSubTypesOf(
                getFinder().loadClass(AppShellConfigurator.class.getName()))) {
            addInternalEntryPoint(appShell);
        }

        try {
            for (Class<?> devToolsMessageHandler : getFinder()
                    .getSubTypesOf(getFinder().loadClass(
                            "com.vaadin.base.devserver.DevToolsMessageHandler"))) {
                addInternalEntryPoint(devToolsMessageHandler);
            }
        } catch (Exception e) {
            // Will fail if dev tools is not on the classpath
        }

        for (Class<?> errorParameters : getFinder().getSubTypesOf(
                getFinder().loadClass(HasErrorParameter.class.getName()))) {
            addInternalEntryPoint(errorParameters);
        }

        // UI should always be collected as it contains 'ConnectionIndicator.js'
        addInternalEntryPoint(UI.class);

        if (generateEmbeddableWebComponents) {
            collectExporterEntrypoints(WebComponentExporter.class);
            collectExporterEntrypoints(WebComponentExporterFactory.class);
        }

    }

    private boolean isEagerRoute(Class<?> route) {
        if (this.eagerRoutes == null) {
            return defaultIsRouteEager(route);
        }

        if (this.eagerRoutes.isEmpty()) {
            // Everything is eager
            return true;
        }

        return this.eagerRoutes.contains(route.getName());
    }

    private boolean defaultIsRouteEager(Class<?> route) {
        // No annotation present, use the Flow default of making "" and
        // "login" eager
        try {
            Annotation routeAnnotation = route.getAnnotation(routeClass);
            Method valueMethod = routeClass.getMethod("value");
            String annotationRoutePath = (String) valueMethod
                    .invoke(routeAnnotation);
            String routePath = DefaultRoutePathProvider
                    .getRoutePath(annotationRoutePath, route);
            boolean eagerRoute = ("".equals(routePath)
                    || "login".equals(routePath));
            return eagerRoute;
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            log().error(
                    "Unable to read @Route annotation for " + route.getName(),
                    e);
        }

        return false;
    }

    private List<String> getDependencyTriggers(Class<?> route,
            Class triggerClass) {
        try {
            Annotation triggerAnnotation = route.getAnnotation(triggerClass);
            if (triggerAnnotation != null) {
                Method valueMethod = triggerClass.getMethod("value");
                Class<?> triggers[] = (Class<?>[]) valueMethod
                        .invoke(triggerAnnotation);
                if (triggers != null) {
                    return Stream.of(triggers).map(cls -> cls.getName())
                            .toList();
                }
            }

        } catch (SecurityException | IllegalArgumentException
                | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            log().warn(
                    "Unable to determine load mode for route class {}. Using eager.",
                    route.getName(), e);
        }
        return null;
    }

    private void addInternalEntryPoint(Class<?> entryPointClass) {
        addEntryPoint(entryPointClass, EntryPointType.INTERNAL, null, true);
    }

    private void addEntryPoint(Class<?> entryPointClass, EntryPointType type,
            List<String> dependencyTriggers, boolean eager) {
        String className = entryPointClass.getName();
        if (entryPoints.containsKey(className)) {
            return;
        }

        EntryPointData data = new EntryPointData(entryPointClass, type,
                dependencyTriggers, eager);
        entryPoints.put(className, data);
    }

    /*
     * Visit all end-points and computes the theme for the application. It fails
     * in the case that there are multiple themes for the application or in the
     * case of Theme and NoTheme found in the application.
     *
     * If no theme is found and the application has entry points, it uses lumo
     * if found in the class-path
     */
    private void computeApplicationTheme() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IOException,
            InvocationTargetException, NoSuchMethodException {

        // This really should check entry points and not all classes, but the
        // old behavior is retained.. for now..
        List<ClassInfo> classesWithTheme = entryPoints.values().stream()
                .flatMap(entryPoint -> entryPoint.reachableClasses.stream())
                .map(className -> visitedClasses.get(className))
                // consider only entry points with theme information
                .filter(this::hasThemeInfo).toList();
        Set<ThemeData> themes = classesWithTheme.stream()
                .map(classInfo -> classInfo.theme)
                // Remove duplicates by returning a set
                .collect(Collectors.toSet());

        if (themes.size() > 1) {
            String names = classesWithTheme.stream()
                    .map(data -> "found '" + getThemeDescription(data.theme)
                            + "' in '" + data.className + "'")
                    .collect(Collectors.joining("\n      "));
            throw new IllegalStateException(
                    "\n Multiple Theme configuration is not supported:\n      "
                            + names);
        }

        Class<? extends AbstractTheme> theme = null;
        String variant = "";
        String themeName = "";
        if (!themes.isEmpty()) {
            // we have a proper theme or no-theme for the app
            ThemeData themeData = themes.iterator().next();
            if (!themeData.isNotheme()) {
                String themeClass = themeData.getThemeClass();
                if (!themeData.getThemeName().isEmpty() && themeClass != null) {
                    throw new IllegalStateException(
                            "Theme name and theme class can not both be specified. "
                                    + "Theme name uses Lumo and can not be used in combination with custom theme class.");
                }
                variant = themeData.getVariant();
                if (themeClass != null) {
                    theme = getFinder().loadClass(themeClass);
                } else {
                    theme = getLumoTheme();
                    if (theme == null) {
                        throw new IllegalStateException(
                                "Lumo dependency needs to be available on the classpath when using a theme name.");
                    }
                }
                themeName = themeData.getThemeName();
            }
        }

        // theme could be null when lumo is not found or when a NoTheme found
        if (theme != null) {
            themeDefinition = new ThemeDefinition(theme, variant, themeName);
            themeInstance = new ThemeWrapper(theme);
        }
    }

    private String getThemeDescription(ThemeData theme) {
        if (theme.isNotheme()) {
            return NoTheme.class.getName();
        }
        if (theme.getThemeName() != null && !theme.getThemeName().isBlank()) {
            return theme.getThemeName();
        }
        return theme.getThemeClass();
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
                getFinder(), NpmPackage.class.getName());

        for (Class<?> component : getFinder()
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
            Set<Boolean> devs = npmPackageVisitor.getValuesForKey(VALUE,
                    dependency, DEV);
            devs.forEach(dev -> {
                if (dev != null && dev) {
                    devPackages.put(dependency, version);
                } else {
                    packages.put(dependency, version);
                }
            });
        }
    }

    private void computeEagerRouteConfiguration()
            throws ClassNotFoundException {
        FrontendAnnotatedClassVisitor loadDependenciesOnStartupVisitor = new FrontendAnnotatedClassVisitor(
                getFinder(), LoadDependenciesOnStartup.class.getName());
        Class<?> appShellConfiguratorClass = getFinder()
                .loadClass(AppShellConfigurator.class.getName());
        for (Class<?> hopefullyAppShellClass : getFinder().getAnnotatedClasses(
                LoadDependenciesOnStartup.class.getName())) {
            if (!Arrays.asList(hopefullyAppShellClass.getInterfaces())
                    .contains(appShellConfiguratorClass)) {
                throw new IllegalStateException(
                        ERROR_INVALID_LOAD_DEPENDENCIES_ANNOTATION);
            }
            loadDependenciesOnStartupVisitor
                    .visitClass(hopefullyAppShellClass.getName());
            List<Type> eagerViews = loadDependenciesOnStartupVisitor
                    .getValue("value");
            if (eagerViews != null) {
                eagerRoutes = new HashSet<>();
                for (Type eagerView : eagerViews) {
                    eagerRoutes.add(eagerView.getClassName());
                }
            }
        }
    }

    /**
     * Find the class with a {@link com.vaadin.flow.server.PWA} annotation and
     * read it into a {@link com.vaadin.flow.server.PwaConfiguration} object.
     *
     * @throws ClassNotFoundException
     */
    private void computePwaConfiguration() throws ClassNotFoundException {
        FrontendAnnotatedClassVisitor pwaVisitor = new FrontendAnnotatedClassVisitor(
                getFinder(), PWA.class.getName());
        Class<?> appShellConfiguratorClass = getFinder()
                .loadClass(AppShellConfigurator.class.getName());

        for (Class<?> hopefullyAppShellClass : getFinder()
                .getAnnotatedClasses(PWA.class.getName())) {
            if (!Arrays.asList(hopefullyAppShellClass.getInterfaces())
                    .contains(appShellConfiguratorClass)) {
                throw new IllegalStateException(ERROR_INVALID_PWA_ANNOTATION
                        + " " + hopefullyAppShellClass.getName()
                        + " does not implement "
                        + AppShellConfigurator.class.getSimpleName());
            }
            pwaVisitor.visitClass(hopefullyAppShellClass.getName());
        }

        Set<String> dependencies = pwaVisitor.getValues("name");
        if (dependencies.size() > 1) {
            throw new IllegalStateException(ERROR_INVALID_PWA_ANNOTATION
                    + " Found " + dependencies.size() + " implementations: "
                    + dependencies);
        }
        if (dependencies.isEmpty()) {
            this.pwaConfiguration = new PwaConfiguration();
            return;
        }

        String name = pwaVisitor.getValue("name");
        String shortName = pwaVisitor.getValue("shortName");
        String description = pwaVisitor.getValue("description");
        String backgroundColor = pwaVisitor.getValue("backgroundColor");
        String themeColor = pwaVisitor.getValue("themeColor");
        String iconPath = pwaVisitor.getValue("iconPath");
        String manifestPath = pwaVisitor.getValue("manifestPath");
        String offlinePath = pwaVisitor.getValue("offlinePath");
        String display = pwaVisitor.getValue("display");
        String startPath = pwaVisitor.getValue("startPath");
        List<String> offlineResources = pwaVisitor.getValue("offlineResources");
        boolean offline = pwaVisitor.getValue("offline");

        this.pwaConfiguration = new PwaConfiguration(true, name, shortName,
                description, backgroundColor, themeColor, iconPath,
                manifestPath, offlinePath, display, startPath,
                offlineResources.toArray(new String[] {}), offline);
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Visits all classes extending
     * {@link com.vaadin.flow.component.WebComponentExporter} or
     * {@link WebComponentExporterFactory} and update an {@link EntryPointData}
     * object with the info found.
     *
     * <p>
     * The limitation with {@code WebComponentExporters} is that only one theme
     * can be defined. If the more than one {@code @Theme} annotation is found
     * on the exporters, {@code IllegalStateException} will be thrown. Having
     * {@code @Theme} and {@code @NoTheme} is considered as two theme
     * annotations. However, if no theme is found, {@code Lumo} is used, if
     * available.
     *
     * @param clazz
     *            the exporter entry point class
     * @throws ClassNotFoundException
     *             if unable to load a class by class name
     */
    private void collectExporterEntrypoints(Class<?> clazz)
            throws ClassNotFoundException {
        // Because of different classLoaders we need compare against class
        // references loaded by the specific class finder loader
        Class<? extends Annotation> routeClass = getFinder()
                .loadClass(Route.class.getName());
        Class<?> exporterClass = getFinder().loadClass(clazz.getName());
        Set<? extends Class<?>> exporterClasses = getFinder()
                .getSubTypesOf(exporterClass);

        // if no exporters in the project, return
        if (exporterClasses.isEmpty()) {
            return;
        }

        HashMap<String, EntryPointData> exportedPoints = new HashMap<>();

        for (Class<?> exporter : exporterClasses) {
            String exporterClassName = exporter.getName();
            if (!entryPoints.containsKey(exporterClassName)) {
                addEntryPoint(exporter, EntryPointType.WEB_COMPONENT, null,
                        true);
            }

            if (!Modifier.isAbstract(exporter.getModifiers())) {
                Class<? extends Component> componentClass = (Class<? extends Component>) ReflectTools
                        .getGenericInterfaceType(exporter, exporterClass);
                if (componentClass != null
                        && !componentClass.isAnnotationPresent(routeClass)) {
                    addEntryPoint(componentClass, EntryPointType.WEB_COMPONENT,
                            null, true);
                }
            }
        }

        entryPoints.putAll(exportedPoints);
    }

    /**
     * Recursive method for visiting class names using bytecode inspection.
     *
     * @param className
     * @param entryPoint
     * @return
     * @throws IOException
     */
    void visitClass(String className, EntryPointData entryPoint)
            throws IOException {

        if (visitedClasses.containsKey(className) || !shouldVisit(className)) {
            return;
        }
        ClassInfo info = new ClassInfo(className);
        visitedClasses.put(className, info);

        URL url = getUrl(className);
        if (url == null) {
            return;
        }

        FrontendClassVisitor visitor = new FrontendClassVisitor(info);
        try (InputStream is = url.openStream()) {
            ClassReader cr = new ClassReader(is);
            cr.accept(visitor, ClassReader.EXPAND_FRAMES);
        } catch (Exception e) {
            log().error(
                    "Visiting class {} failed with {}.\nThis might be a broken class in the project.",
                    className, e.getMessage());
            throw e;
        }

        for (String clazz : info.children) {
            visitClass(clazz, entryPoint);
        }
    }

    protected boolean shouldVisit(String className) {
        // We should visit only those classes that might have NpmPackage,
        // JsImport, JavaScript and HtmlImport annotations, basically
        // HasElement, and AbstractTheme classes, but that prevents the usage of
        // factories. This is the reason of having just a blacklist of some
        // common name-spaces that would not have components.
        // We also exclude Feature-Flag classes
        return className != null && getFinder().shouldInspectClass(className)
                && !isDisabledExperimentalClass(className)
                && !NOT_VISITABLE_CLASS_PATTERN.matcher(className).matches();
    }

    private URL getUrl(String className) {
        return getFinder().getResource(className.replace(".", "/") + ".class");
    }

    @Override
    public String toString() {
        return entryPoints.toString();
    }

    private boolean hasThemeInfo(ClassInfo classInfo) {
        ThemeData theme = classInfo.theme;
        if (theme.getThemeClass() != null) {
            return true;
        }

        if (theme.getThemeName() != null && !theme.getThemeName().isBlank()) {
            return true;
        }

        if (!theme.getVariant().isEmpty()) {
            return true;
        }
        if (theme.isNotheme()) {
            return true;
        }

        return false;
    }

    /**
     * Ensures @Route annotated entry points are placed first in the list,
     * whereas child classes and not annotated classes go to the end of list.
     * This ensures that reachable classes for entry points are properly
     * populated for not annotated entry points.
     *
     * @param entryPoint1
     *            entry point one
     * @param entryPoint2
     *            entry point two
     * @return -1 if entry point 1 should be placed first, 0 if they are
     *         identical, 1 if entry point 2 should be placed first.
     */
    private int compareEntryPoints(Class<?> entryPoint1, Class<?> entryPoint2) {
        Annotation routeAnnotation1 = entryPoint1
                .getDeclaredAnnotation(routeClass);
        Annotation routeAnnotation2 = entryPoint2
                .getDeclaredAnnotation(routeClass);
        // classes with @Route go first in the list
        if (routeAnnotation1 != null && routeAnnotation2 == null) {
            return -1;
        }
        if (routeAnnotation1 == null && routeAnnotation2 != null) {
            return 1;
        }
        // parent classes go first in the list
        if (entryPoint1.isAssignableFrom(entryPoint2)) {
            return -1;
        }
        if (entryPoint2.isAssignableFrom(entryPoint1)) {
            return 1;
        }
        return 0;
    }
}
