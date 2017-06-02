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
package com.vaadin.generator;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Maven plugin for the Java code generation based on JSON metadata extracted
 * from webcomponents.
 * 
 * @see ComponentGenerator
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "generate")
public class ComponentGeneratorPlugin extends AbstractMojo {

    @Parameter(property = "generate.source.dir", defaultValue = "${project.basedir}/json_metadata", required = false)
    private File sourceDir;

    @Parameter(property = "generate.target.dir", defaultValue = "${project.build.sourceDirectory}", required = false)
    private File targetDir;

    @Parameter(property = "generate.base.package", defaultValue = "${project.groupId}", required = false)
    private String basePackage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ComponentGenerator generator = new ComponentGenerator();
            if (!sourceDir.isDirectory()) {
                getLog().warn(
                        "Directory not readable. Can't generate any Java classes from "
                                + sourceDir.getName());
                return;
            }

            File[] files = sourceDir.listFiles((dir, pathName) -> pathName
                    .toLowerCase().endsWith(".json"));
            if (files.length == 0) {
                getLog().warn("No JSON files found at " + sourceDir.getName());
                return;
            }

            getLog().info("Generating " + files.length + " Java classes...");

            for (File file : files) {
                getLog().info("Generating class for " + file.getName() + "...");
                generator.generateClass(file, targetDir, basePackage);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error generating Java sources",
                    e);
        }
    }
}
