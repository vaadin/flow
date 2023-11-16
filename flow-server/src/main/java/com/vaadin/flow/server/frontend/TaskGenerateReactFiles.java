/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Version;
import com.vaadin.open.App;

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TSX;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>index.ts</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateReactFiles implements FallibleCommand {

    private final File frontendDirectory;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateReactFiles(Options options) {
        this.frontendDirectory = options.getFrontendDirectory();
    }

    @Override
    public void execute() throws ExecutionFailedException {
        File appTsx = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                "flow/App.tsx");
        File flowTsx = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                "flow/Flow.tsx");
        File routesTsx = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                "flow/routes.tsx");
        try {
            if(!Path.of(frontendDirectory.getPath(), "App.tsx").toFile().exists()) {
                writeFile(appTsx, getFileContent("App.tsx"));
            }

            if(!Path.of(frontendDirectory.getPath(), "routes.tsx").toFile().exists()) {
                writeFile(routesTsx, getFileContent("routes.tsx"));
            }
            writeFile(flowTsx, getFileContent("Flow.tsx"));
        } catch (IOException e) {
            throw new ExecutionFailedException("Failed to read file content", e);
        }
    }

    private void writeFile(File target, String content) throws ExecutionFailedException {

        try {
            FileIOUtils.writeIfChanged(target, content);
        } catch (IOException exception) {
            String errorMessage = String.format("Error writing '%s'",
                    target);
            throw new ExecutionFailedException(errorMessage, exception);
        }
    }

    protected String getFileContent(String fileName) throws IOException {
        String indexTemplate;
        try (InputStream indexTsStream = getClass()
                .getResourceAsStream(fileName)) {
            indexTemplate = IOUtils.toString(indexTsStream, UTF_8);
        }
        return indexTemplate;
    }
}
