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
import com.vaadin.flow.server.FallibleCommand;
import com.vaadin.flow.server.connect.generator.VaadinConnectTsGenerator;

/**
 * Generate the Vaadin Connect Client file.
 */
public class TaskGenerateConnectTs implements FallibleCommand {

    private final File outputFolder;
    private final File openApi;

    /**
     * Create a task for generating TS files based.
     * 
     * @param openApi
     *            openApi json file.
     * @param output
     *            the output folder.
     */
    TaskGenerateConnectTs(File openApi, File outputFolder) {
        Objects.requireNonNull(openApi,
                "Connect OpenAPI file should not be null.");
        Objects.requireNonNull(outputFolder,
                "Connect output folder should not be null.");
        
        this.openApi = openApi;
        this.outputFolder = outputFolder;
        
    }

    @Override
    public void execute() throws ExecutionFailedException {
        VaadinConnectTsGenerator.launch(openApi, outputFolder);
    }
}
