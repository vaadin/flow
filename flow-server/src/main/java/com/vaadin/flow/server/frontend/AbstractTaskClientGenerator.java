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

import org.apache.commons.io.FileUtils;
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
public abstract class AbstractTaskClientGenerator implements FallibleCommand {

    /**
     * Get file content for writing to the generated file.
     *
     * @return content of the file.
     * @throws IOException
     *             if IO error happens while reading file content.
     */
    protected abstract String getFileContent() throws IOException;

    /**
     * Get the generated file where content will be written.
     *
     * @return the generated file.
     */
    protected abstract File getGeneratedFile();

    /**
     * Check if it should generate the file or not.
     *
     * @return true if it should generate, false otherwise.
     */
    protected abstract boolean shouldGenerate();

    @Override
    public void execute() throws ExecutionFailedException {
        if (!shouldGenerate()) {
            return;
        }
        File generatedFile = getGeneratedFile();
        try {
            String fileContent = getFileContent();
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
