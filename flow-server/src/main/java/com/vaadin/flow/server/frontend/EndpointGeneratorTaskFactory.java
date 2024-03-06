/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;

/**
 * A factory for creating Vaadin Endpoint generator tasks.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 6.0
 */
public interface EndpointGeneratorTaskFactory {

    /**
     * Create a task for generating TS files based.
     *
     * @param applicationProperties
     *            application properties file.
     * @param openApi
     *            openApi json file. not {@code null}
     * @param outputFolder
     *            the output folder. not {@code null}
     * @param frontendDirectory
     *            the frontend folder.
     * @return an endpoint tasks for generating TypeScript files for endpoints.
     */
    TaskGenerateEndpoint createTaskGenerateEndpoint(File applicationProperties,
            File openApi, File outputFolder, File frontendDirectory);

    /**
     * Create a task for generating OpenAPI spec.
     *
     * @param properties
     *            application properties file.
     * @param javaSourceFolder
     *            source paths of the project containing Vaadin Endpoint. not
     *            {@code null}
     * @param classLoader
     *            The class loader which should be used to resolved types in the
     *            source paths. not {@code null}
     * @param output
     *            the output path of the generated json file. not {@code null}
     * @return an endpoint task that generates open api json file.
     */
    TaskGenerateOpenAPI createTaskGenerateOpenAPI(File properties,
            File javaSourceFolder, ClassLoader classLoader, File output);
}
