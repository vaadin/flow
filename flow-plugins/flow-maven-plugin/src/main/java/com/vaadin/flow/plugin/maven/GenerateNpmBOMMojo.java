/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * Goal that generates a CycloneDX SBOM file focused on frontend dependencies.
 */
@Mojo(name = "generate-npm-sbom", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateNpmBOMMojo extends AbstractMojo {

    private static final String GROUP = "org.codehaus.mojo";
    private static final String ARTIFACT = "exec-maven-plugin";
    private static final String VERSION = "1.3.2";
    private static final String GOAL = "exec";

    /**
     * The CycloneDX output format that should be generated (<code>xml</code>,
     * <code>json</code> or <code>all</code>).
     */
    @Parameter(defaultValue = "json")
    private String outputFormat;

    /**
     * The path to the file to be generated.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/resources/bom-npm.json")
    private String outputFilePath;

    /**
     * The path to the package.json file to read.
     */
    @Parameter(defaultValue = "./package.json")
    private String packageManifest;

    @Parameter(defaultValue = "1.4")
    private String specVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        InvocationRequestBuilder requestBuilder = new InvocationRequestBuilder();
        InvocationRequest request = requestBuilder.groupId(GROUP)
                .artifactId(ARTIFACT).version(VERSION).goal(GOAL)
                .createInvocationRequest();

        Properties properties = new Properties();
        properties.setProperty("exec.executable", "npx");
        properties.setProperty("exec.args",
                "@cyclonedx/cyclonedx-npm --spec-version " + specVersion
                        + " --output-file " + outputFilePath
                        + " --output-format " + outputFormat + " -- "
                        + packageManifest);
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        try {
            // the execution will fail if the directory does not exist
            createDirectoryIfNotExists();
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Frontend SBOM generation failed.");
            }
        } catch (MavenInvocationException e) {
            throw new RuntimeException("Error during Frontend SBOM generation",
                    e);
        }
    }

    private boolean createDirectoryIfNotExists() {
        int lastIndex = outputFilePath.lastIndexOf('/');
        File directory = new File(outputFilePath.substring(0, lastIndex));
        return directory.exists() || directory.mkdirs();
    }
}
