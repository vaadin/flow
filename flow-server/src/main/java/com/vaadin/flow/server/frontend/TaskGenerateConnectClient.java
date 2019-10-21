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
import java.util.Objects;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.connect.generator.VaadinConnectClientGenerator;

/**
 * Generate the Vaadin Connect Client file.
 */
public class TaskGenerateConnectClient extends AbstractTaskConnectGenerator {

    private final File output;

    /**
     * Create a task for generating OpenAPI spec.
     * 
     * @param properties
     *            properties file.
     * @param output
     *            the geneated file.
     */
    TaskGenerateConnectClient(File properties, File output) {
        super(properties);
        Objects.requireNonNull(output,
                "Connect Client output file should not be null.");
        this.output = output;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        VaadinConnectClientGenerator generator = new VaadinConnectClientGenerator(readApplicationProperties());
        generator.generateVaadinConnectClientFile(output.toPath());
    }
}
