/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.net.URISyntaxException;
import java.util.Properties;

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

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Goal that generates a CycloneDX SBOM file focused on frontend dependencies.
 */
@Mojo(name = "generate-npm-sbom", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GenerateNpmBOMMojo extends FlowModeAbstractMojo {

    private static final String GROUP = "org.codehaus.mojo";
    private static final String ARTIFACT = "exec-maven-plugin";
    private static final String VERSION = "3.6.2";
    private static final String GOAL = "exec";

    /**
     * Whether to ignore errors of NPM. This might be used, if "npm install" was
     * run with "--force" or "--legacy-peer-deps".
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreNpmErrors;

    /**
     * Whether to only use the lock file, ignoring "node_modules". This means
     * the output will be based only on the few details in and the tree
     * described by the "npm-shrinkwrap.json" or "package-lock.json", rather
     * than the contents of "node_modules" directory.
     */
    @Parameter(defaultValue = "false")
    private boolean packageLockOnly;

    /**
     * Dependency types to omit from the installation tree. (can be set multiple
     * times) (choices: "dev", "optional", "peer", default: "dev" if the
     * NODE_ENV environment variable is set to "production", otherwise empty)
     */
    @Parameter(defaultValue = "dev")
    private String omit;

    /**
     * Whether to flatten the components. This means the actual nesting of node
     * packages is not represented in the SBOM result.
     */
    @Parameter(defaultValue = "false")
    private boolean flattenComponents;

    /**
     * Omit all qualifiers from PackageURLs. This causes information loss in
     * trade-off shorter PURLs, which might improve ingesting these strings.
     */
    @Parameter(defaultValue = "false")
    private boolean shortPURLs;

    /**
     * Whether to go the extra mile and make the output reproducible. This
     * requires more resources, and might result in loss of time- and
     * random-based-values.
     */
    @Parameter(defaultValue = "false")
    private boolean outputReproducible;

    /**
     * Validate resulting BOM before outputting. Validation is skipped, if
     * requirements not met.
     */
    @Parameter(defaultValue = "true")
    private boolean validate;

    /**
     * Mark as production mode.
     */
    @Parameter(defaultValue = "false")
    private boolean productionMode;

    /**
     * Type of the main component. (choices: "application", "firmware",
     * "library")
     */
    @Parameter(defaultValue = "application")
    private String mcType;

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
    protected void executeInternal()
            throws MojoExecutionException, MojoFailureException {
        InvocationRequestBuilder requestBuilder = new InvocationRequestBuilder();
        InvocationRequest request = requestBuilder.groupId(GROUP)
                .artifactId(ARTIFACT).version(VERSION).goal(GOAL)
                .createInvocationRequest();

        var properties = getProperties();
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        try {
            // the execution will fail if the directory does not exist
            createDirectoryIfNotExists();

            // node_modules dir is required
            File nodeModulesDir = new File(this.npmFolder(), "/node_modules");
            if (!nodeModulesDir.exists()) {
                logInfo("No node_modules directory found. Running npm install.");
                ClassFinder classFinder = getClassFinder();
                Lookup lookup = createLookup(classFinder);
                File jarFrontendResourcesFolder = new File(
                        new File(frontendDirectory(), FrontendUtils.GENERATED),
                        FrontendUtils.JAR_RESOURCES_FOLDER);
                Options options = new Options(lookup, npmFolder())
                        .withFrontendDirectory(frontendDirectory())
                        .withBuildDirectory(buildFolder())
                        .withJarFrontendResourcesFolder(
                                jarFrontendResourcesFolder)
                        .createMissingPackageJson(true)
                        .enableImportsUpdate(true).enablePackagesUpdate(true)
                        .withRunNpmInstall(true)
                        .withFrontendGeneratedFolder(generatedTsFolder())
                        .withNodeVersion(nodeVersion())
                        .withNodeDownloadRoot(nodeDownloadRoot())
                        .withHomeNodeExecRequired(requireHomeNodeExec())
                        .setJavaResourceFolder(javaResourceFolder())
                        .withProductionMode(productionMode)
                        .withReact(isReactEnabled())
                        .withNpmExcludeWebComponents(
                                isNpmExcludeWebComponents())
                        .withFrontendIgnoreVersionChecks(
                                isFrontendIgnoreVersionChecks());
                new NodeTasks(options).execute();
                logInfo("SBOM generation created node_modules and all needed metadata. "
                        + "If you don't need it, please run mvn vaadin:clean-frontend");
            }

            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoFailureException(
                        "Frontend SBOM generation failed.",
                        result.getExecutionException());
            }
        } catch (MavenInvocationException | ExecutionFailedException
                | URISyntaxException e) {
            throw new MojoExecutionException(
                    "Error during Frontend SBOM generation", e);
        }
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("exec.executable", "npx");
        properties.setProperty("exec.args", "@cyclonedx/cyclonedx-npm"
                + (ignoreNpmErrors ? " --ignore-npm-errors" : "")
                + (packageLockOnly ? " --package-lock-only" : "")
                + (flattenComponents ? " --flatten-components" : "")
                + (shortPURLs ? " --short-PURLs" : "")
                + (outputReproducible ? " --output-reproducible" : "")
                + (validate ? " --validate" : " --no-validate") + " --mc-type "
                + mcType + " --omit " + omit + " --spec-version " + specVersion
                + " --output-file " + outputFilePath + " --output-format "
                + outputFormat + " -- " + packageManifest);
        return properties;
    }

    private boolean createDirectoryIfNotExists() {
        int lastIndex = outputFilePath
                .lastIndexOf(FrontendUtils.isWindows() ? '\\' : '/');
        File directory = new File(outputFilePath.substring(0, lastIndex));
        return directory.exists() || directory.mkdirs();
    }
}
