/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.frontend;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;
import com.vaadin.fusion.Endpoint;
import com.vaadin.fusion.generator.OpenAPISpecGenerator;

/**
 * Generate OpenAPI json file for Vaadin Endpoints.
 */
public class TaskGenerateOpenAPIImpl extends AbstractTaskFusionGenerator
        implements TaskGenerateOpenAPI {

    private final File javaSourceFolder;
    private final ClassLoader classLoader;
    private final File output;

    /**
     * Create a task for generating OpenAPI spec.
     *
     * @param javaSourceFolder
     *            source paths of the project containing {@link Endpoint}
     * @param classLoader
     *            The class loader which should be used to resolved types in the
     *            source paths.
     * @param output
     *            the output path of the generated json file.
     */
    TaskGenerateOpenAPIImpl(File properties, File javaSourceFolder,
            ClassLoader classLoader, File output) {
        super(properties);
        Objects.requireNonNull(javaSourceFolder,
                "Source paths should not be null.");
        Objects.requireNonNull(output,
                "OpenAPI output file should not be null.");
        Objects.requireNonNull(classLoader, "ClassLoader should not be null.");
        this.javaSourceFolder = javaSourceFolder;
        this.classLoader = classLoader;
        this.output = output;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        OpenAPISpecGenerator openApiSpecGenerator = new OpenAPISpecGenerator(
                readApplicationProperties());
        openApiSpecGenerator.generateOpenApiSpec(
                Collections.singletonList(javaSourceFolder.toPath()),
                classLoader, output.toPath());
    }
}
