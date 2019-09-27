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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Frontend dependencies scanner.
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
            if (allDependenciesScan) {
                // this dep scanner can't distinguish embeddable web component
                // frontend related annotations
                return new FullDependenciesScanner(finder);
            } else {
                return new FrontendDependencies(finder,
                        generateEmbeddableWebComponents);
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

}
