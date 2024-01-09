package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Implemented by classes that want to modify the bootstrap typescript.
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
     */
    default void modify(List<String> bootstrapTypeScript,
            boolean productionMode, ThemeDefinition themeDefinition) {
        modify(bootstrapTypeScript, productionMode);
    }

}
