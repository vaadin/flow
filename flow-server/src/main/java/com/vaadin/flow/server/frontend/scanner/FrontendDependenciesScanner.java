/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Frontend dependencies scanner.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.1
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
         * @return a scanner implementation strategy
         * @deprecated Use
         *             {@link FrontendDependenciesScannerFactory#createScanner(boolean, ClassFinder, boolean, FeatureFlags, boolean)}
         *             instead.
         */
        @Deprecated
        public FrontendDependenciesScanner createScanner(
                boolean allDependenciesScan, ClassFinder finder,
                boolean generateEmbeddableWebComponents) {
            return createScanner(allDependenciesScan, finder,
                    generateEmbeddableWebComponents, null);
        }

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
         * @return a scanner implementation strategy
         *
         * @deprecated Use
         *             {@link FrontendDependenciesScannerFactory#createScanner(boolean, ClassFinder, boolean, FeatureFlags, boolean)}
         *             instead.
         */
        @Deprecated
        public FrontendDependenciesScanner createScanner(
                boolean allDependenciesScan, ClassFinder finder,
                boolean generateEmbeddableWebComponents,
                FeatureFlags featureFlags) {
            return createScanner(allDependenciesScan, finder,
                    generateEmbeddableWebComponents, featureFlags, true);
        }

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

        /**
         * Produces scanner implementation based on the given Options object.
         *
         * @param options
         *            Options to build the scanner from
         * @return a scanner implementation strategy
         * @deprecated Use
         *             {@link FrontendDependenciesScannerFactory#createScanner(boolean, ClassFinder, boolean, FeatureFlags, boolean)}
         *             instead.
         */
        @Deprecated
        public FrontendDependenciesScanner createScanner(Options options) {
            boolean reactEnabled = options.isReactEnabled() && FrontendUtils
                    .isReactRouterRequired(options.getFrontendDirectory());
            return createScanner(!options.isUseByteCodeScanner(),
                    options.getClassFinder(),
                    options.isGenerateEmbeddableWebComponents(),
                    options.getFeatureFlags(), reactEnabled);
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
     * @since 24.3
     */
    Map<String, String> getDevPackages();

    /**
     * Get all npm package assets for the application.
     *
     * @return the npm packages assets
     * @since 24.9
     */
    Map<String, List<String>> getAssets();

    /**
     * Get all npm packages assets needed only for development.
     *
     * @return the `dev` npm package assets
     * @since 24.9
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
     * @since 24.2
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
     * @since 24.2
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
     * @since 6.0
     */
    PwaConfiguration getPwaConfiguration();
}
