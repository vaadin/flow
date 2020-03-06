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
import java.util.Objects;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.connect.generator.VaadinConnectClientGenerator;
import com.vaadin.flow.server.connect.generator.VaadinConnectTsGenerator;

import static com.vaadin.flow.server.connect.generator.VaadinConnectClientGenerator.CONNECT_CLIENT_NAME;

/**
 * Generate the Vaadin Connect TS files for endpoints, and the Client API file.
 */
public class TaskGenerateConnect extends AbstractTaskConnectGenerator {

    private final File outputFolder;
    private final File openApi;
    private final File connectClientFile;

    /**
     * Create a task for generating TS files based.
     *
     * @param applicationProperties
     *            application properties file.
     * @param openApi
     *            openApi json file.
     * @param output
     *            the output folder.
     */
    TaskGenerateConnect(File applicationProperties, File openApi,
            File outputFolder) {
        super(applicationProperties);
        Objects.requireNonNull(openApi,
                "Connect OpenAPI file should not be null.");
        Objects.requireNonNull(outputFolder,
                "Connect output folder should not be null.");
        this.openApi = openApi;
        this.outputFolder = outputFolder;
        this.connectClientFile = new File(outputFolder, CONNECT_CLIENT_NAME);
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (VaadinConnectTsGenerator.launch(openApi, outputFolder)) {
            new VaadinConnectClientGenerator(readApplicationProperties())
                    .generateVaadinConnectClientFile(
                            connectClientFile.toPath());
        }
    }
}
