/*
 * Copyright (C) 2000-2024 Vaadin Ltd
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

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>index.html</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateIndexHtml extends AbstractTaskClientGenerator {

    private File indexHtml;

    /**
     * Create a task to generate <code>index.html</code> if necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param outputDirectory
     *            the output directory of the generated file
     */
    TaskGenerateIndexHtml(File frontendDirectory) {
        indexHtml = new File(frontendDirectory, INDEX_HTML);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(INDEX_HTML)) {
            return IOUtils.toString(indexStream, UTF_8);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return indexHtml;
    }

    @Override
    protected boolean shouldGenerate() {
        return !indexHtml.exists();
    }
}
