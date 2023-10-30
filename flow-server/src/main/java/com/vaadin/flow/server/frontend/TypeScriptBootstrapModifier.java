package com.vaadin.flow.server.frontend;

/**
 * Implemented by classes that want to modify the bootstrap typescript.
 */
public interface TypeScriptBootstrapModifier {

    /**
     * Modifies the bootstrap typescript.
     *
     * @param bootstrapTypeScript
     *            the input typescript
     * @return the output typescript
     */
    String modify(String bootstrapTypeScript);

}
