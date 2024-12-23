package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugin.Mojo;

import java.util.Set;

public interface Reflector {
    /**
     * Copies the mojo to/with the isolated classloader.
     */
    Mojo createIsolatedMojo(FlowModeAbstractMojo sourceMojo,
            Set<String> ignoredFields) throws Exception;

    ReflectorIsolatedClassLoader getIsolatedClassLoader();
}
