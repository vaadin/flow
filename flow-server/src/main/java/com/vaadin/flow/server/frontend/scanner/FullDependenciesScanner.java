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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
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
 *
 * @author Vaadin Ltd
 * @since
 */
class FullDependenciesScanner extends AbstractDependenciesScanner {

    private static final String COULD_NOT_LOAD_ERROR_MSG = "Could not load annotation class ";

    private static final String VALUE = "value";

    private ThemeDefinition themeDefinition;
    private AbstractTheme themeInstance;
    private Set<String> classes = new HashSet<>();
    private Map<String, String> packages;
    private Set<String> scripts = new LinkedHashSet<>();
    private Set<CssData> cssData;
    private List<String> modules;

    private final Class<?> abstractTheme;

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
        try {
            abstractTheme = finder.loadClass(AbstractTheme.class.getName());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not load "
                    + AbstractTheme.class.getName() + " class", exception);
        }

        packages = discoverPackages();

        Map<String, Set<String>> themeModules = new HashMap<>();
        LinkedHashSet<String> regularModules = new LinkedHashSet<>();

        collectAnnotationValues(
                (clazz, module) -> handleModule(clazz, module, regularModules,
                        themeModules),
                JsModule.class,
                module -> invokeAnnotationMethodAsString(module, VALUE));

        collectAnnotationValues((clazz, script) -> {
            classes.add(clazz.getName());
            scripts.add(script);
        }, JavaScript.class,
                module -> invokeAnnotationMethodAsString(module, VALUE));
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

    private CssData createCssData(Annotation cssImport) {
        CssData data = new CssData();
        data.id = adaptCssValue(cssImport, "id");
        data.include = adaptCssValue(cssImport, "include");
        data.themefor = adaptCssValue(cssImport, "themeFor");
        data.value = adaptCssValue(cssImport, VALUE);
        return data;
    }

    private String adaptCssValue(Annotation cssImport, String method) {
        String value = invokeAnnotationMethodAsString(cssImport, method);
        if (value == null) {
            return value;
        }
        return value.isEmpty() ? null : value;
    }

    private Map<String, String> discoverPackages() {
        try {
            Class<? extends Annotation> loadedAnnotation = getFinder()
                    .loadClass(NpmPackage.class.getName());
            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedAnnotation);
            Map<String, String> result = new HashMap<>();
            for (Class<?> clazz : annotatedClasses) {
                classes.add(clazz.getName());
                List<? extends Annotation> packageAnnotations = annotationFinder
                        .apply(clazz, loadedAnnotation);
                packageAnnotations.forEach(pckg -> result.put(
                        invokeAnnotationMethodAsString(pckg, VALUE),
                        invokeAnnotationMethodAsString(pckg, "version")));
            }
            return result;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    COULD_NOT_LOAD_ERROR_MSG + NpmPackage.class.getName(),
                    exception);
        }
    }

    private Set<CssData> discoverCss() {
        try {
            Class<? extends Annotation> loadedAnnotation = getFinder()
                    .loadClass(CssImport.class.getName());
            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedAnnotation);
            Set<CssData> result = new LinkedHashSet<>();
            for (Class<?> clazz : annotatedClasses) {
                classes.add(clazz.getName());
                List<? extends Annotation> imports = annotationFinder
                        .apply(clazz, loadedAnnotation);
                imports.stream().forEach(imp -> result.add(createCssData(imp)));
            }
            return result;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    COULD_NOT_LOAD_ERROR_MSG + CssData.class.getName(),
                    exception);
        }
    }

    private <T extends Annotation> void collectAnnotationValues(
            BiConsumer<Class<?>, String> valueHandler, Class<T> annotationType,
            Function<Annotation, String> valueExtractor) {
        try {
            Class<? extends Annotation> loadedAnnotation = getFinder()
                    .loadClass(annotationType.getName());
            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedAnnotation);
            annotatedClasses.stream().forEach(clazz -> annotationFinder
                    .apply(clazz, loadedAnnotation).forEach(ann -> valueHandler
                            .accept(clazz, valueExtractor.apply(ann))));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    COULD_NOT_LOAD_ERROR_MSG + annotationType.getName(),
                    exception);
        }
    }

    private void discoverTheme() {
        ThemeData data = verifyTheme();

        if (data == null) {
            setupTheme(getLumoTheme(), "");
            return;
        }

        if (data.isNotheme()) {
            return;
        }

        try {
            Class<? extends AbstractTheme> theme = getFinder()
                    .loadClass(data.name);
            setupTheme(theme, data.variant);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Could not load theme class " + data.name, exception);
        }
    }

    private void setupTheme(Class<? extends AbstractTheme> theme,
            String variant) {
        if (theme != null) {
            themeDefinition = new ThemeDefinition(theme, variant);
            try {
                themeInstance = new ThemeWrapper(theme);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to create a new '"
                        + theme.getName() + "' theme instance", e);
            }
        }
    }

    private ThemeData verifyTheme() {
        try {
            Class<? extends Annotation> loadedThemeAnnotation = getFinder()
                    .loadClass(Theme.class.getName());

            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedThemeAnnotation);
            Set<ThemeData> themes = annotatedClasses.stream()
                    .flatMap(clazz -> annotationFinder
                            .apply(clazz, loadedThemeAnnotation).stream())
                    .map(theme -> new ThemeData(
                            ((Class<?>) invokeAnnotationMethod(theme, VALUE))
                                    .getName(),
                            invokeAnnotationMethodAsString(theme, "variant")))
                    .collect(Collectors.toSet());

            Class<? extends Annotation> loadedNoThemeAnnotation = getFinder()
                    .loadClass(NoTheme.class.getName());

            Set<Class<?>> notThemeClasses = getFinder()
                    .getAnnotatedClasses(loadedNoThemeAnnotation).stream()
                    .collect(Collectors.toSet());
            if (themes.size() > 1) {
                throw new IllegalStateException(
                        "Using multiple different Theme configurations is not "
                                + "supported. The list of found themes:\n"
                                + getThemesList(themes));
            }
            if (!themes.isEmpty() && !notThemeClasses.isEmpty()) {
                throw new IllegalStateException("@"
                        + Theme.class.getSimpleName() + " ("
                        + getThemesList(themes) + ") and @"
                        + NoTheme.class.getSimpleName()
                        + " annotations can't be used simultaneously.");
            }
            if (!notThemeClasses.isEmpty()) {
                return ThemeData.createNoTheme();

            }
            return themes.isEmpty() ? null : themes.iterator().next();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Could not load theme annotation class", exception);
        }
    }

    private String getThemesList(Collection<ThemeData> themes) {
        return themes
                .stream().map(theme -> "name = '" + theme.getName()
                        + "' and variant = '" + theme.getVariant() + "'")
                .collect(Collectors.joining(", "));
    }

    private void handleModule(Class<?> clazz, String module,
            Set<String> modules, Map<String, Set<String>> themeModules) {

        if (abstractTheme.isAssignableFrom(clazz)) {
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

    private String invokeAnnotationMethodAsString(Annotation target,
            String methodName) {
        Object result = invokeAnnotationMethod(target, methodName);
        return result == null ? null : result.toString();
    }

    private Object invokeAnnotationMethod(Annotation target,
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

    private Logger getLogger() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }

}
