package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.List;

/**
 * Implemented by classes that want to modify the bootstrap typescript.
 */
public interface TypeScriptBootstrapModifier extends Serializable {

    /**
     * Modifies the bootstrap typescript by mutating the parameter.
     *
     * @param bootstrapTypeScript
     *            the input typescript split into lines
     */
    void modify(List<String> bootstrapTypeScript);

}
