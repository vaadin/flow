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

import com.vaadin.flow.internal.StringUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Version;

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TSX;

/**
 * Generate <code>index.ts</code> if it is missing in frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateIndexTs extends AbstractTaskClientGenerator {

    private static final String ROUTES_JS_IMPORT_PATH_TOKEN = "%routesJsImportPath%";

    private final File frontendDirectory;
    private Options options;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateIndexTs(Options options) {
        this.frontendDirectory = options.getFrontendDirectory();
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (!shouldGenerate()) {
            cleanup();
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

    @Override
    protected File getGeneratedFile() {
        if (options.isReactEnabled()) {
            return new File(
                    new File(frontendDirectory, FrontendUtils.GENERATED),
                    INDEX_TSX);
        }
        return new File(new File(frontendDirectory, FrontendUtils.GENERATED),
                INDEX_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        return Arrays.asList(INDEX_TSX, INDEX_TS, INDEX_JS).stream()
                .map(type -> new File(frontendDirectory, type))
                .filter(File::exists)
                .peek(this::compareActualIndexWithIndexTemplate).findAny()
                .isEmpty();
    }

    @Override
    protected String getFileContent() throws IOException {
        String indexTemplate;
        String indexFile = INDEX_TS;
        if (options.isReactEnabled()) {
            indexFile = "index-react.tsx";
        }
        try (InputStream indexTsStream = getClass()
                .getResourceAsStream(indexFile)) {
            indexTemplate = StringUtil.toUtf8Str(indexTsStream);
            if (options.isReactEnabled()) {
                File routesTsx = new File(frontendDirectory,
                        FrontendUtils.ROUTES_TSX);
                indexTemplate = indexTemplate.replace(
                        ROUTES_JS_IMPORT_PATH_TOKEN,
                        (routesTsx.exists())
                                ? FrontendUtils.FRONTEND_FOLDER_ALIAS
                                        + FrontendUtils.ROUTES_JS
                                : FrontendUtils.FRONTEND_FOLDER_ALIAS
                                        + FrontendUtils.GENERATED
                                        + FrontendUtils.ROUTES_JS);
            }
        }
        return indexTemplate;
    }

    private void cleanup() {
        FileIOUtils.deleteFileQuietly(getGeneratedFile());
    }

    /**
     * Ensure that the given relative path is valid as an import path. NOTE:
     * expose only for testing purpose.
     *
     * @param relativePath
     *            given relative path
     * @return valid import path
     */
    static String ensureValidRelativePath(String relativePath) {
        if (!relativePath.startsWith(".")) {
            relativePath = "./" + relativePath;
        }
        return relativePath;
    }

    private void compareActualIndexWithIndexTemplate(File indexFileExist) {
        String indexContent = null;
        String indexTemplate = null;
        try {
            indexContent = Files.readString(indexFileExist.toPath());
            indexTemplate = getFileContent();
        } catch (IOException e) {
            log().warn("Failed to read file content", e);
        }
        if (indexContent != null && !indexContent.equals(indexTemplate)) {
            UsageStatistics.markAsUsed(Constants.STATISTIC_ROUTING_CLIENT,
                    Version.getFullVersion());
        }
    }

}
