package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;


/**
 * "Builder" for {@link Reflector}
 */
public interface ReflectorController {
    /**
     * Reflector Identifier used for e.g. performing cache lookups.
     */
    String getReflectorClassIdentifier();

    /**
     * Tries to reuse/adapt the given reflector.
     *
     * @throws RuntimeException Might be thrown if reuse fails.
     */
    Reflector adaptFrom(Object reflector);

    Reflector of(MavenProject project, MojoExecution mojoExecution);
}
