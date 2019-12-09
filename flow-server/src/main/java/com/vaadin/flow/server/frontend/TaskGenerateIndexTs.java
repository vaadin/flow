/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.apache.commons.io.IOUtils;

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>index.js</code> if it is missing in frontend folder.
 *
 * @since 3.0
 */
public class TaskGenerateIndexTs extends AbstractTaskClientGenerator {

    private final File frontendDirectory;
    private File generatedImports;
    private final File outputDirectory;

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param generatedImports
     *            the flow generated imports file to include in the
     *            <code>index.js</code>
     * @param outputDirectory
     *            the output directory of the generated file
     */
    TaskGenerateIndexTs(File frontendDirectory, File generatedImports,
            File outputDirectory) {
        this.frontendDirectory = frontendDirectory;
        this.generatedImports = generatedImports;
        this.outputDirectory = outputDirectory;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(outputDirectory, INDEX_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        File indexTs = new File(frontendDirectory, INDEX_TS);
        File indexJs = new File(frontendDirectory, INDEX_JS);
        return !indexTs.exists() && !indexJs.exists();
    }

    @Override
    protected String getFileContent() throws IOException {
        String indexTemplate = IOUtils
                .toString(getClass().getResourceAsStream(INDEX_TS), UTF_8);
        String relativizedImport = ensureValidRelativePath(
                FrontendUtils.getUnixRelativePath(outputDirectory.toPath(),
                        generatedImports.toPath()));
        return indexTemplate.replace("[to-be-generated-by-flow]",
                relativizedImport);
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

}
