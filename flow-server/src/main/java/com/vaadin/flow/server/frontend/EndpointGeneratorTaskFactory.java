/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

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
     * @param options
     *            the task options
     * @return an endpoint tasks for generating TypeScript files for endpoints.
     */
    TaskGenerateEndpoint createTaskGenerateEndpoint(Options options);

    /**
     * Create a task for generating OpenAPI spec.
     *
     * @param options
     *            the task options
     * @return an endpoint task that generates open api json file.
     */
    TaskGenerateOpenAPI createTaskGenerateOpenAPI(Options options);
}
