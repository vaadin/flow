/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * Common scanner functionality.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
abstract class AbstractDependenciesScanner
        implements FrontendDependenciesScanner {

    public static final String LUMO = "com.vaadin.flow.theme.lumo.Lumo";

    protected static final String ERROR_INVALID_LOAD_DEPENDENCIES_ANNOTATION = "There can only be one @"
            + LoadDependenciesOnStartup.class.getSimpleName()
            + " annotation and it must be set on the "
            + AppShellConfigurator.class.getSimpleName() + " implementor.";
    protected static final String ERROR_INVALID_PWA_ANNOTATION = "There can only be one @PWA annotation and it must be set on the "
            + AppShellConfigurator.class.getSimpleName() + " implementor.";

    private final ClassFinder finder;
    private final FeatureFlags featureFlags;

    protected AbstractDependenciesScanner(ClassFinder finder,
            FeatureFlags featureFlags) {
        this.finder = finder;
        this.featureFlags = featureFlags;
    }

    protected final ClassFinder getFinder() {
        return finder;
    }

    protected final boolean isDisabledExperimentalClass(String className) {
        return featureFlags != null && featureFlags.getFeatures().stream()
                .anyMatch(f -> !f.isEnabled()
                        && className.equals(f.getComponentClassName()));
    }

    protected Class<? extends AbstractTheme> getLumoTheme() {
        try {
            return finder.loadClass(LUMO);
        } catch (ClassNotFoundException ignore) { // NOSONAR
            return null;
        }
    }

    /**
     * Tells whether runtime {@code @JsModule} and {@code @JavaScript} values
     * declared on the given class are added to the page at runtime. That is
     * only done for components and the app shell, which also inherit
     * annotations from the interfaces they implement. For any other class,
     * runtime URL {@code @JsModule} values are left to the bundle as before,
     * while runtime {@code @JavaScript} values are not loaded at all.
     *
     * @param clazz
     *            the class declaring the annotation, loaded through the finder
     * @return {@code true} if runtime values of the class are loaded at runtime
     */
    protected boolean loadsRuntimeDependencies(Class<?> clazz) {
        try {
            return clazz.isInterface()
                    || finder.loadClass(Component.class.getName())
                            .isAssignableFrom(clazz)
                    || finder.loadClass(AppShellConfigurator.class.getName())
                            .isAssignableFrom(clazz);
        } catch (ClassNotFoundException ignore) { // NOSONAR
            return true;
        }
    }

    protected void addValues(Map<String, List<String>> map, String key,
            List<String> values) {
        List<String> valueList = map.getOrDefault(key,
                new ArrayList<>(values.size()));
        valueList.addAll(values);
        map.put(key, valueList);
    }
}
