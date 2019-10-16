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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.connect.generator.OpenApiSpecGenerator;

/**
 * Generate OpenAPI json file for Connect Services.
 */
public class TaskGenerateOpenApi extends AbstractTaskConnectGenerator {

    private final List<File> sourcePaths;
    private final URL[] classLoaderURLs;
    private final File output;

    /**
     * Create a task for generating OpenAPI spec.
     * 
     * @param javaSourceDirs
     *            source paths of the project containing
     *            {@link com.vaadin.flow.server.connect.VaadinService}
     * @param classLoaderURLs
     *            URL of Jars/folder which should be used to resolved types in
     *            the source paths. If this is <code>null</code>, the class
     *            loader of this class will be used.
     * @param output
     *            the output path of the generated json file.
     */

    TaskGenerateOpenApi(File properties, List<File> javaSourceDirs,
            URL[] classLoaderURLs, File output) {
        super(properties);
        Objects.requireNonNull(javaSourceDirs, "Source paths should not be null.");
        Objects.requireNonNull(output,
                "OpenAPI output file should not be null.");
        this.sourcePaths = javaSourceDirs;
        this.classLoaderURLs = classLoaderURLs;
        this.output = output;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        OpenApiSpecGenerator openApiSpecGenerator = new OpenApiSpecGenerator(
                readApplicationProperties());
        List<Path> paths = sourcePaths.stream().map(File::toPath)
                .collect(Collectors.toList());
        if (classLoaderURLs == null) {
            // when triggered by DevModeHandler, we can use the current
            // ClassLoader because they are in the same class loader with
            // sourcePaths
            openApiSpecGenerator.generateOpenApiSpec(paths,
                    this.getClass().getClassLoader(), output.toPath());
            getLogger().debug("Generate OpenAPI spec to {} using the current "
                    + "ClassLoader", output);
        } else {
            // when triggered by the maven plugin, we need to create an URL
            // class loader from the compiled class of the project and its
            // dependencies
            try (URLClassLoader classLoader = new URLClassLoader(
                    classLoaderURLs)) {
                openApiSpecGenerator.generateOpenApiSpec(paths, classLoader,
                        output.toPath());
                getLogger().debug("Generate OpenAPI spec to {} using the "
                        + "collected URLClassLoader", output);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "I/O error happens when closing project's URLClassLoader after generating OpenAPI spec.",
                        e);
            }
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(TaskGenerateOpenApi.class);
    }

}
