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

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.connect.generator.OpenApiSpecGenerator;

/**
 * Generate OpenAPI json file for Connect Services.
 */
public class TaskGenerateOpenAPI extends AbstractTaskConnectGenerator {

    private final List<Path> sourcePaths;
    private final URL[] classLoaderURLs;
    private final Path output;

    /**
     * Create a task for generating OpenAPI spec.
     * 
     * @param sourcePaths
     *            source paths of the project containing
     *            {@link com.vaadin.flow.server.connect.VaadinService}
     * @param classLoaderURLs
     *            URL of Jars/folder which should be used to resolved types in
     *            the source paths.
     * @param output
     *            the output path of the generated json file.
     */
    TaskGenerateOpenAPI(File properties, List<Path> sourcePaths,
            URL[] classLoaderURLs, Path output) {
        super(properties);
        this.sourcePaths = sourcePaths;
        this.classLoaderURLs = classLoaderURLs;
        this.output = output;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        OpenApiSpecGenerator openApiSpecGenerator = new OpenApiSpecGenerator(
                readApplicationProperties());
        if (classLoaderURLs == null) {
            // when running in DevMode, we can use the current ClassLoader
            openApiSpecGenerator.generateOpenApiSpec(sourcePaths,
                    this.getClass().getClassLoader(), output);
        } else {
            try (URLClassLoader classLoader = new URLClassLoader(
                    classLoaderURLs)) {
                openApiSpecGenerator.generateOpenApiSpec(sourcePaths,
                        classLoader, output);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "I/O error happens when closing project's URLClassLoader after generating OpenAPI spec.",
                        e);
            }
        }
    }

}
