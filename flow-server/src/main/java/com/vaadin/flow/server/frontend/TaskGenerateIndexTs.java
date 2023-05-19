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
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Version;

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
public class TaskGenerateIndexTs extends AbstractTaskClientGenerator {

    private final File frontendDirectory;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateIndexTs(Options options) {
        this.frontendDirectory = options.getFrontendDirectory();
    }

    @Override
    protected File getGeneratedFile() {
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
        try (InputStream indexTsStream = getClass()
                .getResourceAsStream(INDEX_TS)) {
            indexTemplate = IOUtils.toString(indexTsStream, UTF_8);
        }
        return indexTemplate;
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
            indexContent = IOUtils.toString(indexFileExist.toURI(), UTF_8);
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
