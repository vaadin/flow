/*
 * Copyright (C) 2000-2026 Vaadin Ltd
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
 * @since 9.0
 */
public class TaskGenerateViteDevMode extends AbstractTaskClientGenerator {

    private Options options;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateViteDevMode(Options options) {
        this.options = options;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(
                new File(options.getFrontendDirectory(),
                        FrontendUtils.GENERATED),
                FrontendUtils.VITE_DEVMODE_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        return options.isFrontendHotdeploy() || options.isBundleBuild();
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream devModeStream = getClass()
                .getResourceAsStream(FrontendUtils.VITE_DEVMODE_TS)) {
            return IOUtils.toString(devModeStream, UTF_8);
        }
    }

}
