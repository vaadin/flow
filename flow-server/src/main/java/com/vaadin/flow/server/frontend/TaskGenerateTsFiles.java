/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Abstract class for generating client files.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsFiles implements FallibleCommand {

    private static final String TSCONFIG_JSON = "tsconfig.json";
    private static final String TS_DEFINITIONS = "types.d.ts";
    private final File npmFolder;

    /**
     * Create a task to generate <code>tsconfig.json</code> file.
     *
     * @param npmFolder
     *            project folder where the file will be generated.
     */
    TaskGenerateTsFiles(File npmFolder) {
        this.npmFolder = npmFolder;
    }

    /**
     * Generate typescript config if it doesn't exist.
     *
     * @return if typescript config file should be created
     */
    protected boolean shouldGenerate() {
        return !new File(npmFolder, TSCONFIG_JSON).exists();
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (!shouldGenerate()) {
            return;
        }

        writeFile(TSCONFIG_JSON);
        writeFile(TS_DEFINITIONS);
    }

    private void writeFile(String file) throws ExecutionFailedException {
        File generatedFile = new File(npmFolder, file);

        if (generatedFile.exists()) {
            return;
        }

        try {
            String fileContent = IOUtils
                    .toString(getClass().getResourceAsStream(file), UTF_8);
            log().debug("writing file '{}'", generatedFile);

            FileUtils.forceMkdirParent(generatedFile);
            FileUtils.writeStringToFile(generatedFile, fileContent, UTF_8);
        } catch (IOException exception) {
            String errorMessage = String.format("Error writing '%s'",
                    generatedFile);
            throw new ExecutionFailedException(errorMessage, exception);
        }
    }

    Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
