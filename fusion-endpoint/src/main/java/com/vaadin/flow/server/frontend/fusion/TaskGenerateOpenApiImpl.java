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
package com.vaadin.flow.server.frontend.fusion;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.TaskGenerateOpenApi;
import com.vaadin.flow.server.connect.generator.OpenApiSpecGenerator;

/**
 * Generate OpenAPI json file for Vaadin Endpoints.
 */
public class TaskGenerateOpenApiImpl extends AbstractTaskConnectGenerator implements TaskGenerateOpenApi {

    private File javaSourceFolder;
    private transient ClassLoader classLoader;

    @Override
    public void execute() throws ExecutionFailedException {
        OpenApiSpecGenerator openApiSpecGenerator = new OpenApiSpecGenerator(
                readApplicationProperties());
        openApiSpecGenerator.generateOpenApiSpec(
                Collections.singletonList(javaSourceFolder.toPath()),
                classLoader, outputFolder.toPath());
    }

    @Override
    public TaskGenerateOpenApi withJavaSourceFolder(File javaSourceFolder) {
        Objects.requireNonNull(javaSourceFolder,
                "Source paths should not be null.");
        this.javaSourceFolder = javaSourceFolder;
        return this;
    }

    @Override
    public TaskGenerateOpenApi withClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader,
                "ClassLoader should not be null.");
        this.javaSourceFolder = javaSourceFolder;
        return this;
    }
}
