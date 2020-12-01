/*
 * Copyright 2000-2020 Vaadin Ltd.
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

/**
 * Generate OpenAPI json file for Vaadin Endpoints.
 */
public interface TaskGenerateOpenApi extends FallibleCommand {

    /**
     * Initialize a task for generating OpenAPI spec.
     *
     * @param properties
     *            the application propperties
     * @param javaSourceFolder
     *            source paths of the project containing {@link Endpoint}
     * @param classLoader
     *            The class loader which should be used to resolved types in the
     *            source paths.
     * @param output
     *            the output path of the generated json file.
     */
    void init(File properties, File javaSourceFolder,
            ClassLoader classLoader, File output);
}
