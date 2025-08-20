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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.scanner.FrontendClassVisitor.DEV;

/**
 * Full classpath scanner.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
class FullDependenciesScanner extends AbstractDependenciesScanner {

    private static final String COULD_NOT_LOAD_ERROR_MSG = "Could not load annotation class ";

    private static final String VALUE = "value";
    private static final String DEVELOPMENT_ONLY = "developmentOnly";
    private static final String VERSION = "version";
    private static final String ASSETS = "assets";

    private ThemeDefinition themeDefinition;
    private AbstractTheme themeInstance;
    private PwaConfiguration pwaConfiguration;
    private Set<String> classes = new HashSet<>();
    private Map<String, String> packages;
    private Map<String, String> devPackages;
    private HashMap<String, List<String>> assets = new HashMap<>();
    private HashMap<String, List<String>> devAssets = new HashMap<>();
    private List<CssData> cssData;
    private List<String> scripts;
    private List<String> scriptsDevelopment;
    private List<String> modules;
    private List<String> modulesDevelopment;

    private final Class<?> abstractTheme;

    private final SerializableBiFunction<Class<?>, Class<? extends Annotation>, List<? extends Annotation>> annotationFinder;

    /**
     * Creates a new scanner instance which discovers all dependencies in the
     * classpath.
     *
     * @param finder
     *            a class finder
     * @param featureFlags
     *            available feature flags and their status
     * @param reactEnabled
     *            true if react classes are enabled
     */
    FullDependenciesScanner(ClassFinder finder, FeatureFlags featureFlags,
            boolean reactEnabled) {
        this(finder, AnnotationReader::getAnnotationsFor, featureFlags,
                reactEnabled);
    }

    /**
     * Creates a new scanner instance which discovers all dependencies in the
     * classpath using a given annotation finder.
     *
     * @param finder
     *            a class finder
     * @param annotationFinder
     *            a strategy to discover class annotations
     * @param featureFlags
     *            available feature flags and their status
     * @param reactEnabled
     *            true if react classes are enabled
     */
    FullDependenciesScanner(ClassFinder finder,
            SerializableBiFunction<Class<?>, Class<? extends Annotation>, List<? extends Annotation>> annotationFinder,
            FeatureFlags featureFlags, boolean reactEnabled) {
        super(finder, featureFlags);

        long start = System.currentTimeMillis();
        // Wraps the finder function to provide debugging information in case of
        // failures
        this.annotationFinder = (clazz, loadedAnnotation) -> {
            try {
                return annotationFinder.apply(clazz, loadedAnnotation);
            } catch (RuntimeException exception) {
                getLogger().error("Could not read {} annotation from class {}.",
                        loadedAnnotation.getName(), clazz.getName(), exception);
                throw exception;
            }
        };

        try {
            abstractTheme = finder.loadClass(AbstractTheme.class.getName());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not load "
                    + AbstractTheme.class.getName() + " class", exception);
        }

        packages = new HashMap<>();
        devPackages = new HashMap<>();
        discoverPackages(packages, devPackages);

        LinkedHashSet<String> modulesSet = new LinkedHashSet<>();
        LinkedHashSet<String> modulesSetDevelopment = new LinkedHashSet<>();
        LinkedHashSet<String> scriptsSet = new LinkedHashSet<>();
        LinkedHashSet<String> scriptsSetDevelopment = new LinkedHashSet<>();
        discoverTheme();

        collectScripts(modulesSet, modulesSetDevelopment, JsModule.class);
        collectScripts(scriptsSet, scriptsSetDevelopment, JavaScript.class);
        cssData = discoverCss();

        if (!reactEnabled) {
            modulesSet.removeIf(
                    module -> module.contains("ReactRouterOutletElement.tsx"));
        }

        modules = new ArrayList<>(modulesSet);
        modulesDevelopment = new ArrayList<>(modulesSetDevelopment);
        scripts = new ArrayList<>(scriptsSet);
        scriptsDevelopment = new ArrayList<>(scriptsSetDevelopment);

        pwaConfiguration = discoverPwa();

        getLogger().info("Visited {} classes. Took {} ms.", getClasses().size(),
                System.currentTimeMillis() - start);
    }

    @Override
    public Map<String, String> getPackages() {
        return Collections.unmodifiableMap(packages);
    }

    @Override
    public Map<String, String> getDevPackages() {
        return Collections.unmodifiableMap(devPackages);
    }

    @Override
    public Map<String, List<String>> getAssets() {
        return Collections.unmodifiableMap(assets);
    }

    @Override
    public Map<String, List<String>> getDevAssets() {
        return Collections.unmodifiableMap(devAssets);
    }

    @Override
    public Map<ChunkInfo, List<String>> getModules() {
        return Collections.singletonMap(ChunkInfo.GLOBAL,
                Collections.unmodifiableList(modules));
    }

    @Override
    public Map<ChunkInfo, List<String>> getModulesDevelopment() {
        return Collections.singletonMap(ChunkInfo.GLOBAL,
                Collections.unmodifiableList(modulesDevelopment));
    }

    @Override
    public Map<ChunkInfo, List<String>> getScripts() {
        return Collections.singletonMap(ChunkInfo.GLOBAL,
                new ArrayList<>(scripts));
    }

    @Override
    public Map<ChunkInfo, List<String>> getScriptsDevelopment() {
        return Collections.singletonMap(ChunkInfo.GLOBAL,
                new ArrayList<>(scriptsDevelopment));
    }

    @Override
    public Map<ChunkInfo, List<CssData>> getCss() {
        return Collections.singletonMap(ChunkInfo.GLOBAL,
                new ArrayList<>(cssData));
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
    public PwaConfiguration getPwaConfiguration() {
        return pwaConfiguration;
    }

    @Override
    public Set<String> getClasses() {
        return Collections.unmodifiableSet(classes);
    }

    private CssData createCssData(Annotation cssImport) {
        String id = adaptCssValue(cssImport, "id");
        String include = adaptCssValue(cssImport, "include");
        String themeFor = adaptCssValue(cssImport, "themeFor");
        String value = adaptCssValue(cssImport, VALUE);
        return new CssData(value, id, include, themeFor);
    }

    private String adaptCssValue(Annotation cssImport, String method) {
        String value = getAnnotationValueAsString(cssImport, method);
        if (value == null) {
            return value;
        }
        return value.isEmpty() ? null : value;
    }

    private void discoverPackages(final Map<String, String> packages,
            final Map<String, String> devPackages) {
        try {
            Class<? extends Annotation> loadedAnnotation = getFinder()
                    .loadClass(NpmPackage.class.getName());
            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedAnnotation);
            Set<String> logs = new HashSet<>();
            for (Class<?> clazz : annotatedClasses) {
                classes.add(clazz.getName());
                List<? extends Annotation> packageAnnotations = annotationFinder
                        .apply(clazz, loadedAnnotation);
                packageAnnotations.forEach(annotation -> {
                    String value = getAnnotationValueAsString(annotation,
                            VALUE);
                    String version = getAnnotationValueAsString(annotation,
                            VERSION);
                    String[] npmAssets = (String[]) getAnnotationValue(
                            annotation, ASSETS);

                    boolean dev = getAnnotationValueAsBoolean(annotation, DEV);
                    logs.add(value + " " + version + " " + clazz.getName());
                    Map<String, String> result = dev ? devPackages : packages;
                    if (npmAssets.length > 0) {
                        List<String> assetsList = Arrays.asList(npmAssets);
                        if (!assetsList.isEmpty()) {
                            if (dev) {
                                addValues(devAssets, value, assetsList);
                            } else {
                                addValues(assets, value, assetsList);
                            }
                        }
                    }
                    if (result.containsKey(value)
                            && !result.get(value).equals(version)) {
                        String foundVersions = "[" + result.get(value) + ", "
                                + version + "]";
                        getLogger().warn(
                                "Multiple npm versions for {} found:  {}. First version found '{}' will be considered.",
                                value, foundVersions, result.get(value));
                    } else {
                        result.put(value, version);
                    }
                });
            }
            debug("npm dependencies", logs);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    COULD_NOT_LOAD_ERROR_MSG + NpmPackage.class.getName(),
                    exception);
        }
    }

    private List<CssData> discoverCss() {
        try {
            Class<? extends Annotation> loadedAnnotation = getFinder()
                    .loadClass(CssImport.class.getName());
            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedAnnotation);
            LinkedHashSet<CssData> result = new LinkedHashSet<>();
            for (Class<?> clazz : annotatedClasses) {
                classes.add(clazz.getName());
                List<? extends Annotation> imports = annotationFinder
                        .apply(clazz, loadedAnnotation);
                imports.stream().forEach(imp -> result.add(createCssData(imp)));
            }
            return new ArrayList<>(result);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    COULD_NOT_LOAD_ERROR_MSG + CssData.class.getName(),
                    exception);
        }
    }

    private <T extends Annotation> void collectScripts(
            LinkedHashSet<String> target, LinkedHashSet<String> targetDevOnly,
            Class<T> annotationType) {
        try {
            Set<String> logs = new HashSet<>();
            Class<? extends Annotation> loadedAnnotation = getFinder()
                    .loadClass(annotationType.getName());
            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedAnnotation);

            annotatedClasses.stream()
                    .filter(c -> !isDisabledExperimentalClass(c.getName()))
                    .forEach(clazz -> annotationFinder
                            .apply(clazz, loadedAnnotation).forEach(ann -> {
                                String value = getAnnotationValueAsString(ann,
                                        VALUE);
                                Boolean developmentOnly = getAnnotationValueAsBoolean(
                                        ann, DEVELOPMENT_ONLY);

                                classes.add(clazz.getName());

                                if (isNotActiveThemeClass(clazz)) {
                                    // The scanner will discover all theme
                                    // classes (Lumo and Material)
                                    // but should include imports only from the
                                    // active one
                                    return;
                                }

                                if (developmentOnly) {
                                    targetDevOnly.add(value);
                                } else {
                                    target.add(value);
                                }

                                logs.add(value + " " + clazz);
                            }));

            debug("@" + annotationType.getSimpleName(), logs);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    COULD_NOT_LOAD_ERROR_MSG + annotationType.getName(),
                    exception);
        }
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
        try {
            Class<? extends Annotation> loadedThemeAnnotation = getFinder()
                    .loadClass(Theme.class.getName());

            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedThemeAnnotation);
            Set<ThemeData> themes = annotatedClasses.stream()
                    .flatMap(clazz -> annotationFinder
                            .apply(clazz, loadedThemeAnnotation).stream())
                    .map(theme -> new ThemeData(
                            ((Class<?>) getAnnotationValue(theme, "themeClass"))
                                    .getName(),
                            getAnnotationValueAsString(theme, "variant"),
                            getAnnotationValueAsString(theme, VALUE)))
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
                throw new IllegalStateException(
                        "@" + Theme.class.getSimpleName() + " ("
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
        return themes.stream()
                .map(theme -> "themeClass = '" + theme.getThemeClass()
                        + "' and variant = '" + theme.getVariant()
                        + "' and name = '" + theme.getThemeName() + "'")
                .collect(Collectors.joining(", "));
    }

    private boolean isNotActiveThemeClass(Class<?> clazz) {
        if (!abstractTheme.isAssignableFrom(clazz)) {
            return false;
        }

        ThemeDefinition themeDef = getThemeDefinition();
        if (themeDef == null) {
            return true;
        }
        return !themeDef.getTheme().getName().equals(clazz.getName());
    }

    private String getAnnotationValueAsString(Annotation target,
            String methodName) {
        Object result = getAnnotationValue(target, methodName);
        return result == null ? null : result.toString();
    }

    private Boolean getAnnotationValueAsBoolean(Annotation target,
            String methodName) {
        Object result = getAnnotationValue(target, methodName);
        return result == null ? null : (Boolean) result;
    }

    private Object getAnnotationValue(Annotation target, String methodName) {
        try {
            Object value = target.getClass().getDeclaredMethod(methodName)
                    .invoke(target);
            if (value == null) {
                // Fallback to using declared default value from the annotation
                value = target.getClass().getDeclaredMethod(methodName)
                        .getDefaultValue();
            }
            return value;
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

    private PwaConfiguration discoverPwa() {
        try {
            Class<? extends Annotation> loadedPWAAnnotation = getFinder()
                    .loadClass(PWA.class.getName());

            Set<Class<?>> annotatedClasses = getFinder()
                    .getAnnotatedClasses(loadedPWAAnnotation);
            if (annotatedClasses.isEmpty()) {
                return new PwaConfiguration();
            } else if (annotatedClasses.size() != 1) {
                throw new IllegalStateException(ERROR_INVALID_PWA_ANNOTATION
                        + " Found " + annotatedClasses.size()
                        + " implementations: " + annotatedClasses);
            }

            Class<?> hopefullyAppShellClass = annotatedClasses.iterator()
                    .next();
            if (!Arrays.stream(hopefullyAppShellClass.getInterfaces())
                    .map(Class::getName).collect(Collectors.toList())
                    .contains(AppShellConfigurator.class.getName())) {
                throw new IllegalStateException(ERROR_INVALID_PWA_ANNOTATION
                        + " " + hopefullyAppShellClass.getName()
                        + " does not implement "
                        + AppShellConfigurator.class.getSimpleName());
            }

            Annotation pwa = annotationFinder
                    .apply(hopefullyAppShellClass, loadedPWAAnnotation).get(0);

            String name = getAnnotationValueAsString(pwa, "name");
            String shortName = getAnnotationValueAsString(pwa, "shortName");
            String description = getAnnotationValueAsString(pwa, "description");
            String backgroundColor = getAnnotationValueAsString(pwa,
                    "backgroundColor");
            String themeColor = getAnnotationValueAsString(pwa, "themeColor");
            String iconPath = getAnnotationValueAsString(pwa, "iconPath");
            String manifestPath = getAnnotationValueAsString(pwa,
                    "manifestPath");
            String offlinePath = getAnnotationValueAsString(pwa, "offlinePath");
            String display = getAnnotationValueAsString(pwa, "display");
            String startPath = getAnnotationValueAsString(pwa, "startPath");
            String[] offlineResources = (String[]) getAnnotationValue(pwa,
                    "offlineResources");
            boolean offline = (Boolean) getAnnotationValue(pwa, "offline");

            assert shortName != null; // required in @PWA annotation

            return new PwaConfiguration(true, name, shortName, description,
                    backgroundColor, themeColor, iconPath, manifestPath,
                    offlinePath, display, startPath, offlineResources, offline);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Could not load PWA annotation class", exception);
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

}
