/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Maven plugin for the Java code generation based on JSON metadata extracted
 * from webcomponents.
 *
 * @see ComponentGenerator
 * @since 1.0
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "generate")
public class ComponentGeneratorPlugin extends AbstractMojo {

    /**
     * The source directory from where the JSON files are read.
     */
    @Parameter(property = "generate.source.dir", defaultValue = "${project.basedir}/json_metadata", required = false)
    private File sourceDir;

    /**
     * The target base directory where the generated Java files will be written.
     */
    @Parameter(property = "generate.target.dir", defaultValue = "${project.build.sourceDirectory}", required = false)
    private File targetDir;

    /**
     * The file containing the license to be added as a comment in the beginning
     * of every generated Java class. It's expected that the file is encoded in
     * UTF-8.
     */
    @Parameter(property = "generate.license.file", required = false)
    private File licenseFile;

    /**
     * The base package of all generated Java classes.
     */
    @Parameter(property = "generate.base.package", defaultValue = "${project.groupId}", required = false)
    private String basePackage;

    /**
     * Flag that indicates if the plugin should fail if any Java file gets an
     * error on being generated. When <code>false</code>, the plugin will ignore
     * errors on Java code generation (but will still log them to the console).
     */
    @Parameter(property = "generate.fail.on.error", defaultValue = "true", required = false)
    private boolean failOnError;

    /**
     * The prefix to add to the name of all generated classes.
     */
    @Parameter(property = "generate.class.name.prefix", defaultValue = "", required = false)
    private String classNamePrefix;

    /**
     * Sets the frontend directory from where the files will be served. It is
     * used primarily to set the contents of the <code>@HtmlImport</code>
     * annotation. The default is <code>bower_components</code>.
     */
    @Parameter(property = "generate.dependencies.working.directory", defaultValue = "bower_components")
    private String dependenciesWorkingDirectory;

    /**
     * When <code>true</code>, all methods, getters, setters and events are
     * generated as protected. The default is <code>false</code>.
     */
    @Parameter(property = "generate.protected.methods", defaultValue = "false")
    private boolean protectedMethods;

    /**
     * When <code>true</code>, the classes are generated as abstract. The
     * default is <code>false</code>.
     */
    @Parameter(property = "generate.abstract.classes", defaultValue = "false")
    private boolean abstractClasses;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!sourceDir.isDirectory()) {
            getLog().warn(
                    "Directory not readable. Can't generate any Java classes from "
                            + sourceDir.getAbsolutePath());
            return;
        }

        File[] files = sourceDir.listFiles(
                (dir, pathName) -> pathName.toLowerCase().endsWith(".json"));
        if (files == null || files.length == 0) {
            getLog().warn(
                    "No JSON files found at " + sourceDir.getAbsolutePath());
            return;
        }

        ComponentGenerator generator = new ComponentGenerator();
        if (licenseFile != null) {
            try {
                String licenseNote = new String(
                        Files.readAllBytes(licenseFile.toPath()), UTF_8);

                generator.withLicenseNote(licenseNote);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Error reading license file at "
                                + licenseFile.getAbsolutePath(),
                        e);
            }
        }

        getLog().info("Generating " + files.length + " Java classes...");

        generator.withTargetPath(targetDir).withBasePackage(basePackage)
                .withFrontendDirectory(dependenciesWorkingDirectory)
                .withClassNamePrefix(classNamePrefix)
                .withProtectedMethods(protectedMethods)
                .withAbstractClass(abstractClasses);

        for (File file : files) {
            getLog().info("Generating class for " + file.getName() + "...");
            try {
                generator.withJsonFile(file).build();
            } catch (Exception e) {
                if (failOnError) {
                    throw new MojoExecutionException(
                            "Error generating Java source for "
                                    + file.getAbsolutePath(),
                            e);
                }
                getLog().error("Error generating Java source for "
                                + file.getAbsolutePath()
                                + ". The property \"failOnError\" is false, skipping file...",
                        e);
            }
        }
    }
}
