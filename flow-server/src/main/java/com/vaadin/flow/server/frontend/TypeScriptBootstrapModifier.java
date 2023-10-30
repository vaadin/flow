package com.vaadin.flow.server.frontend;

import java.io.Serializable;

/**
 * Implemented by classes that want to modify the bootstrap typescript.
 */
public interface TypeScriptBootstrapModifier extends Serializable {

    /**
     * Modifies the bootstrap typescript.
     *
     * @param bootstrapTypeScript
     *            the input typescript
     * @return the output typescript
     */
    String modify(String bootstrapTypeScript);

}
