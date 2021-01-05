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

import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.apache.commons.io.IOUtils;


/**
 * Generate <code>index.html</code> if it is missing in frontend folder.
 * 
 * @since 3.0
 */
public class TaskGenerateServiceWorker extends AbstractTaskClientGenerator {

    private File frontendDirectory;
    private File outputDirectory;

    /**
     * Create a task to generate <code>sw.ts</code> if necessary.
     * 
     * @param frontendDirectory
     *            frontend directory is to check if the file already exists
     *            there.
     * @param outputDirectory
     *            the output directory of the generated file
     */
    TaskGenerateServiceWorker(File frontendDirectory, File outputDirectory) {
        this.frontendDirectory = frontendDirectory;
        this.outputDirectory = outputDirectory;
    }

    @Override
    protected String getFileContent() throws IOException {
        return IOUtils
                .toString(getClass().getResourceAsStream(SERVICE_WORKER_SRC), UTF_8);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(outputDirectory, SERVICE_WORKER_SRC);
    }

    @Override
    protected boolean shouldGenerate() {
        File serviceWorker = new File(frontendDirectory, SERVICE_WORKER_SRC);
        File serviceWorkerJs = new File(frontendDirectory, SERVICE_WORKER_SRC_JS);
        return !serviceWorker.exists() && !serviceWorkerJs.exists();
    }
}
