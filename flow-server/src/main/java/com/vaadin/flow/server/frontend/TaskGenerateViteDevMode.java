/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Generate <code>vite-devmode.ts</code> if it is missing in frontend/generated
 * folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public class TaskGenerateViteDevMode extends AbstractTaskClientGenerator {

    private final File frontendDirectory;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param generatedImports
     *            the flow generated imports file to include in the
     *            <code>index.js</code>
     * @param outputDirectory
     *            the build output directory
     */
    TaskGenerateViteDevMode(File frontendDirectory) {
        this.frontendDirectory = frontendDirectory;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(new File(frontendDirectory, FrontendUtils.GENERATED),
                FrontendUtils.VITE_DEVMODE_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream devModeStream = getClass()
                .getResourceAsStream(FrontendUtils.VITE_DEVMODE_TS)) {
            return IOUtils.toString(devModeStream, UTF_8);
        }
    }

}
