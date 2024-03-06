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

import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_HTML;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>web-component.html</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateWebComponentHtml extends AbstractTaskClientGenerator {

    private File webComponentHtml;

    /**
     * Create a task to generate <code>web-component.html</code> in the frontend
     * directory if necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     */
    TaskGenerateWebComponentHtml(File frontendDirectory) {
        // The user is generally not supposed to modify web-component.html and
        // therefore you might have expected it to be generated
        // in the frontend generated directory.
        // It is however generated in the frontend directory like index.html
        // since it is easier to serve it from there in terms of the Vite
        // config.
        webComponentHtml = new File(frontendDirectory, WEB_COMPONENT_HTML);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(WEB_COMPONENT_HTML)) {
            return IOUtils.toString(indexStream, UTF_8);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return webComponentHtml;
    }

    @Override
    protected boolean shouldGenerate() {
        return !webComponentHtml.exists();
    }
}
