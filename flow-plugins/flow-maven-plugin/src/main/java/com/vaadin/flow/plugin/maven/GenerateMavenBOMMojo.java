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
 * Goal that generates a CycloneDX SBOM file focused on backend dependencies.
 */
@Mojo(name = "generate-maven-sbom", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GenerateMavenBOMMojo extends AbstractMojo {

    protected static final String GROUP = "org.cyclonedx";
    protected static final String ARTIFACT = "cyclonedx-maven-plugin";
    protected static final String VERSION = "2.7.10";
    protected static final String GOAL = "makeAggregateBom";

    protected static final String PROJECT_TYPE = "projectType";
    protected static final String SCHEMA_VERSION = "schemaVersion";
    protected static final String INCLUDE_BOM_SERIAL_NUMBER = "includeBomSerialNumber";
    protected static final String INCLUDE_COMPILE_SCOPE = "includeCompileScope";
    protected static final String INCLUDE_PROVIDED_SCOPE = "includeProvidedScope";
    protected static final String INCLUDE_RUNTIME_SCOPE = "includeRuntimeScope";
    protected static final String INCLUDE_TEST_SCOPE = "includeTestScope";
    protected static final String INCLUDE_SYSTEM_SCOPE = "includeSystemScope";
    protected static final String INCLUDE_LICENSE_TEXT = "includeLicenseText";
    protected static final String OUTPUT_REACTOR_PROJECTS = "outputReactorProjects";
    protected static final String OUTPUT_FORMAT = "outputFormat";
    protected static final String OUTPUT_NAME = "outputName";
    protected static final String OUTPUT_DIRECTORY = "outputDirectory";
    protected static final String EXCLUDE_TYPES = "excludeTypes";
    protected static final String EXCLUDE_ARTIFACT_ID = "excludeArtifactId";
    protected static final String EXCLUDE_GROUP_ID = "excludeGroupId";
    protected static final String EXCLUDE_TEST_PROJECT = "excludeTestProject";
    protected static final String CYCLONEDX_VERBOSE = "cyclonedx.verbose";
    protected static final String VERBOSE = "verbose";
    /**
     * The component type associated to the SBOM metadata. See <a href=
     * "https://cyclonedx.org/docs/1.4/json/#metadata_component_type">CycloneDX
     * reference</a> for supported values.
     */
    @Parameter(property = PROJECT_TYPE, defaultValue = "application")
    protected String projectType;
    /**
     * The CycloneDX schema version the BOM will comply with.
     */
    @Parameter(property = SCHEMA_VERSION, defaultValue = "1.4")
    protected String schemaVersion;
    /**
     * Should the resulting BOM contain a unique serial number?
     */
    @Parameter(property = INCLUDE_BOM_SERIAL_NUMBER, defaultValue = "true")
    protected boolean includeBomSerialNumber;
    /**
     * Should compile scoped Maven dependencies be included in bom?
     */
    @Parameter(property = INCLUDE_COMPILE_SCOPE, defaultValue = "true")
    protected boolean includeCompileScope;
    /**
     * Should provided scoped Maven dependencies be included in bom?
     */
    @Parameter(property = INCLUDE_PROVIDED_SCOPE, defaultValue = "true")
    protected boolean includeProvidedScope;
    /**
     * Should runtime scoped Maven dependencies be included in bom?
     */
    @Parameter(property = INCLUDE_RUNTIME_SCOPE, defaultValue = "true")
    protected boolean includeRuntimeScope;
    /**
     * Should test scoped Maven dependencies be included in bom?
     */
    @Parameter(property = INCLUDE_TEST_SCOPE, defaultValue = "false")
    protected boolean includeTestScope;
    /**
     * Should system scoped Maven dependencies be included in bom?
     */
    @Parameter(property = INCLUDE_SYSTEM_SCOPE, defaultValue = "true")
    protected boolean includeSystemScope;
    /**
     * Should license text be included in bom?
     */
    @Parameter(property = INCLUDE_LICENSE_TEXT, defaultValue = "false")
    protected boolean includeLicenseText;
    /**
     * Should non-root reactor projects create a module-only BOM?
     */
    @Parameter(property = OUTPUT_REACTOR_PROJECTS, defaultValue = "true")
    protected boolean outputReactorProjects;
    /**
     * The CycloneDX output format that should be generated (<code>xml</code>,
     * <code>json</code> or <code>all</code>).
     */
    @Parameter(property = OUTPUT_FORMAT, defaultValue = "json")
    protected String outputFormat;
    /**
     * The CycloneDX output file name (without extension) that should be
     * generated (in {@code outputDirectory} directory).
     */
    @Parameter(property = OUTPUT_NAME, defaultValue = "bom")
    protected String outputName;
    /**
     * The output directory where to store generated CycloneDX output files.
     */
    @Parameter(property = OUTPUT_DIRECTORY, defaultValue = "${project.build.outputDirectory}/resources")
    protected String outputDirectory;
    /**
     * Excluded types.
     */
    @Parameter(property = EXCLUDE_TYPES)
    protected String[] excludeTypes;
    /**
     * Excluded reactor project (aka module) ArtifactIds from aggregate BOM.
     */
    @Parameter(property = EXCLUDE_ARTIFACT_ID)
    protected String[] excludeArtifactId;
    /**
     * Excluded reactor project (aka module) GroupIds from aggregate BOM.
     */
    @Parameter(property = EXCLUDE_GROUP_ID)
    protected String[] excludeGroupId;
    /**
     * Should reactor project (aka module) artifactId with the word "test" be
     * excluded from aggregate BOM?
     */
    @Parameter(property = EXCLUDE_TEST_PROJECT, defaultValue = "false")
    protected boolean excludeTestProject;
    /**
     * Verbose output.
     */
    @Parameter(property = VERBOSE, defaultValue = "false")
    protected boolean verbose = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        InvocationRequestBuilder requestBuilder = new InvocationRequestBuilder();
        InvocationRequest request = requestBuilder.groupId(GROUP)
                .artifactId(ARTIFACT).version(VERSION).goal(GOAL)
                .createInvocationRequest();

        Properties properties = new Properties();
        properties.setProperty(PROJECT_TYPE, projectType);
        properties.setProperty(SCHEMA_VERSION, schemaVersion);
        properties.setProperty(INCLUDE_BOM_SERIAL_NUMBER,
                String.valueOf(includeBomSerialNumber));
        properties.setProperty(INCLUDE_COMPILE_SCOPE,
                String.valueOf(includeCompileScope));
        properties.setProperty(INCLUDE_PROVIDED_SCOPE,
                String.valueOf(includeProvidedScope));
        properties.setProperty(INCLUDE_RUNTIME_SCOPE,
                String.valueOf(includeRuntimeScope));
        properties.setProperty(INCLUDE_TEST_SCOPE,
                String.valueOf(includeTestScope));
        properties.setProperty(INCLUDE_SYSTEM_SCOPE,
                String.valueOf(includeSystemScope));
        properties.setProperty(INCLUDE_LICENSE_TEXT,
                String.valueOf(includeLicenseText));
        properties.setProperty(OUTPUT_REACTOR_PROJECTS,
                String.valueOf(outputReactorProjects));
        properties.setProperty(OUTPUT_FORMAT, outputFormat);
        properties.setProperty(OUTPUT_NAME, outputName);
        properties.setProperty(OUTPUT_DIRECTORY, outputDirectory);
        properties.setProperty(EXCLUDE_TYPES, String.join(",", excludeTypes));
        properties.setProperty(EXCLUDE_ARTIFACT_ID,
                String.join(",", excludeArtifactId));
        properties.setProperty(EXCLUDE_GROUP_ID,
                String.join(",", excludeGroupId));
        properties.setProperty(EXCLUDE_TEST_PROJECT,
                String.valueOf(excludeTestProject));
        properties.setProperty(CYCLONEDX_VERBOSE, String.valueOf(verbose));
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoFailureException("Maven SBOM generation failed.",
                        result.getExecutionException());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException(
                    "Error during Maven SBOM generation", e);
        }
    }

}
