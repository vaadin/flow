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

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.TaskGenerateFusion;
import com.vaadin.fusion.generator.MainGenerator;

import static com.vaadin.fusion.generator.ClientAPIGenerator.CUSTOM_CONNECT_CLIENT_NAME;

/**
 * Starts the generation of TS files for endpoints.
 */
public class TaskGenerateFusionImpl extends AbstractTaskFusionGenerator
        implements TaskGenerateFusion {

    private final File frontendDirectory;
    private final File openApi;
    private final File outputFolder;

    TaskGenerateFusionImpl(File applicationProperties, File openApi,
            File outputFolder, File frontendDirectory) {
        super(applicationProperties);
        Objects.requireNonNull(openApi,
                "Vaadin OpenAPI file should not be null.");
        Objects.requireNonNull(outputFolder,
                "Vaadin output folder should not be null.");
        this.openApi = openApi;
        this.outputFolder = outputFolder;
        this.frontendDirectory = frontendDirectory;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        File customConnectClient = new File(frontendDirectory,
                CUSTOM_CONNECT_CLIENT_NAME);
        String customName = customConnectClient.exists()
                ? ("../" + CUSTOM_CONNECT_CLIENT_NAME)
                : null;

        new MainGenerator(openApi, outputFolder, readApplicationProperties(),
                customName).start();
    }
}
