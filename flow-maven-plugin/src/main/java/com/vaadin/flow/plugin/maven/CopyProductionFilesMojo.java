/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.common.ArtifactData;
import com.vaadin.flow.plugin.common.JarContentsManager;
import com.vaadin.flow.plugin.production.ProductionModeCopyStep;

/**
 * Goal that copies all production mode files into the
 * {@link CopyProductionFilesMojo#copyOutputDirectory} directory. Files are
 * copied from {@link CopyProductionFilesMojo#frontendWorkingDirectory}
 * directory, WebJars and regular jars, refer to {@link ProductionModeCopyStep}
 * for details.
 */
@Mojo(name = "copy-production-files", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CopyProductionFilesMojo extends AbstractMojo {
    
    /**
     * The directory where frontend files are copied for further processing.
     */
    @Parameter(name = "copyOutputDirectory", defaultValue = "${project.build.directory}/frontend/", required = true)
    private File copyOutputDirectory;

    @Parameter(name = "excludes", defaultValue = "**/LICENSE*,**/LICENCE*,**/demo/**,**/docs/**,**/test*/**,**/.*,**/*.md,**/bower.json,**/package.json,**/package-lock.json", required = true)
    private String excludes;

    /**
     * The directory where your source files, such as Vaadin templates, are stored in this project.
     */
    @Parameter(name = "frontendWorkingDirectory", property = "frontend.working.directory", defaultValue = "${project.basedir}/src/main/webapp/frontend/", required = true)
    private File frontendWorkingDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        List<ArtifactData> projectArtifacts = project.getArtifacts().stream()
                .map(artifact -> new ArtifactData(artifact.getFile(),
                        artifact.getArtifactId(), artifact.getVersion()))
                .collect(Collectors.toList());
        new ProductionModeCopyStep(new JarContentsManager(), projectArtifacts)
                .copyWebApplicationFiles(copyOutputDirectory,
                        frontendWorkingDirectory, excludes);
    }
}
