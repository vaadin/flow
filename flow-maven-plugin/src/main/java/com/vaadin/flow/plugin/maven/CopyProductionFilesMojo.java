/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.common.ArtifactData;
import com.vaadin.flow.plugin.production.ProductionModeCopyStep;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.JarContentsManager;

/**
 * Goal that copies all production mode files into the
 * {@link CopyProductionFilesMojo#copyOutputDirectory} directory. Files are
 * copied from {@link CopyProductionFilesMojo#frontendWorkingDirectory}
 * directory, WebJars and regular jars, refer to {@link ProductionModeCopyStep}
 * for details.
 *
 * @since 1.0
 */
@Mojo(name = "copy-production-files", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CopyProductionFilesMojo extends FlowModeAbstractMojo {

    private static final String ADD_ON_FRONTEND = "src/main/resources/"
            + Constants.RESOURCES_FRONTEND_DEFAULT;

    /**
     * Target directory where the files that are used for the production build
     * will be copied to.
     */
    @Parameter(name = "copyOutputDirectory", defaultValue = "${project.build.directory}/frontend/", required = true)
    private File copyOutputDirectory;

    /**
     * Files and directories that should not be copied.
     */
    @Parameter(name = "excludes", defaultValue = "**/LICENSE*,**/LICENCE*,**/demo/**,**/docs/**,**/test*/**,**/.*,**/*.md,**/bower.json,**/package.json,**/package-lock.json", required = true)
    private String excludes;

    /**
     * Directory from which the files for the production mode build should be
     * copied from.
     */
    @Parameter(name = "frontendWorkingDirectory", property = "frontend.working.directory")
    private File frontendWorkingDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
    }
}