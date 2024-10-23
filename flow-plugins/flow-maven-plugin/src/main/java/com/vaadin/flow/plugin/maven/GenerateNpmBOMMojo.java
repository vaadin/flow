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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal that generates a CycloneDX SBOM file focused on frontend dependencies.
 */
@Mojo(name = "generate-npm-sbom", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GenerateNpmBOMMojo extends FlowModeAbstractMojo {

    private static final String GROUP = "org.codehaus.mojo";
    private static final String ARTIFACT = "exec-maven-plugin";
    private static final String VERSION = "1.3.2";
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
    protected Class<?> taskClass(Reflector reflector)
            throws ClassNotFoundException {
        return reflector.loadClass(GenerateNpmBOMTask.class.getName());
    }

}
