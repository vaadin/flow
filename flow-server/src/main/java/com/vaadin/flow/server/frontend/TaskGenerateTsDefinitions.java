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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>types.d.ts</code> if it is missing in project folder and
 * <code>tsconfig.json</code> exists in project folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsDefinitions extends AbstractTaskClientGenerator {

    private static final String TS_DEFINITIONS = "types.d.ts";
    private final File npmFolder;

    /**
     * Create a task to generate <code>types.d.ts</code> file.
     *
     * @param npmFolder
     *            project folder where the file will be generated.
     */
    TaskGenerateTsDefinitions(File npmFolder) {
        this.npmFolder = npmFolder;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream tsDefinitionStream = getClass()
                .getResourceAsStream(TS_DEFINITIONS)) {
            return IOUtils.toString(tsDefinitionStream, UTF_8);
        }
    }

    @Override
    protected File getGeneratedFile() {
        return new File(npmFolder, TS_DEFINITIONS);
    }

    @Override
    protected boolean shouldGenerate() {
        File tsDefinitionsFile = new File(npmFolder, TS_DEFINITIONS);
        return !tsDefinitionsFile.exists()
                && new File(npmFolder, TaskGenerateTsConfig.TSCONFIG_JSON)
                        .exists();
    }
}
