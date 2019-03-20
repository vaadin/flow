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
import com.vaadin.flow.server.Constants;

/**
 * Goal that copies all JavaScript files into the {@link
 * CopyFrontendFilesMojo#frontendWorkingDirectory}
 * directory from regular jars, refer to {@link ProductionModeCopyStep} for
 * details.
 */
@Mojo(name = "copy-frontend-files", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CopyFrontendFilesMojo extends AbstractMojo {

    /**
     * Files and directories that should be copied. Default is only .js files.
     */
    @Parameter(name = "includes", defaultValue = "**/*.js", required = true)
    private String includes;

    /**
     * Directory where the files to be transpiled should be copied to.
     */
    @Parameter(name = "frontendWorkingDirectory", property = "frontend.working.directory", defaultValue = "frontend")
    private File frontendWorkingDirectory;

    /**
     * The path inside the JAR to copy frontend files matching the {@link CopyFrontendFilesMojo#includes} filter.
     *
     * Default is 'META-INF/frontend'
     */
    @Parameter(name = "resourcePath", property = "resource.path", defaultValue = "META-INF/frontend")
    private String resourcePath;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        // Do nothing when not in bower mode
        if (!Boolean.getBoolean("vaadin." + Constants.SERVLET_PARAMETER_BOWER_MODE)) {
            return;
        }

        List<ArtifactData> projectArtifacts = project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .map(artifact -> new ArtifactData(artifact.getFile(),
                        artifact.getArtifactId(), artifact.getVersion()))
                .collect(Collectors.toList());

        new ProductionModeCopyStep(new JarContentsManager(), projectArtifacts)
                .copyFrontendJavaScriptFiles(frontendWorkingDirectory, includes,
                        resourcePath);
    }
}
