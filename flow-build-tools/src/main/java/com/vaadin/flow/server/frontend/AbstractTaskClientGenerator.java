/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for generating client files.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public abstract class AbstractTaskClientGenerator
        extends AbstractFileGeneratorFallibleCommand {

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
            writeIfChanged(generatedFile, getFileContent());
        } catch (IOException exception) {
            String errorMessage = String.format("Error writing '%s'",
                    generatedFile);
            throw new ExecutionFailedException(errorMessage, exception);
        }
    }

    static Logger log() {
        return LoggerFactory.getLogger(AbstractTaskClientGenerator.class);
    }

}
