/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.apache.commons.io.IOUtils;


/**
 * Generate <code>index.html</code> if it is missing in frontend folder.
 * 
 * @since 3.0
 */
public class TaskGenerateIndexHtml extends AbstractTaskClientGenerator {

    private File frontendDirectory;
    private File outputDirectory;

    /**
     * Create a task to generate <code>index.html</code> if necessary.
     * 
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param outputDirectory
     *            the output directory of the generated file
     */
    TaskGenerateIndexHtml(File frontendDirectory, File outputDirectory) {
        this.frontendDirectory = frontendDirectory;
        this.outputDirectory = outputDirectory;
    }

    @Override
    protected String getFileContent() throws IOException {
        return IOUtils
                .toString(getClass().getResourceAsStream(INDEX_HTML), UTF_8);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(outputDirectory, INDEX_HTML);
    }

    @Override
    protected boolean shouldGenerate() {
        File indexHTML = new File(frontendDirectory, INDEX_HTML);
        return !indexHTML.exists();
    }
}
