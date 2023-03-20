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
import java.util.Objects;

import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.TaskGenerateFusion;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;

/**
 * An implementation of the EndpointGeneratorTaskFactory, which creates endpoint
 * generator tasks.
 */
public class EndpointGeneratorTaskFactoryImpl
        implements EndpointGeneratorTaskFactory {

    @Override
    public TaskGenerateFusion createTaskGenerateFusion(
            File applicationProperties, File openApi, File outputFolder,
            File frontendDirectory) {
        Objects.requireNonNull(openApi,
                "Vaadin OpenAPI file should not be null.");
        Objects.requireNonNull(outputFolder,
                "Vaadin output folder should not be null.");
        return new TaskGenerateFusionImpl(applicationProperties, openApi,
                outputFolder, frontendDirectory);
    }

    @Override
    public TaskGenerateOpenAPI createTaskGenerateOpenAPI(File properties,
            File javaSourceFolder, ClassLoader classLoader, File output) {
        Objects.requireNonNull(javaSourceFolder,
                "Source paths should not be null.");
        Objects.requireNonNull(output,
                "OpenAPI output file should not be null.");
        Objects.requireNonNull(classLoader, "ClassLoader should not be null.");
        return new TaskGenerateOpenAPIImpl(properties, javaSourceFolder,
                classLoader, output);
    }

}
