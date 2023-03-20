/**
 * Copyright (C) 2000-2023 Vaadin Ltd
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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>tsconfig.json</code> if it is missing in project folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsConfig extends AbstractTaskClientGenerator {

    static final String TSCONFIG_JSON = "tsconfig.json";
    private final File npmFolder;

    /**
     * Create a task to generate <code>tsconfig.json</code> file.
     *
     * @param npmFolder
     *            project folder where the file will be generated.
     */
    TaskGenerateTsConfig(File npmFolder) {
        this.npmFolder = npmFolder;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream tsConfStream = getClass()
                .getResourceAsStream(TSCONFIG_JSON)) {
            return IOUtils.toString(tsConfStream, UTF_8);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return new File(npmFolder, TSCONFIG_JSON);
    }

    @Override
    protected boolean shouldGenerate() {
        return !new File(npmFolder, TSCONFIG_JSON).exists();
    }
}
