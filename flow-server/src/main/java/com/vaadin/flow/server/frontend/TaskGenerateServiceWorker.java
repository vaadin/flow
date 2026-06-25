/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>index.html</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateServiceWorker extends AbstractTaskClientGenerator {

    private Options options;

    /**
     * Create a task to generate <code>sw.ts</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateServiceWorker(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream swStream = getClass()
                .getResourceAsStream(SERVICE_WORKER_SRC)) {
            return IOUtils.toString(swStream, UTF_8);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return new File(options.getBuildDirectory(), SERVICE_WORKER_SRC);
    }

    @Override
    protected boolean shouldGenerate() {
        File serviceWorker = new File(options.getFrontendDirectory(),
                SERVICE_WORKER_SRC);
        File serviceWorkerJs = new File(options.getFrontendDirectory(),
                SERVICE_WORKER_SRC_JS);
        return !serviceWorker.exists() && !serviceWorkerJs.exists();
    }
}
