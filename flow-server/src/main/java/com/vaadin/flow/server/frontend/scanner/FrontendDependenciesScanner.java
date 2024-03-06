/*
 * Copyright (C) 2000-2024 Vaadin Ltd
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
         * @return a scanner implementation strategy
         */
        public FrontendDependenciesScanner createScanner(
                boolean allDependenciesScan, ClassFinder finder,
                boolean generateEmbeddableWebComponents) {
            return createScanner(allDependenciesScan, finder,
                    generateEmbeddableWebComponents, false, null);
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
         * @param useV14Bootstrap
         *            whether we are in legacy V14 bootstrap mode
         * @param featureFlags
         *            available feature flags and their status
         * @return a scanner implementation strategy
         *
         */
        public FrontendDependenciesScanner createScanner(
                boolean allDependenciesScan, ClassFinder finder,
                boolean generateEmbeddableWebComponents,
                boolean useV14Bootstrap, FeatureFlags featureFlags) {
            return createScanner(allDependenciesScan, finder,
                    generateEmbeddableWebComponents, useV14Bootstrap,
                    featureFlags, false);
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
         * @param useV14Bootstrap
         *            whether we are in legacy V14 bootstrap mode
         * @param featureFlags
         *            available feature flags and their status
         * @param fallback
         *            whether FullDependenciesScanner is used as fallback
         * @return a scanner implementation strategy
         *
         */
        public FrontendDependenciesScanner createScanner(
                boolean allDependenciesScan, ClassFinder finder,
                boolean generateEmbeddableWebComponents,
                boolean useV14Bootstrap, FeatureFlags featureFlags,
                boolean fallback) {
            if (allDependenciesScan) {
                // this dep scanner can't distinguish embeddable web component
                // frontend related annotations
                return new FullDependenciesScanner(finder, useV14Bootstrap,
                        featureFlags, fallback);
            } else {
                return new FrontendDependencies(finder,
                        generateEmbeddableWebComponents, useV14Bootstrap,
                        featureFlags);
            }
        }
    }

    /**
     * Get all npm packages the application depends on.
     *
     * @return the set of npm packages
     */
    Map<String, String> getPackages();

    /**
     * Get all ES6 modules needed for run the application. Modules that are
     * theme dependencies are guaranteed to precede other modules in the result.
     *
     * @return list of JS modules
     */
    List<String> getModules();

    /**
     * Get all the JS files used by the application.
     *
     * @return the set of JS files
     */
    Set<String> getScripts();

    /**
     * Get all the CSS files used by the application.
     *
     * @return the set of CSS files
     */
    Set<CssData> getCss();

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
