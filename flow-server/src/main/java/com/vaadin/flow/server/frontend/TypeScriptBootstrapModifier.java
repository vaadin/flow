/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Implemented by classes that want to modify the bootstrap typescript.
 *
 * @since 24.3
 */
public interface TypeScriptBootstrapModifier extends Serializable {

    /**
     * Modifies the bootstrap typescript by mutating the parameter.
     *
     * @param bootstrapTypeScript
     *            the input typescript split into lines
     * @param productionMode
     *            true if building for production, false otherwise
     * @deprecated use {@link #modify(List, boolean, ThemeDefinition)} instead
     */
    @Deprecated
    default void modify(List<String> bootstrapTypeScript,
            boolean productionMode) {

    }

    /**
     * Modifies the bootstrap typescript by mutating the parameter.
     *
     * @param bootstrapTypeScript
     *            the input typescript split into lines
     * @param productionMode
     *            true if building for production, false otherwise
     * @param themeDefinition
     *            the theme used by the application
     * @since 24.3.3
     */
    @Deprecated
    default void modify(List<String> bootstrapTypeScript,
            boolean productionMode, ThemeDefinition themeDefinition) {
        modify(bootstrapTypeScript, productionMode);
    }

    /**
     * Modifies the bootstrap typescript by mutating the parameter.
     *
     * @param bootstrapTypeScript
     *            the input typescript split into lines
     * @param options
     *            options used by the build
     * @param frontendDependenciesScanner
     *            the frontend dependencies scanner
     * @since 24.4
     */
    default void modify(List<String> bootstrapTypeScript, Options options,
            FrontendDependenciesScanner frontendDependenciesScanner) {
        modify(bootstrapTypeScript, options.isProductionMode(),
                frontendDependenciesScanner.getThemeDefinition());
    }

    /**
     * Modifies the bootstrap typescript by mutating the parameter.
     *
     * @param bootstrapTypeScript
     *            the input typescript split into lines
     * @param options
     *            options used by the build
     * @since 24.10.5
     */
    default void modify(List<String> bootstrapTypeScript, Options options) {
        modify(bootstrapTypeScript, options,
                options.getFrontendDependenciesScanner());
    }
}
