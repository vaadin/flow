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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Frontend dependencies scanner.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public interface FrontendDependenciesScanner extends Serializable {

    /**
     * Frontend dependencies scanner factory.
     *
     * @author Vaadin Ltd
     *
     */
    class FrontendDependenciesScannerFactory {

        /**
         * Produces scanner implementation based on {@code allDependenciesScan}
         * value.
         * <p>
         *
         * @param allDependenciesScan
         *            if {@code true} then full classpath scanning strategy is
         *            used, otherwise byte scanning strategy is produced
         * @param finder
         *            a class finder
         * @param generateEmbeddableWebComponents
         *            checks {@code WebComponentExporter} classes for
         *            dependencies if {@code true}, doesn't check otherwise
         * @param featureFlags
         *            available feature flags and their status
         * @param reactEnabled
         *            {@code true} if react is enabled, {@code true otherwise}
         * @return a scanner implementation strategy
         */
        public FrontendDependenciesScanner createScanner(
                boolean allDependenciesScan, ClassFinder finder,
                boolean generateEmbeddableWebComponents,
                FeatureFlags featureFlags, boolean reactEnabled) {
            if (allDependenciesScan) {
                // this dep scanner can't distinguish embeddable web component
                // frontend related annotations
                return new FullDependenciesScanner(finder, featureFlags,
                        reactEnabled);
            } else {
                return new FrontendDependencies(finder,
                        generateEmbeddableWebComponents, featureFlags,
                        reactEnabled);
            }
        }

    }

    /**
     * Get all npm packages the application depends on.
     *
     * @return the npm packages
     */
    Map<String, String> getPackages();

    /**
     * Get all npm packages needed only for development.
     *
     * @return the `devDependencies` packages
     */
    Map<String, String> getDevPackages();

    /**
     * Get all npm package assets for the application.
     *
     * @return the npm packages assets
     */
    Map<String, List<String>> getAssets();

    /**
     * Get all npm packages assets needed only for development.
     *
     * @return the `dev` npm package assets
     */
    Map<String, List<String>> getDevAssets();

    /**
     * Get all ES6 modules needed for run the application. Modules that are
     * theme dependencies are guaranteed to precede other modules in the result.
     *
     * @return the JS modules
     */
    Map<ChunkInfo, List<String>> getModules();

    /**
     * Get all ES6 modules needed only in development mode. Modules that are
     * theme dependencies are guaranteed to precede other modules in the result.
     *
     * @return the JS modules
     */
    Map<ChunkInfo, List<String>> getModulesDevelopment();

    /**
     * Get all the JS files used by the application.
     *
     * @return the JS files
     */
    Map<ChunkInfo, List<String>> getScripts();

    /**
     * Get all the JS files needed only in development mode.
     *
     * @return the JS files
     */
    Map<ChunkInfo, List<String>> getScriptsDevelopment();

    /**
     * Get all the CSS files used by the application.
     *
     * @return the CSS files
     */
    Map<ChunkInfo, List<CssData>> getCss();

    /**
     * Get the {@link ThemeDefinition} of the application.
     *
     * @return the theme definition
     */
    ThemeDefinition getThemeDefinition();

    /**
     * Get the {@link AbstractTheme} instance used in the application.
     *
     * @return the theme instance
     */
    AbstractTheme getTheme();

    /**
     * Get all Java classes considered when looking for used dependencies.
     *
     * @return the set of JS files
     */
    Set<String> getClasses();

    /**
     * Get the {@link PwaConfiguration} of the application.
     *
     * @return the PWA configuration
     */
    PwaConfiguration getPwaConfiguration();
}
