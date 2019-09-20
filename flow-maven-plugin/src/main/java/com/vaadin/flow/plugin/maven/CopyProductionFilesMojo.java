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
        super.execute();

        // Do nothing when not in compatibility mode
        if (!compatibility) {
            getLog().info(
                    "Skipped `copy-production-files` goal because compatibility mode is not set.");
            return;
        }

        List<ArtifactData> projectArtifacts = project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .map(artifact -> new ArtifactData(artifact.getFile(),
                        artifact.getArtifactId(), artifact.getVersion()))
                .collect(Collectors.toList());

        if (frontendWorkingDirectory == null) {
            // No directory given, try to find from common locations
            final List<String> potentialFrontEndDirectories = Arrays.asList(
                    "src/main/webapp/frontend", ADD_ON_FRONTEND,
                    "src/main/resources/META-INF/resources/frontend",
                    "src/main/resources/public/frontend",
                    "src/main/resources/static/frontend",
                    "src/main/resources/resources/frontend");
            for (String dir : potentialFrontEndDirectories) {
                File directory = new File(project.getBasedir(), dir);
                if (directory.exists()) {
                    frontendWorkingDirectory = directory;
                    break;
                }
            }
        }

        new ProductionModeCopyStep(new JarContentsManager(), projectArtifacts)
                .copyWebApplicationFiles(copyOutputDirectory,
                        frontendWorkingDirectory, excludes);
    }

    @Override
    boolean isDefaultCompatibility() {
        return true;
    }

}
