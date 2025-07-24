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
     * @param options
     *            options used by the build
     * @param frontendDependenciesScanner
     *            the frontend dependencies scanner
     */
    default void modify(List<String> bootstrapTypeScript, Options options,
            FrontendDependenciesScanner frontendDependenciesScanner) {
    }

}
