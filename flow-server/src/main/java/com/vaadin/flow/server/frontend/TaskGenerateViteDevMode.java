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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * Generate <code>vite-devmode.ts</code> if it is missing in frontend/generated
 * folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public class TaskGenerateViteDevMode extends AbstractTaskClientGenerator {

    private final File frontendDirectory;

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
     *            the build output directory
     */
    TaskGenerateViteDevMode(File frontendDirectory) {
        this.frontendDirectory = frontendDirectory;
    }

    @Override
    protected File getGeneratedFile() {
        return new File(new File(frontendDirectory, FrontendUtils.GENERATED),
                FrontendUtils.VITE_DEVMODE_TS);
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }

    @Override
    protected String getFileContent() throws IOException {
        return IOUtils.toString(
                getClass().getResourceAsStream(FrontendUtils.VITE_DEVMODE_TS),
                UTF_8);
    }

}
