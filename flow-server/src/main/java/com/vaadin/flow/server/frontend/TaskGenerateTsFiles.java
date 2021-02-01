/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Abstract class for generating client files.
 *
 * @since 3.0
 */
public class TaskGenerateTsFiles implements FallibleCommand {

    private static final String TSCONFIG_JSON = "tsconfig.json";
    private static final String TS_DEFINITIONS = "types.d.ts";
    private final File npmFolder;
    private final List<String> modules;

    /**
     * Create a task to generate <code>tsconfig.json</code> file.
     *
     * @param npmFolder
     *     project folder where the file will be generated.
     * @param modules
     */
    TaskGenerateTsFiles(File npmFolder, List<String> modules) {
        this.npmFolder = npmFolder;
        this.modules = modules;
    }

    /**
     * Only generate if there exists typescript modules in the project.
     *
     * @return if we have modules for typescript files
     */
    protected boolean shouldGenerate() {
        return modules.stream().filter(file -> file.endsWith(".ts")).count()
            >= 1;
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

        if(generatedFile.exists()) {
            return;
        }

        try {
            String fileContent = IOUtils
                .toString(getClass().getResourceAsStream(file), UTF_8);
            log().info("writing file '{}'", generatedFile);

            FileUtils.forceMkdirParent(generatedFile);
            FileUtils.writeStringToFile(generatedFile, fileContent, UTF_8);
        } catch (IOException exception) {
            String errorMessage = String
                .format("Error writing '%s'", generatedFile);
            throw new ExecutionFailedException(errorMessage, exception);
        }
    }

    Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
