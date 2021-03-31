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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>types.d.ts</code> if it is missing in project folder and
 * <code>tsconfig.json</code> exists in project folder.
 *
 * @since 3.0
 */
public class TaskGenerateTsDefinitions extends AbstractTaskClientGenerator {

    private static final String TS_DEFINITIONS = "types.d.ts";
    private final File npmFolder;

    /**
     * Create a task to generate <code>types.d.ts</code> file.
     *
     * @param npmFolder
     *            project folder where the file will be generated.
     */
    TaskGenerateTsDefinitions(File npmFolder) {
        this.npmFolder = npmFolder;
    }

    @Override
    protected String getFileContent() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(TS_DEFINITIONS),
                UTF_8);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(npmFolder, TS_DEFINITIONS);
    }

    @Override
    protected boolean shouldGenerate() {
        File tsDefinitionsFile = new File(npmFolder, TS_DEFINITIONS);
        return !tsDefinitionsFile.exists()
                && new File(npmFolder, TaskGenerateTsConfig.TSCONFIG_JSON)
                        .exists();
    }
}
