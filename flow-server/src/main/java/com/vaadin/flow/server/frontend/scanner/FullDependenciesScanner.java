/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Full classpath scanner.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
class FullDependenciesScanner extends AbstractDependenciesScanner {

    private static final String VALUE = "value";

    private ThemeDefinition themeDefinition;
    private AbstractTheme themeInstance;
    private Set<String> classes = new HashSet<>();
    private Map<String, String> packages;
    private Set<String> scripts = new LinkedHashSet<>();
    private Set<CssData> cssData;
    private List<String> modules;

    private final SerializableBiFunction<Class<?>, Class<? extends Annotation>, List<? extends Annotation>> annotationFinder;

    /**
     * Creates a new scanner instance which discovers all dependencies in the
     * classpath.
     *
     * @param finder
     *            a class finder
     */
    FullDependenciesScanner(ClassFinder finder) {
        this(finder, AnnotationReader::getAnnotationsFor);
    }

    /**
     * Creates a new scanner instance which discovers all dependencies in the
     * classpath using a given annotation finder.
     *
     * @param finder
     *            a class finder
     * @param annotationFinder
     *            a strategy to discover class annotations
     */
    FullDependenciesScanner(ClassFinder finder,
            SerializableBiFunction<Class<?>, Class<? extends Annotation>, List<? extends Annotation>> annotationFinder) {
        super(finder);
        long start = System.currentTimeMillis();
        this.annotationFinder = annotationFinder;

        packages = discoverPackages();

        Map<String, Set<String>> themeModules = new HashMap<>();
        LinkedHashSet<String> regularModules = new LinkedHashSet<>();

        collectAnnotationValues(
                (clazz, module) -> handleModule(clazz, module, regularModules,
                        themeModules),
                JsModule.class, module -> ((JsModule) module).value());

        collectAnnotationValues((clazz, script) -> {
            classes.add(clazz.getName());
            scripts.add(script);
        }, JavaScript.class, module -> ((JavaScript) module).value());
        cssData = discoverCss();

        discoverTheme();

        modules = calculateModules(regularModules, themeModules);
        getLogger().info("Visited {} classes. Took {} ms.", getClasses().size(),
                System.currentTimeMillis() - start);
    }

    @Override
    public Map<String, String> getPackages() {
        return Collections.unmodifiableMap(packages);
    }

    @Override
    public List<String> getModules() {
        return Collections.unmodifiableList(modules);
    }

    @Override
    public Set<String> getScripts() {
        return Collections.unmodifiableSet(scripts);
    }

    @Override
    public Set<CssData> getCss() {
        return Collections.unmodifiableSet(cssData);
    }

    @Override
    public ThemeDefinition getThemeDefinition() {
        return themeDefinition;
    }

    @Override
    public AbstractTheme getTheme() {
        return themeInstance;
    }

    @Override
    public Set<String> getClasses() {
        return Collections.unmodifiableSet(classes);
    }

    private CssData createCssData(CssImport cssImport) {
        CssData data = new CssData();
        data.id = adaptCssValue(cssImport.id());
        data.include = adaptCssValue(cssImport.include());
        data.themefor = adaptCssValue(cssImport.themeFor());
        data.value = adaptCssValue(cssImport.value());
        return data;
    }

    private String adaptCssValue(String value) {
        if (value == null) {
            return value;
        }
        return value.isEmpty() ? null : value;
    }

    private Map<String, String> discoverPackages() {
        Set<Class<?>> annotatedClasses = getFinder()
                .getAnnotatedClasses(NpmPackage.class);
        Map<String, String> result = new HashMap<>();
        Set<String> logs = new HashSet<>();
        for (Class<?> clazz : annotatedClasses) {
            classes.add(clazz.getName());
            List<? extends Annotation> packageAnnotations = annotationFinder
                    .apply(clazz, NpmPackage.class);
            packageAnnotations.forEach(pckg -> {
                NpmPackage npmPckg = (NpmPackage) pckg;
                String value = npmPckg.value();
                String vers = npmPckg.version();
                logs.add(value + " " + vers + " " + clazz.getName());
                result.put(value, vers);
            });
        }
        debug("npm dependencies", logs);
        return result;
    }

    private Set<CssData> discoverCss() {
        Set<Class<?>> annotatedClasses = getFinder()
                .getAnnotatedClasses(CssImport.class);
        Set<CssData> result = new LinkedHashSet<>();
        for (Class<?> clazz : annotatedClasses) {
            classes.add(clazz.getName());
            List<? extends Annotation> imports = annotationFinder.apply(clazz,
                    CssImport.class);
            imports.stream()
                    .forEach(imp -> result.add(createCssData((CssImport) imp)));
        }
        return result;
    }

    private <T extends Annotation> void collectAnnotationValues(
            BiConsumer<Class<?>, String> valueHandler, Class<T> annotationType,
            Function<Annotation, String> valueExtractor) {
        Set<String> logs = new HashSet<>();
        Set<Class<?>> annotatedClasses = getFinder()
                .getAnnotatedClasses(annotationType);

        annotatedClasses.stream().forEach(clazz -> annotationFinder
                .apply(clazz, annotationType).forEach(ann -> {
                    String value = valueExtractor.apply(ann);
                    valueHandler.accept(clazz, value);
                    logs.add(value + " " + clazz);
                }));

        debug("@" + annotationType.getSimpleName(), logs);
    }

    private void debug(String label, Set<String> log) {
        if (getLogger().isDebugEnabled()) {
            log.add("\n List of " + label + " found in the project:");
            getLogger().debug(log.stream().sorted()
                    .collect(Collectors.joining("\n  - ")));
        }
    }

    private void discoverTheme() {
        ThemeData data = verifyTheme();

        if (data == null) {
            setupTheme(getLumoTheme(), "", "");
            return;
        }

        if (data.isNotheme()) {
            return;
        }

        try {
            Class<? extends AbstractTheme> theme = getFinder()
                    .loadClass(data.getThemeClass());
            setupTheme(theme, data.getVariant(), data.getThemeName());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Could not load theme class " + data.getThemeClass(),
                    exception);
        }
    }

    private void setupTheme(Class<? extends AbstractTheme> theme,
            String variant, String name) {
        if (theme != null) {
            themeDefinition = new ThemeDefinition(theme, variant, name);
            try {
                themeInstance = new ThemeWrapper(theme);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to create a new '"
                        + theme.getName() + "' theme instance", e);
            }
        }
    }

    private ThemeData verifyTheme() {
        Set<Class<?>> annotatedClasses = getFinder()
                .getAnnotatedClasses(Theme.class);
        Set<ThemeData> themes = annotatedClasses.stream()
                .flatMap(clazz -> annotationFinder
                        .apply(clazz, Theme.class).stream())
                .map(theme -> (Theme) theme)
                .map(theme -> new ThemeData(theme.value().getName(),
                        theme.variant(), theme.themeFolder()))
                .collect(Collectors.toSet());

        Set<Class<?>> notThemeClasses = getFinder()
                .getAnnotatedClasses(NoTheme.class).stream()
                .collect(Collectors.toSet());
        if (themes.size() > 1) {
            throw new IllegalStateException(
                    "Using multiple different Theme configurations is not "
                            + "supported. The list of found themes:\n"
                            + getThemesList(themes));
        }
        if (!themes.isEmpty() && !notThemeClasses.isEmpty()) {
            throw new IllegalStateException("@" + Theme.class.getSimpleName()
                    + " (" + getThemesList(themes) + ") and @"
                    + NoTheme.class.getSimpleName()
                    + " annotations can't be used simultaneously.");
        }
        if (!notThemeClasses.isEmpty()) {
            return ThemeData.createNoTheme();

        }
        return themes.isEmpty() ? null : themes.iterator().next();
    }

    private String getThemesList(Collection<ThemeData> themes) {
        return themes.stream()
                .map(theme -> "themeClass = '" + theme.getThemeClass()
                        + "' and variant = '" + theme.getVariant()
                        + "' and name = '" + theme.getThemeName() + "'")
                .collect(Collectors.joining(", "));
    }

    private void handleModule(Class<?> clazz, String module,
            Set<String> modules, Map<String, Set<String>> themeModules) {

        if (AbstractTheme.class.isAssignableFrom(clazz)) {
            Set<String> themingModules = themeModules.computeIfAbsent(
                    clazz.getName(), cl -> new LinkedHashSet<>());
            themingModules.add(module);
        } else {
            classes.add(clazz.getName());
            modules.add(module);
        }
    }

    private List<String> calculateModules(Set<String> modules,
            Map<String, Set<String>> themeModules) {
        Set<String> themingModules = themeModules
                .get(getThemeDefinition() == null ? null
                        : getThemeDefinition().getTheme().getName());
        if (themingModules == null) {
            return new ArrayList<>(modules);
        }
        if (getThemeDefinition() != null) {
            classes.add(getThemeDefinition().getTheme().getName());
        }
        // get rid of duplicate but preserve the order
        List<String> result = new ArrayList<>(
                themingModules.size() + modules.size());
        result.addAll(themingModules);
        result.addAll(modules);
        return result;
    }

    private Logger getLogger() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }

}
